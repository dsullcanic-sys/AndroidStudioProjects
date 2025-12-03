package com.ryuk.sispreventas;

public class Cliente {
    private String id;
    private String nombre;
    private String apePaterno;
    private String apeMaterno;
    private String celular;

    public Cliente() { }

    public Cliente(String id, String nombre, String apePaterno, String apeMaterno, String celular) {
        this.id = id;
        this.nombre = nombre;
        this.apePaterno = apePaterno;
        this.apeMaterno = apeMaterno;
        this.celular = celular;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApePaterno() { return apePaterno; }
    public String getApeMaterno() { return apeMaterno; }
    public String getCelular() { return celular; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApePaterno(String apePaterno) { this.apePaterno = apePaterno; }
    public void setApeMaterno(String apeMaterno) { this.apeMaterno = apeMaterno; }
    public void setCelular(String celular) { this.celular = celular; }
}
