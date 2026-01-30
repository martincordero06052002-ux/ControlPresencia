package com.example.controlpresencia.data.network;

import com.example.controlpresencia.data.model.FichajeRequest;
import com.example.controlpresencia.data.model.LoginRequest;
import com.example.controlpresencia.data.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // Login
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Fichar
    @POST("api/presencia/fichar")
    Call<Void> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );
}