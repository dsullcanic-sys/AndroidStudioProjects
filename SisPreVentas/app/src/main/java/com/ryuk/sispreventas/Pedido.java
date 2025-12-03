package com.ryuk.sispreventas;

import java.util.Map;

public class Pedido {
    private String id;
    private String fecha;
    private String estado;
    private Map<String, Integer> items;

    // Datos del cliente embebidos
    private String nombreCliente;
    private String apePaternoCliente;
    private String apeMaternoCliente;
    private String celularCliente;

    public Pedido() { }

    public Pedido(String id, String fecha, String estado,
                  Map<String, Integer> items,
                  String nombreCliente, String apePaternoCliente,
                  String apeMaternoCliente, String celularCliente) {
        this.id = id;
        this.fecha = fecha;
        this.estado = estado;
        this.items = items;
        this.nombreCliente = nombreCliente;
        this.apePaternoCliente = apePaternoCliente;
        this.apeMaternoCliente = apeMaternoCliente;
        this.celularCliente = celularCliente;
    }

    public String getId() { return id; }
    public String getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public Map<String, Integer> getItems() { return items; }
    public String getNombreCliente() { return nombreCliente; }
    public String getApePaternoCliente() { return apePaternoCliente; }
    public String getApeMaternoCliente() { return apeMaternoCliente; }
    public String getCelularCliente() { return celularCliente; }

    public void setId(String id) { this.id = id; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setItems(Map<String, Integer> items) { this.items = items; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public void setApePaternoCliente(String apePaternoCliente) { this.apePaternoCliente = apePaternoCliente; }
    public void setApeMaternoCliente(String apeMaternoCliente) { this.apeMaternoCliente = apeMaternoCliente; }
    public void setCelularCliente(String celularCliente) { this.celularCliente = celularCliente; }
}
