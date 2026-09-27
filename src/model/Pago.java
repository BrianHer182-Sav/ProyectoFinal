package model;

public class Pago {

    private int id;
    private int socioId;
    private String socioNombre;
    private String membresia;
    private double monto;
    private String fecha;
    private String metodoPago;
    private String concepto;

    public Pago(int id, int socioId, String socioNombre,
                String membresia, double monto, String fecha,
                String metodoPago, String concepto) {

        this.id = id;
        this.socioId = socioId;
        this.socioNombre = socioNombre;
        this.membresia = membresia;
        this.monto = monto;
        this.fecha = fecha;
        this.metodoPago = metodoPago;
        this.concepto = concepto;
    }

    public int getId() {
        return id;
    }

    public int getSocioId() {
        return socioId;
    }

    public String getSocioNombre() {
        return socioNombre;
    }

    public String getMembresia() {
        return membresia;
    }

    public double getMonto() {
        return monto;
    }

    public String getFecha() {
        return fecha;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setSocioId(int socioId) {
        this.socioId = socioId;
    }

    public void setSocioNombre(String socioNombre) {
        this.socioNombre = socioNombre;
    }

    public void setMembresia(String membresia) {
        this.membresia = membresia;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }
}