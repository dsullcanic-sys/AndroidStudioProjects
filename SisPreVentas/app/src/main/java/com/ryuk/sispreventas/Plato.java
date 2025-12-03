package com.ryuk.sispreventas;

public class Plato {
    private String nombre;
    private int precio;
    private String imagen;

    public Plato() { } // Necesario para Firebase

    public Plato(String nombre, int precio, String imagen) {
        this.nombre = nombre;
        this.precio = precio;
        this.imagen = imagen;
    }

    public String getNombre() { return nombre; }
    public int getPrecio() { return precio; }
    public String getImagen() { return imagen; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(int precio) { this.precio = precio; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
