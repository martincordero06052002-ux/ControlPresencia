package com.example.controlpresencia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.controlpresencia.data.model.LoginRequest;
import com.example.controlpresencia.data.model.LoginResponse;
import com.example.controlpresencia.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Vincular vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        //Verificar si ya tenemos token
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (preferences.contains("auth_token")) {
            irAHome();
        }

        //Acción del botón
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
    }

    private void hacerLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(email, password);

        // Llamada a la API
        RetrofitClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    //Login correcto
                    String token = response.body().getAccess_token();
                    String nombre = response.body().getNombre();

                    // Guardar Token en preferencias
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("auth_token", "Bearer " + token); // Guardamos con prefijo Bearer
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
                Toast.makeText(MainActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Cierra el login para que no se pueda volver atrás
    }
}