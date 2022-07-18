package wifilocation.background;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import wifilocation.background.service.EstimateLoggingService;

public class MainActivity extends AppCompatActivity {

    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    }

    public void startEstimateLoggingService() {
        startService(serviceIntent);
    }

    public void stopEstimateLoggingService() {
        stopService(serviceIntent);
    }
}