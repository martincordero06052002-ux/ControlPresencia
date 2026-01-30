package com.example.controlpresencia;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.controlpresencia.data.model.FichajeRequest;
import com.example.controlpresencia.data.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView tvBienvenida, tvEstado;
    private Button btnEntrada, btnSalida, btnLogout;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int CODIGO_PERMISO_GPS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Inicializar Vistas
        tvBienvenida = findViewById(R.id.tvBienvenida);
        tvEstado = findViewById(R.id.tvEstado);
        btnEntrada = findViewById(R.id.btnEntrada);
        btnSalida = findViewById(R.id.btnSalida);
        btnLogout = findViewById(R.id.btnLogout);

        // 2. Inicializar Cliente de Ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 3. Recuperar nombre del usuario
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String nombre = prefs.getString("user_name", "Usuario");
        tvBienvenida.setText("Hola, " + nombre);

        // 4. Configurar Botones
        btnEntrada.setOnClickListener(v -> intentarFichar("entrada"));
        btnSalida.setOnClickListener(v -> intentarFichar("salida"));

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply(); // Borrar token
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });
    }

    private void intentarFichar(String tipo) {
        // Verificar Permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Pedir permiso si no lo tenemos
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    CODIGO_PERMISO_GPS);
        } else {
            // Ya tenemos permiso, obtener ubicación
            obtenerUbicacionYFichar(tipo);
        }
    }

    private void obtenerUbicacionYFichar(String tipo) {
        tvEstado.setText("Obteniendo GPS...");

        // Android requiere suprimir el aviso de error de permiso porque ya lo comprobamos antes
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // ¡Tenemos coordenadas! Enviamos al servidor
                                enviarFichajeAlServidor(location.getLatitude(), location.getLongitude(), tipo);
                            } else {
                                tvEstado.setText("Error: GPS activado pero sin señal. Abre Maps para probar.");
                                Toast.makeText(HomeActivity.this, "Ubicación no encontrada. Activa el GPS.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void enviarFichajeAlServidor(double lat, double lon, String tipo) {
        tvEstado.setText("Conectando con el servidor...");

        // Preparar datos
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", ""); // Ya incluye "Bearer "

        FichajeRequest request = new FichajeRequest(lat, lon, tipo);

        // Llamada Retrofit
        RetrofitClient.getApiService().fichar(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    tvEstado.setText("¡ÉXITO! " + tipo.toUpperCase() + " REGISTRADA.");
                    Toast.makeText(HomeActivity.this, "Fichaje Correcto", Toast.LENGTH_LONG).show();
                } else {
                    // Si falla (ej: fuera de rango o error lógico)
                    try {
                        // Intentamos leer el mensaje de error del JSON
                        String errorMsg = response.errorBody().string();
                        tvEstado.setText("Error: " + errorMsg);
                    } catch (Exception e) {
                        tvEstado.setText("Error al fichar: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                tvEstado.setText("Fallo de red: " + t.getMessage());
            }
        });
    }

    // Gestionar la respuesta del usuario al permiso de GPS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso GPS concedido. Vuelve a pulsar el botón.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Necesitamos GPS para fichar.", Toast.LENGTH_LONG).show();
            }
        }
    }
}