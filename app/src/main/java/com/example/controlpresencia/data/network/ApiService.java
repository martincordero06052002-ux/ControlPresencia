package com.example.controlpresencia.data.network;

import com.example.controlpresencia.data.model.FichajeRequest;
import com.example.controlpresencia.data.model.HorasExtraResponse;
import com.example.controlpresencia.data.model.IncidenciaRequest;
import com.example.controlpresencia.data.model.LoginRequest;
import com.example.controlpresencia.data.model.LoginResponse;
import com.example.controlpresencia.data.model.MensajeResponse;
import com.example.controlpresencia.data.model.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // Login
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Fichar
    @POST("api/presencia/fichar")
    Call<MensajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // Recuperar Contraseña
    @POST("api/auth/reset-password")
    Call<MensajeResponse> resetPassword(@Body ResetPasswordRequest request);

    // Cerrar Sesión (Logout)
    @POST("api/auth/logout")
    Call<MensajeResponse> logout(@Header("Authorization") String token);

    // Enviar una Incidencia
    @POST("api/incidencias/")
    Call<MensajeResponse> reportarIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );

    @GET("api/presencia/horas-extras")
    Call<HorasExtraResponse> getHorasExtra(@Header("Authorization") String token);
}