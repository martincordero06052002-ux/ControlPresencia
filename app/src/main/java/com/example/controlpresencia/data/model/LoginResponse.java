package com.example.controlpresencia.data.model;

public class LoginResponse {
    private String access_token;
    private String rol;
    private String nombre;

    // Getters
    public String getAccess_token() { return access_token; }
    public String getRol() { return rol; }
    public String getNombre() { return nombre; }
}