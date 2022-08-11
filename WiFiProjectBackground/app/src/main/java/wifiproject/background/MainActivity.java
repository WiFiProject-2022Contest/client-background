package wifilocation.background;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import wifilocation.background.barcode.Barcode;
import wifilocation.background.database.DatabaseHelper;
import wifilocation.background.database.ItemInfo;
import wifilocation.background.serverconnection.RetrofitAPI;
import wifilocation.background.serverconnection.RetrofitClient;
import wifilocation.background.service.EstimateLoggingService;

public class MainActivity extends AppCompatActivity {

    Intent serviceIntent;
    public static String building = "WiFiLocation3F";
    public static String ssid = "WiFiLocation@PDA";
    public static String uuid = ".";

    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        serviceIntent = new Intent(this, EstimateLoggingService.class);

        Button buttonStartLogging = findViewById(R.id.buttonStartLogging);
        buttonStartLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startEstimateLoggingService();
            }
        });

        Button buttonStopLogging = findViewById(R.id.buttonStopLogging);
        buttonStopLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopEstimateLoggingService();
            }
        });

        uuid = getDevicesUUID(this);

        SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String today = dateFormat.format(date);
        String last_saved = sp.getString("last_date", today);
        if (!last_saved.equals(today)) {
            LoadRemoteTask loadRemoteTask = new LoadRemoteTask(this);
            loadRemoteTask.execute();
        }
    }

    private void getPermission() {
        // 권한 체크하고 권한 요청
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
            return;

        if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1000);
        }
    }

    public void startEstimateLoggingService() {
        startService(serviceIntent);
    }

    public void stopEstimateLoggingService() {
        stopService(serviceIntent);
    }

    private String getDevicesUUID(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }

    private class LoadRemoteTask extends AsyncTask<String, String, String> {

        ProgressDialog progressDialog;
        DatabaseHelper dbHelper;

        public LoadRemoteTask(Context context) {
            this.progressDialog = new ProgressDialog(context);
            this.dbHelper = new DatabaseHelper(context);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            publishProgress("기존 데이터 삭제하는 중...");
            dbHelper.deleteWiFiInfo();
            dbHelper.deleteBarcode();
            publishProgress("데이터 받아오는 중...");
            loadRemote();
            return "데이터 받아오기 완료";
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress.length > 0) {
                progressDialog.setMessage(progress[0]);
            }
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }

        private void loadRemote() {
            System.out.println("EstimateLoggingService.loadRemote");
            RetrofitAPI retrofitAPI = RetrofitClient.getRetrofitAPI();
            try {
                List<ItemInfo> itemInfoResult = retrofitAPI.getDataWiFiItem(null, null, null, null, null, null).execute().body();
                dbHelper.insertIntoWiFiInfo(itemInfoResult);
                List<Barcode> barcodeResult = retrofitAPI.getDataBarcode().execute().body();
                dbHelper.insertIntoBarcode(barcodeResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}