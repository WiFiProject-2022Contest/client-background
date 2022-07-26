package wifilocation.background.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import wifilocation.background.MainActivity;
import wifilocation.background.R;
import wifilocation.background.database.AppDatabase;
import wifilocation.background.database.EstimatedResult;
import wifilocation.background.database.ItemInfo;
import wifilocation.background.estimate.PositioningAlgorithm;

public class EstimateLoggingService extends Service {

    EstimateLoggingTask estimateLoggingTask = new EstimateLoggingTask();
    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;

    Context context;
    WifiManager wm;
    List<ItemInfo> items = new ArrayList<>();
    List<ItemInfo> savedItemInfos = new ArrayList<>();

    final static double standardRecordDistance = 8;


    SharedPreferences sp;
    SharedPreferences.Editor edit;

    private BroadcastReceiver wifi_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT <= 22) {
                scanSuccess();
            } else {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            }
        }
    };

    AppDatabase db;

    public EstimateLoggingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifi_receiver, filter);

        db = AppDatabase.getInstance(context);

        sp = getSharedPreferences("sp", MODE_PRIVATE);
        edit = sp.edit();

        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String today = dateFormat.format(date);
        String last_saved = sp.getString("last_date", "");
        if (!last_saved.equals(today)) {
            db.estimatedResultDao().deleteAll();
            edit.putString("last_date", today);
            edit.commit();
            Toast.makeText(context, "데이터 초기화 및 날짜 변경 완료", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent testIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, testIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel", "play", NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "channel")
                    .setSmallIcon(R.drawable.wifilocation)
                    .setContentTitle("WiFiLocation Background")
                    .setContentIntent(pendingIntent)
                    .setContentText("실행 중...");

            try {
                notificationManager.notify(1, notificationBuilder.build());
                estimateLoggingTask.execute();
                Toast.makeText(context, "로깅 시작...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "이미 실행 중입니다!", Toast.LENGTH_SHORT).show();
            }

            return START_STICKY;
            // startForeground(1, notificationBuilder.build());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    class EstimateLoggingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (!isCancelled()) {
                try {
                    wm.startScan();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(context, "로깅 중지...", Toast.LENGTH_SHORT).show();
        context.unregisterReceiver(wifi_receiver);
        AppDatabase.destroyInstance();
        estimateLoggingTask.cancel(true);
        notificationManager.cancel(1);
    }

    private void scanSuccess() {
        items.clear();
        List<ScanResult> results = wm.getScanResults();
        for (ScanResult result : results) {
            items.add(new ItemInfo(0.0f, 0.0f, result.SSID, result.BSSID, result.level, result.frequency, MainActivity.uuid, MainActivity.building, "WiFi", 1));
        }
        Toast.makeText(context, "WiFi Scan Success!.", Toast.LENGTH_SHORT).show();

        EstimateLoggingRunnable estimateLoggingRunnable = new EstimateLoggingRunnable();
        Thread t = new Thread(estimateLoggingRunnable);
        t.start();
    }

    private void scanFailure() {
        Toast.makeText(context, "WiFi Scan failed.", Toast.LENGTH_SHORT).show();
        wm.getScanResults();
    }


    private class EstimateLoggingRunnable implements Runnable {
        @Override
        public void run() {
            List<EstimatedResult> estimatedResults = getEstimatedResults();
            db.estimatedResultDao().insertAll(estimatedResults);
        }

        private List<EstimatedResult> getEstimatedResults() {
            LiveData<ItemInfo> l;
            if (savedItemInfos.size() == 0) {
                savedItemInfos = db.itemInfoDao().loadAllItems();
            }

            List<EstimatedResult> results = new ArrayList<EstimatedResult>();

            // 2ghz
            EstimatedResult result2ghz = PositioningAlgorithm.run(items, savedItemInfos, MainActivity.building, MainActivity.ssid, MainActivity.uuid, "WiFi", 2, standardRecordDistance);
            if (result2ghz != null) {
                results.add(result2ghz);
            }
            // 5ghz
            EstimatedResult result5ghz = PositioningAlgorithm.run(items, savedItemInfos, MainActivity.building, MainActivity.ssid, MainActivity.uuid, "WiFi", 5, standardRecordDistance);
            if (result5ghz != null) {
                results.add(result5ghz);
            }

            return results;
        }
    }
}