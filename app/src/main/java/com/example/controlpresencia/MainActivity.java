package com.example.controlpresencia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.controlpresencia.data.model.LoginRequest;
import com.example.controlpresencia.data.model.LoginResponse;
import com.example.controlpresencia.data.model.MensajeResponse;
import com.example.controlpresencia.data.model.ResetPasswordRequest;
import com.example.controlpresencia.data.network.ApiService;
import com.example.controlpresencia.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vincular vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Verificar si ya tenemos token
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (preferences.contains("auth_token")) {
            irAHome();
        }

        // Acción del botón Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (!email.isEmpty() && !pass.isEmpty()) {
                    hacerLogin(email, pass);
                } else {
                    Toast.makeText(MainActivity.this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // NUEVO: Acción para recuperar contraseña
        tvForgotPassword.setOnClickListener(v -> {
            final EditText etInputEmail = new EditText(this);
            etInputEmail.setHint("Tu correo registrado");
            etInputEmail.setPadding(50, 40, 50, 40);

            new AlertDialog.Builder(this)
                    .setTitle("Recuperar Contraseña")
                    .setMessage("Se enviará una nueva clave temporal a tu email.")
                    .setView(etInputEmail)
                    .setPositiveButton("Enviar", (dialog, which) -> {
                        String email = etInputEmail.getText().toString().trim();
                        if (!email.isEmpty()) {
                            enviarSolicitudReset(email);
                        } else {
                            Toast.makeText(MainActivity.this, "Introduce un email válido", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void hacerLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password);

        RetrofitClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccess_token();
                    String nombre = response.body().getNombre();

                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("auth_token", "Bearer " + token);
                    editor.putString("user_name", nombre);
                    editor.apply();

                    Toast.makeText(MainActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
                    irAHome();
                } else {
                    Toast.makeText(MainActivity.this, "Error: Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }

    // NUEVO: Método para llamar al endpoint de recuperación de contraseña
    private void enviarSolicitudReset(String email) {
        ApiService apiService = RetrofitClient.getApiService();
        ResetPasswordRequest request = new ResetPasswordRequest(email);

        apiService.resetPassword(request).enqueue(new Callback<MensajeResponse>() {
            @Override
            public void onResponse(Call<MensajeResponse> call, Response<MensajeResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Instrucciones enviadas. Revisa tu email.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "No se pudo procesar la solicitud", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MensajeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irAHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}