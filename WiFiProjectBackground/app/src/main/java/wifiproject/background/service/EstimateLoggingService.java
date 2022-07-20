package wifilocation.background.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

import wifilocation.background.MainActivity;
import wifilocation.background.R;
import wifilocation.background.database.DatabaseHelper;
import wifilocation.background.estimate.EstimatedResult;
import wifilocation.background.estimate.PositioningAlgorithm;
import wifilocation.background.model.ItemInfo;

public class EstimateLoggingService extends Service {

    EstimateLoggingTask estimateLoggingTask = new EstimateLoggingTask();
    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;

    Context context;
    WifiManager wm;
    List<ItemInfo> items = new ArrayList<>();
    List<ItemInfo> savedItemInfos = new ArrayList<>();

    final static double standardRecordDistance = 8;

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

    public EstimateLoggingService() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent testIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, testIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        context = this;
        wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifi_receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel", "play", NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "channel")
                    .setSmallIcon(R.drawable.wifilocation)
                    .setContentTitle("WiFiLocation Background")
                    .setContentIntent(pendingIntent)
                    .setContentText("실행 중...");

            notificationManager.notify(1, notificationBuilder.build());
            estimateLoggingTask.execute();

            return START_STICKY;
//            startForeground(1, notificationBuilder.build());
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

        estimateLoggingTask.cancel(true);
        notificationManager.cancel(1);
    }

    private void scanSuccess() {
        items.clear();
        List<ScanResult> results = wm.getScanResults();
        for (ScanResult result : results) {
            items.add(new ItemInfo(0.0f, 0.0f, result.SSID, result.BSSID, result.level, result.frequency, MainActivity.uuid, MainActivity.building, "WiFi"));
        }
        // 스캔한 결과로 측정한 결과 local db에 push
        List<EstimatedResult> estimatedResults = getEstimatedResults();
        pushEstimatedResultsToLocal(estimatedResults, 1);
    }

    private void scanFailure() {
        Toast.makeText(context, "WiFi Scan failed.", Toast.LENGTH_SHORT).show();
        wm.getScanResults();
    }

    private List<EstimatedResult> getEstimatedResults() {
        savedItemInfos = searchItemInfoFromLocal();

        List<EstimatedResult> results = new ArrayList<>();
        // 2ghz
        results.add(PositioningAlgorithm.run(items, savedItemInfos, MainActivity.building, MainActivity.ssid, MainActivity.uuid, "WiFi", 2, standardRecordDistance));
        // 5ghz
        results.add(PositioningAlgorithm.run(items, savedItemInfos, MainActivity.building, MainActivity.ssid, MainActivity.uuid, "WiFi", 5, standardRecordDistance));

        return results;
    }

    private List<ItemInfo> searchItemInfoFromLocal() {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<ItemInfo> savedWiFiItemInfos = dbHelper.searchFromWiFiInfo(MainActivity.building, MainActivity.ssid, null, null, null, null, null);

        return savedWiFiItemInfos;
    }

    private void pushEstimatedResultsToLocal(List<EstimatedResult> estimatedResults, Integer _new) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.insertIntoFingerprint(estimatedResults, (_new == null ? 0 : 1));
    }
}