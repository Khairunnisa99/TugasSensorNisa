package com.example.tugassensornisa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private Button mFindME;

    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    private TextView azimuthTextView, pitchTextView, rollTextView;
    private TextView MagnetX,MagnetY, MagnetZ, Lokasiku, titikLokasi;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    //private static final String FILE_NAME = "sensor_data.txt"; // Nama file untuk menyimpan data sensor

    private Vector<String> mDataLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        // Inisialisasi TextViews
        azimuthTextView = findViewById(R.id.azimuthTextView);
        pitchTextView = findViewById(R.id.pitchTextView);
        rollTextView = findViewById(R.id.rollTextView);

        MagnetX = findViewById(R.id.magneticXTextView);
        MagnetY = findViewById(R.id.magneticYTextView);
        MagnetZ = findViewById(R.id.magneticZTextView);

        Lokasiku = findViewById(R.id.latitudeTextView);
        titikLokasi = findViewById(R.id.KoordinatTextView);


        mFindME = findViewById(R.id.FindMe);

        mDataLog = new Vector<>(100);

        // Set up the click listener for the Save button
        mFindME.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check location permission before getting the location
                checkLocationPermission();
            }
        });
    }

    private void checkLocationPermission() {
        // Check if the app has location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // If permission is already granted, get the location
            getLocationAndAddress();
        }
    }

    // Callback for when the user responds to the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted, get the location
                getLocationAndAddress();
            } else {
                // If permission is denied, show a message
                Toast.makeText(this, "Location permission is required to get the address.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndAddress() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                String address = getAddressFromLocation(latitude, longitude);

                                // Display the location in mTextLokasi
                                Lokasiku.setText("Lokasi Saya: " + address);
                                titikLokasi.setText("titik Lokasi : " + latitude +" ,"+longitude);
                            } else {
                                Toast.makeText(MainActivity.this, "Could not get location. Please enable GPS.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        // Use Geocoder to get the address from latitude and longitude
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return address.getAddressLine(0); // You can customize the address format as needed
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not found";
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Daftarkan listener sensor
        if (accelerometerSensor != null && magneticFieldSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Batalkan pendaftaran listener sensor untuk menghemat daya
        sensorManager.unregisterListener(this);
    }

    private static final long SENSOR_UPDATE_DELAY = 500; // Pembaruan setiap 500 milidetik
    private long lastSensorUpdateTime = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Batasi frekuensi pembaruan
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSensorUpdateTime < SENSOR_UPDATE_DELAY) {
            return;
        }

        // Perbarui nilai sensor
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values;


            float magneticX = magneticFieldValues[0];
            float magneticY = magneticFieldValues[1];
            float magneticZ = magneticFieldValues[2];

            String formattedMagneticX = String.format("%.2f", magneticX);
            String formattedMagneticY = String.format("%.2f", magneticY);
            String formattedMagneticZ = String.format("%.2f", magneticZ);

            MagnetX.setText("Magnetic X: " + formattedMagneticX);
            MagnetY.setText("Magnetic Y: " + formattedMagneticY);
            MagnetZ.setText("Magnetic Z: " + formattedMagneticZ);
        }

        // Hitung orientasi
        float[] rotationMatrix = new float[9];
        float[] orientationValues = new float[3];
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticFieldValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            // Konversi radian ke derajat
            float azimuth = (float) Math.toDegrees(orientationValues[0]);
            float pitch = (float) Math.toDegrees(orientationValues[1]);
            float roll = (float) Math.toDegrees(orientationValues[2]);

            // Format nilai untuk menampilkan hanya dua desimal
            String formattedAzimuth = String.format("%.2f", azimuth);
            String formattedPitch = String.format("%.2f", pitch);
            String formattedRoll = String.format("%.2f", roll);



            // Perbarui TextViews
            azimuthTextView.setText("Azimuth: " + formattedAzimuth);
            pitchTextView.setText("Pitch: " + formattedPitch);
            rollTextView.setText("Roll: " + formattedRoll);

            //MagnetY.setText("Magnetic X:" + );

            // Perbarui waktu pembaruan terakhir
            lastSensorUpdateTime = currentTime;

            // Simpan data ke dalam vektor dan file CSV
            String logText = formattedAzimuth + "," + formattedPitch + "," + formattedRoll;
            String location = Lokasiku.getText().toString();
            mDataLog.add(logText);
            //saveDataToCSV(logText);
            //saveDataToCSV(logText, location);

            //String logText = formattedAzimuth + "," + formattedPitch + "," + formattedRoll;
            mDataLog.add(logText);
            saveDataToCSV(logText);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak melakukan apa-apa untuk saat ini
    }

    private void saveDataToCSV(String text) {
        try {
            File csvFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SensorNisa.csv");

            if (!csvFile.exists()) {
                csvFile.createNewFile();
                // Jika file baru dibuat, tambahkan header (opsional)
                BufferedWriter headerWriter = new BufferedWriter(new FileWriter(csvFile, true));
                headerWriter.write("Timestamp,Azimuth,Pitch,Roll,X,Y,Z,Location"); // Sesuaikan header sesuai kebutuhan
                headerWriter.newLine();
                headerWriter.close();
            }

            // Dapatkan lokasi dari TextView Lokasiku
            String location = Lokasiku.getText().toString();

            // Tulis data ke file CSV
            BufferedWriter buf = new BufferedWriter(new FileWriter(csvFile, true));
            buf.append(System.currentTimeMillis() + "," + text + "," + location);

            buf.append(",").append(titikLokasi.getText().toString());

            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class WriteThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mDataLog.size() > 0) {
                    SystemClock.sleep(10);
                    saveDataToCSV(mDataLog.firstElement());
                    mDataLog.remove(0);
                } else {
                    SystemClock.sleep(100);
                }
            }
        }
    }
}
