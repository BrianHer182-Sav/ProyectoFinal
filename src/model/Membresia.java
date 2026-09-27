package model;

public class Membresia {

    private int id;
    private String nombre;
    private String descripcion;
    private int duracionDias;
    private double precio;
    private boolean activa;

    public Membresia(int id, String nombre, String descripcion,
                     int duracionDias, double precio) {

        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionDias = duracionDias;
        this.precio = precio;
        this.activa = true;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public double getPrecio() {
        return precio;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setDuracionDias(int duracionDias) {
        this.duracionDias = duracionDias;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}