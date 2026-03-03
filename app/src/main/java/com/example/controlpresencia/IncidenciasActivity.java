package com.example.controlpresencia;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.controlpresencia.data.model.IncidenciaRequest;
import com.example.controlpresencia.data.model.MensajeResponse;
import com.example.controlpresencia.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidenciasActivity extends AppCompatActivity {

    private EditText etDescripcion;
    private Button btnEnviarIncidencia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incidencias);

        // Ajuste de márgenes para diseño EdgeToEdge
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 1. Vincular vistas (Asegúrate de que estos IDs existan en tu activity_incidencias.xml)
        etDescripcion = findViewById(R.id.etDescripcionIncidencia);
        btnEnviarIncidencia = findViewById(R.id.btnEnviarIncidencia);

        // 2. Acción del botón
        btnEnviarIncidencia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String descripcion = etDescripcion.getText().toString().trim();
                if (!descripcion.isEmpty()) {
                    enviarIncidencia(descripcion);
                } else {
                    Toast.makeText(IncidenciasActivity.this, "Por favor, describe la incidencia", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void enviarIncidencia(String desc) {
        // Recuperar el token guardado en el Login
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparar la petición
        IncidenciaRequest request = new IncidenciaRequest(desc);
        btnEnviarIncidencia.setEnabled(false);

        // Llamada a la API (Endpoint: api/incidencias/)
        RetrofitClient.getApiService().reportarIncidencia(token, request).enqueue(new Callback<MensajeResponse>() {
            @Override
            public void onResponse(Call<MensajeResponse> call, Response<MensajeResponse> response) {
                btnEnviarIncidencia.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(IncidenciasActivity.this, "Incidencia enviada correctamente", Toast.LENGTH_LONG).show();
                    finish(); // Cerramos la actividad y volvemos a la pantalla anterior
                } else {
                    Toast.makeText(IncidenciasActivity.this, "Error al enviar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MensajeResponse> call, Throwable t) {
                btnEnviarIncidencia.setEnabled(true);
                Toast.makeText(IncidenciasActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}