package com.example.controlpresencia.data.model;

public class FichajeRequest {
    private double lat;
    private double lon;
    private String tipo;

    public FichajeRequest(double lat, double lon, String tipo) {
        this.lat = lat;
        this.lon = lon;
        this.tipo = tipo;
    }

    // Getters y Setters
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getTipo() { return tipo; }
}