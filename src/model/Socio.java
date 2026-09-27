package model;

public class Socio {

    private int id;
    private String nombre;
    private String dni;
    private String correo;
    private String telefono;
    private String fechaNacimiento;
    private String membresia;
    private double peso;
    private double altura;
    private String fechaAlta = "";
    private String fechaVencimiento = "";
    private boolean activo;
    private String fotoPath = "";

    public Socio(int id, String nombre, String dni, String correo,
                 String telefono, String fechaNacimiento,
                 String membresia, double peso, double altura) {

        this.id = id;
        this.nombre = nombre;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.membresia = membresia;
        this.peso = peso;
        this.altura = altura;
        this.activo = true;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDni() {
        return dni;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getMembresia() {
        return membresia;
    }

    public double getPeso() {
        return peso;
    }

    public double getAltura() {
        return altura;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getFechaAlta() {
        return fechaAlta;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaAlta(String fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setMembresia(String membresia) {
        this.membresia = membresia;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public void setAltura(double altura) {
        this.altura = altura;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getFotoPath() {
        return fotoPath;
    }

    public void setFotoPath(String fotoPath) {
        this.fotoPath = fotoPath;
    }
}