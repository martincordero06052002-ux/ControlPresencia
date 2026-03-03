package com.example.controlpresencia;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.controlpresencia.data.model.FichajeRequest;
import com.example.controlpresencia.data.model.HorasExtraResponse;
import com.example.controlpresencia.data.model.MensajeResponse;
import com.example.controlpresencia.data.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvBienvenida, tvEstado, tvHorasExtra;
    private Button btnEntrada, btnSalida, btnLogout, btnIncidencias;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int CODIGO_PERMISO_GPS = 100;
    private static final int CODIGO_PERMISO_NOTIF = 101;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvBienvenida = findViewById(R.id.tvBienvenida);
        tvEstado = findViewById(R.id.tvEstado);
        tvHorasExtra = findViewById(R.id.tvHorasExtra);
        btnEntrada = findViewById(R.id.btnEntrada);
        btnSalida = findViewById(R.id.btnSalida);
        btnLogout = findViewById(R.id.btnLogout);
        btnIncidencias = findViewById(R.id.btnIncidencias);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initNfc();

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String nombre = prefs.getString("user_name", "Usuario");
        tvBienvenida.setText("Hola, " + nombre);

        btnEntrada.setOnClickListener(v -> intentarFichar("entrada"));
        btnSalida.setOnClickListener(v -> intentarFichar("salida"));
        btnIncidencias.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, IncidenciasActivity.class)));

        btnLogout.setOnClickListener(v -> {
            String token = prefs.getString("auth_token", "");
            RetrofitClient.getApiService().logout(token).enqueue(new Callback<MensajeResponse>() {
                @Override
                public void onResponse(Call<MensajeResponse> call, Response<MensajeResponse> response) {
                    realizarLogoutLocal(prefs);
                }
                @Override
                public void onFailure(Call<MensajeResponse> call, Throwable t) {
                    realizarLogoutLocal(prefs);
                }
            });
        });

        obtenerHorasExtra();
        mostrarNotificacionRecordatorio();
    }

    private void realizarLogoutLocal(SharedPreferences prefs) {
        prefs.edit().clear().apply();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            intentarFichar("entrada");
        }
    }

    private void intentarFichar(String tipo) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CODIGO_PERMISO_GPS);
        } else {
            obtenerUbicacionYFichar(tipo);
        }
    }

    private void obtenerUbicacionYFichar(String tipo) {
        tvEstado.setText("Obteniendo GPS...");
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) enviarFichajeAlServidor(location.getLatitude(), location.getLongitude(), tipo);
                else tvEstado.setText("Error: Activa el GPS.");
            });
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void enviarFichajeAlServidor(double lat, double lon, String tipo) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");
        FichajeRequest request = new FichajeRequest(lat, lon, tipo);

        RetrofitClient.getApiService().fichar(token, request).enqueue(new Callback<MensajeResponse>() {
            @Override
            public void onResponse(Call<MensajeResponse> call, Response<MensajeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvEstado.setText(response.body().getMsg());
                    obtenerHorasExtra();
                } else tvEstado.setText("Error: Fuera de rango");
            }
            @Override
            public void onFailure(Call<MensajeResponse> call, Throwable t) { tvEstado.setText("Error de red"); }
        });
    }

    private void obtenerHorasExtra() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");

        RetrofitClient.getApiService().getHorasExtra(token).enqueue(new Callback<HorasExtraResponse>() {
            @Override
            public void onResponse(Call<HorasExtraResponse> call, Response<HorasExtraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HorasExtraResponse data = response.body();
                    tvHorasExtra.setText("Mes: " + data.getMes_actual() + "\nTrabajado: " + data.getHoras_trabajadas() + "h | Extras: " + data.getHoras_extras() + "h");
                }
            }
            @Override
            public void onFailure(Call<HorasExtraResponse> call, Throwable t) { tvHorasExtra.setText("Error en cálculo"); }
        });
    }

    private void mostrarNotificacionRecordatorio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODIGO_PERMISO_NOTIF);
                return;
            }
        }

        String canalId = "recordatorio_fichaje";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(canalId, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Control de Presencia")
                .setContentText("No olvides registrar tu jornada.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        nm.notify(1, builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CODIGO_PERMISO_GPS) Toast.makeText(this, "GPS concedido", Toast.LENGTH_SHORT).show();
            if (requestCode == CODIGO_PERMISO_NOTIF) mostrarNotificacionRecordatorio();
        }
    }
}