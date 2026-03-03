package com.example.controlpresencia.data.model;

public class IncidenciaRequest {
    private String descripcion;

    public IncidenciaRequest(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}