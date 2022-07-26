package wifilocation.background;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.UUID;

import wifilocation.background.database.EstimatedResultViewModel;
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
}