package control;

import java.util.ArrayList;
import model.Socio;
import model.Entrenador;
import model.Membresia;
import model.Pago;

import database.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GimnasioControl {

    // ==========================================
    // ACTIVIDAD RECIENTE
    // ==========================================
    // Registro real de lo que va pasando en el sistema (altas,
    // ediciones, renovaciones, bajas, pagos...), para que la
    // sección "Actividad Reciente" del Inicio no muestre datos
    // fijos sino lo que de verdad se hizo en la sesión.

    public static class Actividad {

        public final String tipo;
        public final String titulo;
        public final String descripcion;
        public final String fechaHora;

        public Actividad(String tipo, String titulo, String descripcion, String fechaHora) {

            this.tipo = tipo;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.fechaHora = fechaHora;
        }
    }

    private ArrayList<Actividad> actividades = new ArrayList<>();

    public void registrarActividad(String tipo, String titulo, String descripcion) {

        String fechaHora =
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(new java.util.Date());

        actividades.add(0, new Actividad(tipo, titulo, descripcion, fechaHora));

        // Se limita el historial para que no crezca sin control
        // durante una sesión larga.
        while (actividades.size() > 60) {
            actividades.remove(actividades.size() - 1);
        }
    }

    public ArrayList<Actividad> getActividades() {
        return actividades;
    }

    // ==========================================
    // INSTANCIA COMPARTIDA
    // ==========================================
    // Antes cada `new GimnasioControl()` creaba un gimnasio nuevo
    // con los datos de ejemplo reiniciados, así que cerrar sesión
    // y volver a entrar (o crear otra cuenta) parecía "traer los
    // mismos datos" porque en realidad los reseteaba. Con esto,
    // todas las cuentas comparten el mismo gimnasio real mientras
    // el programa siga abierto, como en un sistema con varios
    // empleados usando la misma base.

    private static GimnasioControl instancia;

    public static GimnasioControl obtenerInstancia() {

        if (instancia == null) {
            instancia = new GimnasioControl();
        }

        return instancia;
    }

    // ==========================================
    // LISTAS DEL SISTEMA
    // ==========================================

    private ArrayList<Socio> socios;
    private ArrayList<Entrenador> entrenadores;
    private ArrayList<Membresia> membresias;
    private ArrayList<Pago> pagos;

    // ==========================================
    // CONTADORES PARA LOS ID
    // ==========================================

    private int siguienteSocio = 1;
    private int siguienteEntrenador = 1;
    private int siguienteMembresia = 1;
    private int siguientePago = 1;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    // true si el programa logró conectarse a MySQL en el arranque.
    // Si es false, el programa sigue funcionando (para no dejar a
    // alguien sin poder exponer si XAMPP no prendió), pero todo
    // vive solo en memoria y se pierde al cerrar, como antes.
    private boolean conectadoBD = false;

    public boolean isConectadoBD() {
        return conectadoBD;
    }

    public GimnasioControl() {

        socios = new ArrayList<>();
        entrenadores = new ArrayList<>();
        membresias = new ArrayList<>();
        pagos = new ArrayList<>();

        conectadoBD = Conexion.probar();

        if (conectadoBD) {

            try {

                cargarDesdeBD();

                if (membresias.isEmpty() && socios.isEmpty()) {

                    // Base de datos recién creada (tablas vacías):
                    // se siembra con los datos de ejemplo, y esta
                    // vez sí quedan guardados en MySQL de verdad.
                    cargarDatosIniciales();
                }

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo leer la base de datos, se "
                        + "usará memoria temporal: " + ex.getMessage()
                );

                conectadoBD = false;

                cargarDatosIniciales();
            }

        } else {

            System.err.println(
                    "No se pudo conectar a MySQL (XAMPP). El "
                    + "programa sigue funcionando, pero los datos "
                    + "NO se van a guardar entre sesiones."
            );

            cargarDatosIniciales();
        }
    }

    // ==========================================
    // CARGA DESDE LA BASE DE DATOS
    // ==========================================

    private void cargarDesdeBD() throws SQLException {

        try (Connection con = Conexion.obtener()) {

            // ---- Membresías ----
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT * FROM membresias ORDER BY id")) {

                while (rs.next()) {

                    Membresia m = new Membresia(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getInt("duracion_dias"),
                            rs.getDouble("precio")
                    );

                    m.setActiva(rs.getBoolean("activa"));

                    membresias.add(m);

                    if (m.getId() >= siguienteMembresia) {
                        siguienteMembresia = m.getId() + 1;
                    }
                }
            }

            // ---- Socios ----
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT * FROM socios ORDER BY id")) {

                while (rs.next()) {

                    Socio s = new Socio(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("dni"),
                            rs.getString("correo"),
                            rs.getString("telefono"),
                            rs.getString("fecha_nacimiento"),
                            rs.getString("membresia"),
                            rs.getDouble("peso"),
                            rs.getDouble("altura")
                    );

                    s.setFechaAlta(rs.getString("fecha_alta"));
                    s.setFechaVencimiento(rs.getString("fecha_vencimiento"));
                    s.setActivo(rs.getBoolean("activo"));
                    s.setFotoPath(rs.getString("foto_path"));

                    socios.add(s);

                    if (s.getId() >= siguienteSocio) {
                        siguienteSocio = s.getId() + 1;
                    }
                }
            }

            // ---- Entrenadores ----
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT * FROM entrenadores ORDER BY id")) {

                while (rs.next()) {

                    Entrenador e = new Entrenador(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("especialidad"),
                            rs.getString("telefono"),
                            rs.getString("correo"),
                            rs.getDouble("salario")
                    );

                    e.setActivo(rs.getBoolean("activo"));
                    e.setFotoPath(rs.getString("foto_path"));

                    entrenadores.add(e);

                    if (e.getId() >= siguienteEntrenador) {
                        siguienteEntrenador = e.getId() + 1;
                    }
                }
            }

            // ---- Pagos ----
            try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT * FROM pagos ORDER BY id")) {

                while (rs.next()) {

                    Pago p = new Pago(
                            rs.getInt("id"),
                            rs.getInt("socio_id"),
                            rs.getString("socio_nombre"),
                            rs.getString("membresia"),
                            rs.getDouble("monto"),
                            rs.getString("fecha"),
                            rs.getString("metodo_pago"),
                            rs.getString("concepto")
                    );

                    pagos.add(p);

                    if (p.getId() >= siguientePago) {
                        siguientePago = p.getId() + 1;
                    }
                }
            }
        }
    }

    // ==========================================
    // DATOS INICIALES
    // ==========================================

    private void cargarDatosIniciales() {

        // ------------------------------
        // MEMBRESIAS
        // ------------------------------

        agregarMembresia(
            "Día",
            "Visita de un solo día al gimnasio.",
            1,
            100.00
        );

        agregarMembresia(
            "Básica",
            "Acceso a todas las áreas del gimnasio.",
            30,
            500.00
        );

        agregarMembresia(
            "Premium",
            "Acceso completo + clases grupales.",
            90,
            1200.00
        );

        agregarMembresia(
            "Trimestral",
            "Acceso completo + entrenador.",
            90,
            2200.00
        );

        agregarMembresia(
            "Semestral",
            "Acceso completo + beneficios adicionales.",
            180,
            3200.00
        );

        agregarMembresia(
            "Anual",
            "Acceso completo durante un año.",
            365,
            4000.00
        );

        agregarMembresia(
            "Estudiante",
            "Descuento especial para estudiantes.",
            90,
            800.00
        );

        // ------------------------------
        // SOCIOS
        // ------------------------------

        agregarSocio(
            "Carlos Hernández",
            "12345678",
            "carlos@gmail.com",
            "9981234567",
            "15/05/2000",
            "Premium",
            75.5,
            1.75
        );

        agregarSocio(
            "Laura Martínez",
            "23456789",
            "laura@gmail.com",
            "9987654321",
            "20/08/2002",
            "Básica",
            62.0,
            1.65
        );

        // ------------------------------
        // SOCIO DE PRUEBA: BÁSICA VENCIDA AYER
        // ------------------------------
        // Para poder probar la sección de "Vencidas" / renovación
        // sin esperar a que pase el tiempo real.

        Socio socioVencido = agregarSocio(
            "Ricardo Flores",
            "34567890",
            "ricardo.flores@gmail.com",
            "9981239876",
            "10/02/1998",
            "Básica",
            80.0,
            1.78
        );

        {
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("dd/MM/yyyy");

            java.util.Calendar cal = java.util.Calendar.getInstance();

            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);

            String ayer = fmt.format(cal.getTime());

            cal.add(java.util.Calendar.DAY_OF_MONTH, -30);

            String haceUnMes = fmt.format(cal.getTime());

            socioVencido.setFechaAlta(haceUnMes);
            socioVencido.setFechaVencimiento(ayer);

            actualizarSocio(socioVencido);

            agregarPago(
                socioVencido.getId(),
                socioVencido.getNombre(),
                "Básica",
                500.00,
                haceUnMes,
                "Efectivo",
                "Alta"
            );
        }

        // ------------------------------
        // 20 SOCIOS DE PRUEBA ADICIONALES
        // ------------------------------
        // Con membresías y métodos de pago variados, para tener
        // datos de sobra con los que probar Socios, Pagos y
        // Reportes.

        String[] nombresPrueba = {
            "Ana Torres", "Miguel Ángel Ruiz", "Fernanda López",
            "Jorge Castillo", "Paola Jiménez", "Luis Ortega",
            "Valeria Mendoza", "Diego Salazar", "Camila Reyes",
            "Andrés Vargas", "Gabriela Soto", "Emilio Guerrero",
            "Daniela Rojas", "Héctor Morales", "Ximena Cabrera",
            "Iván Delgado", "Renata Aguilar", "Sergio Pineda",
            "Mariana Cortés", "Alejandro Nava"
        };

        String[] membresiasPrueba = {
            "Día", "Básica", "Premium", "Trimestral",
            "Semestral", "Anual", "Estudiante"
        };

        String[] metodosPrueba = {
            "Efectivo", "Tarjeta", "Transferencia"
        };

        java.text.SimpleDateFormat fmtAlta =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        for (int i = 0; i < nombresPrueba.length; i++) {

            String nombre = nombresPrueba[i];

            String dni = String.valueOf(40000000 + i * 137);

            String correo =
                    nombre.toLowerCase()
                            .replace(" ", ".")
                            .replaceAll("[áéíóúñ]", "a")
                    + "@gmail.com";

            String telefono = "998" + String.format("%07d", 1000000 + i * 91);

            String membresia = membresiasPrueba[i % membresiasPrueba.length];

            double peso = 55 + (i % 12) * 3.5;
            double altura = 1.55 + (i % 8) * 0.03;

            // Altas escalonadas en los últimos días, para que se
            // vean movimientos recientes en Reportes / Actividad.
            java.util.Calendar calAlta = java.util.Calendar.getInstance();
            calAlta.add(java.util.Calendar.DAY_OF_MONTH, -(i % 15));
            String fechaAltaTexto = fmtAlta.format(calAlta.getTime());

            String fechaNacimiento =
                    String.format(
                            "%02d/%02d/%d",
                            10 + (i % 18),
                            1 + (i % 12),
                            1985 + (i % 20)
                    );

            Socio nuevo = agregarSocio(
                nombre,
                dni,
                correo,
                telefono,
                fechaNacimiento,
                membresia,
                Math.round(peso * 10) / 10.0,
                Math.round(altura * 100) / 100.0
            );

            nuevo.setFechaAlta(fechaAltaTexto);

            actualizarSocio(nuevo);

            double precio = precioMembresia(membresia);

            if (precio > 0) {

                agregarPago(
                    nuevo.getId(),
                    nuevo.getNombre(),
                    membresia,
                    precio,
                    fechaAltaTexto,
                    metodosPrueba[i % metodosPrueba.length],
                    "Alta"
                );
            }
        }

        // ------------------------------
        // ENTRENADORES
        // ------------------------------

        agregarEntrenador(
            "Daniel Ramírez",
            "Musculación",
            "9981112233",
            "daniel@gym.com",
            8500.00
        );

        agregarEntrenador(
            "Sofía Torres",
            "Entrenamiento funcional",
            "9984445566",
            "sofia@gym.com",
            9000.00
        );
    }

    // ==========================================
    // SOCIOS
    // ==========================================

    public Socio agregarSocio(
            String nombre,
            String dni,
            String correo,
            String telefono,
            String fechaNacimiento,
            String membresia,
            double peso,
            double altura) {

        Socio socio = new Socio(
            siguienteSocio++,
            nombre,
            dni,
            correo,
            telefono,
            fechaNacimiento,
            membresia,
            peso,
            altura
        );

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Calendar cal = java.util.Calendar.getInstance();

        socio.setFechaAlta(fmt.format(cal.getTime()));

        int dias = 30;

        for (Membresia m : membresias) {

            if (m.getNombre().equalsIgnoreCase(membresia)) {
                dias = m.getDuracionDias();
                break;
            }
        }

        cal.add(java.util.Calendar.DAY_OF_MONTH, dias);

        socio.setFechaVencimiento(fmt.format(cal.getTime()));

        socios.add(socio);

        if (conectadoBD) {

            String sql = "INSERT INTO socios "
                    + "(id, nombre, dni, correo, telefono, fecha_nacimiento, "
                    + "membresia, peso, altura, fecha_alta, fecha_vencimiento, "
                    + "activo, foto_path) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, socio.getId());
                ps.setString(2, socio.getNombre());
                ps.setString(3, socio.getDni());
                ps.setString(4, socio.getCorreo());
                ps.setString(5, socio.getTelefono());
                ps.setString(6, socio.getFechaNacimiento());
                ps.setString(7, socio.getMembresia());
                ps.setDouble(8, socio.getPeso());
                ps.setDouble(9, socio.getAltura());
                ps.setString(10, socio.getFechaAlta());
                ps.setString(11, socio.getFechaVencimiento());
                ps.setBoolean(12, socio.isActivo());
                ps.setString(13, socio.getFotoPath());

                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo guardar el socio en la base "
                        + "de datos: " + ex.getMessage()
                );
            }
        }

        // NOTA: el pago de la membresía ya NO se genera aquí de forma
        // automática ni oculta. Se registra desde el formulario de
        // Socios (MainFrame), donde el usuario elige el método de
        // pago real con el que se cobró el alta. Así el concepto y
        // el método que aparecen en Pagos siempre corresponden a lo
        // que realmente se seleccionó, en vez de quedar fijos en
        // "Efectivo" / "Alta".

        return socio;
    }

    /**
     * Guarda en la base de datos los cambios hechos a un socio que
     * ya existía (después de usar sus "set..." directamente). Se
     * llama desde la pantalla de Socios al editar o renovar.
     */
    public void actualizarSocio(Socio socio) {

        if (!conectadoBD || socio == null) {
            return;
        }

        String sql = "UPDATE socios SET nombre=?, dni=?, correo=?, "
                + "telefono=?, fecha_nacimiento=?, membresia=?, peso=?, "
                + "altura=?, fecha_alta=?, fecha_vencimiento=?, activo=?, "
                + "foto_path=? WHERE id=?";

        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, socio.getNombre());
            ps.setString(2, socio.getDni());
            ps.setString(3, socio.getCorreo());
            ps.setString(4, socio.getTelefono());
            ps.setString(5, socio.getFechaNacimiento());
            ps.setString(6, socio.getMembresia());
            ps.setDouble(7, socio.getPeso());
            ps.setDouble(8, socio.getAltura());
            ps.setString(9, socio.getFechaAlta());
            ps.setString(10, socio.getFechaVencimiento());
            ps.setBoolean(11, socio.isActivo());
            ps.setString(12, socio.getFotoPath());
            ps.setInt(13, socio.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {

            System.err.println(
                    "No se pudo actualizar el socio en la base de "
                    + "datos: " + ex.getMessage()
            );
        }
    }

    /**
     * Recalcula la fecha de vencimiento de un socio a partir de HOY,
     * según la duración del plan indicado. Se usa al renovar una
     * membresía (incluida la de un socio ya vencido).
     */
    public String renovarVencimiento(String nombreMembresia) {

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Calendar cal = java.util.Calendar.getInstance();

        int dias = 30;

        for (Membresia m : membresias) {

            if (m.getNombre().equalsIgnoreCase(nombreMembresia)) {
                dias = m.getDuracionDias();
                break;
            }
        }

        cal.add(java.util.Calendar.DAY_OF_MONTH, dias);

        return fmt.format(cal.getTime());
    }

    /** Precio actual de una membresía por nombre (0 si no existe o es "Sin membresía"). */
    public double precioMembresia(String nombreMembresia) {

        for (Membresia m : membresias) {

            if (m.getNombre().equalsIgnoreCase(nombreMembresia)) {
                return m.getPrecio();
            }
        }

        return 0;
    }

    public ArrayList<Socio> getSocios() {
        return socios;
    }

    public Socio buscarSocio(int id) {

        for (Socio socio : socios) {

            if (socio.getId() == id) {
                return socio;
            }
        }

        return null;
    }

    public void eliminarSocio(int id) {

        Socio socio = buscarSocio(id);

        if (socio != null) {
            socios.remove(socio);
        }

        // La base de datos borra en cascada los pagos de este
        // socio (ON DELETE CASCADE); se hace lo mismo aquí en
        // memoria para que ambos queden iguales sin reiniciar.
        pagos.removeIf(p -> p.getSocioId() == id);

        if (conectadoBD) {

            // ON DELETE CASCADE en la tabla "pagos" borra también
            // los pagos de este socio en la base de datos.
            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM socios WHERE id=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo eliminar el socio de la base "
                        + "de datos: " + ex.getMessage()
                );
            }
        }
    }

    // ==========================================
    // ENTRENADORES
    // ==========================================

    public Entrenador agregarEntrenador(
            String nombre,
            String especialidad,
            String telefono,
            String correo,
            double salario) {

        Entrenador entrenador = new Entrenador(
            siguienteEntrenador++,
            nombre,
            especialidad,
            telefono,
            correo,
            salario
        );

        entrenadores.add(entrenador);

        if (conectadoBD) {

            String sql = "INSERT INTO entrenadores "
                    + "(id, nombre, especialidad, telefono, correo, "
                    + "salario, activo, foto_path) VALUES (?,?,?,?,?,?,?,?)";

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, entrenador.getId());
                ps.setString(2, entrenador.getNombre());
                ps.setString(3, entrenador.getEspecialidad());
                ps.setString(4, entrenador.getTelefono());
                ps.setString(5, entrenador.getCorreo());
                ps.setDouble(6, entrenador.getSalario());
                ps.setBoolean(7, entrenador.isActivo());
                ps.setString(8, entrenador.getFotoPath());

                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo guardar el entrenador en la "
                        + "base de datos: " + ex.getMessage()
                );
            }
        }

        return entrenador;
    }

    /** Guarda en la base de datos los cambios de un entrenador editado. */
    public void actualizarEntrenador(Entrenador entrenador) {

        if (!conectadoBD || entrenador == null) {
            return;
        }

        String sql = "UPDATE entrenadores SET nombre=?, especialidad=?, "
                + "telefono=?, correo=?, salario=?, activo=?, foto_path=? "
                + "WHERE id=?";

        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, entrenador.getNombre());
            ps.setString(2, entrenador.getEspecialidad());
            ps.setString(3, entrenador.getTelefono());
            ps.setString(4, entrenador.getCorreo());
            ps.setDouble(5, entrenador.getSalario());
            ps.setBoolean(6, entrenador.isActivo());
            ps.setString(7, entrenador.getFotoPath());
            ps.setInt(8, entrenador.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {

            System.err.println(
                    "No se pudo actualizar el entrenador en la "
                    + "base de datos: " + ex.getMessage()
            );
        }
    }

    public ArrayList<Entrenador> getEntrenadores() {
        return entrenadores;
    }

    public Entrenador buscarEntrenador(int id) {

        for (Entrenador entrenador : entrenadores) {

            if (entrenador.getId() == id) {
                return entrenador;
            }
        }

        return null;
    }

    public void eliminarEntrenador(int id) {

        Entrenador entrenador = buscarEntrenador(id);

        if (entrenador != null) {
            entrenadores.remove(entrenador);
        }

        if (conectadoBD) {

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM entrenadores WHERE id=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo eliminar el entrenador de la "
                        + "base de datos: " + ex.getMessage()
                );
            }
        }
    }

    // ==========================================
    // MEMBRESIAS
    // ==========================================

    public void agregarMembresia(
            String nombre,
            String descripcion,
            int duracionDias,
            double precio) {

        Membresia membresia = new Membresia(
            siguienteMembresia++,
            nombre,
            descripcion,
            duracionDias,
            precio
        );

        membresias.add(membresia);

        if (conectadoBD) {

            String sql = "INSERT INTO membresias "
                    + "(id, nombre, descripcion, duracion_dias, precio, activa) "
                    + "VALUES (?,?,?,?,?,?)";

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, membresia.getId());
                ps.setString(2, membresia.getNombre());
                ps.setString(3, membresia.getDescripcion());
                ps.setInt(4, membresia.getDuracionDias());
                ps.setDouble(5, membresia.getPrecio());
                ps.setBoolean(6, membresia.isActiva());

                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo guardar la membresía en la "
                        + "base de datos: " + ex.getMessage()
                );
            }
        }
    }

    /** Guarda en la base de datos los cambios de una membresía editada. */
    public void actualizarMembresia(Membresia membresia) {

        if (!conectadoBD || membresia == null) {
            return;
        }

        String sql = "UPDATE membresias SET nombre=?, descripcion=?, "
                + "duracion_dias=?, precio=?, activa=? WHERE id=?";

        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, membresia.getNombre());
            ps.setString(2, membresia.getDescripcion());
            ps.setInt(3, membresia.getDuracionDias());
            ps.setDouble(4, membresia.getPrecio());
            ps.setBoolean(5, membresia.isActiva());
            ps.setInt(6, membresia.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {

            System.err.println(
                    "No se pudo actualizar la membresía en la "
                    + "base de datos: " + ex.getMessage()
            );
        }
    }

    public ArrayList<Membresia> getMembresias() {
        return membresias;
    }

    public Membresia buscarMembresia(int id) {

        for (Membresia membresia : membresias) {

            if (membresia.getId() == id) {
                return membresia;
            }
        }

        return null;
    }

    public void eliminarMembresia(int id) {

        Membresia membresia = buscarMembresia(id);

        if (membresia != null) {
            membresias.remove(membresia);
        }

        if (conectadoBD) {

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM membresias WHERE id=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo eliminar la membresía de la "
                        + "base de datos: " + ex.getMessage()
                );
            }
        }
    }

    // ==========================================
    // PAGOS
    // ==========================================

    public void agregarPago(
            int socioId,
            String socioNombre,
            String membresia,
            double monto,
            String fecha,
            String metodoPago,
            String concepto) {

        Pago pago = new Pago(
            siguientePago++,
            socioId,
            socioNombre,
            membresia,
            monto,
            fecha,
            metodoPago,
            concepto
        );

        pagos.add(pago);

        if (conectadoBD) {

            String sql = "INSERT INTO pagos "
                    + "(id, socio_id, socio_nombre, membresia, monto, "
                    + "fecha, metodo_pago, concepto) VALUES (?,?,?,?,?,?,?,?)";

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, pago.getId());
                ps.setInt(2, pago.getSocioId());
                ps.setString(3, pago.getSocioNombre());
                ps.setString(4, pago.getMembresia());
                ps.setDouble(5, pago.getMonto());
                ps.setString(6, pago.getFecha());
                ps.setString(7, pago.getMetodoPago());
                ps.setString(8, pago.getConcepto());

                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo guardar el pago en la base de "
                        + "datos: " + ex.getMessage()
                );
            }
        }
    }

    /** Guarda en la base de datos los cambios de un pago editado. */
    public void actualizarPago(Pago pago) {

        if (!conectadoBD || pago == null) {
            return;
        }

        String sql = "UPDATE pagos SET socio_id=?, socio_nombre=?, "
                + "membresia=?, monto=?, fecha=?, metodo_pago=?, "
                + "concepto=? WHERE id=?";

        try (Connection con = Conexion.obtener();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pago.getSocioId());
            ps.setString(2, pago.getSocioNombre());
            ps.setString(3, pago.getMembresia());
            ps.setDouble(4, pago.getMonto());
            ps.setString(5, pago.getFecha());
            ps.setString(6, pago.getMetodoPago());
            ps.setString(7, pago.getConcepto());
            ps.setInt(8, pago.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {

            System.err.println(
                    "No se pudo actualizar el pago en la base de "
                    + "datos: " + ex.getMessage()
            );
        }
    }

    public ArrayList<Pago> getPagos() {
        return pagos;
    }

    public void eliminarPago(int id) {

        Pago encontrado = null;

        for (Pago p : pagos) {

            if (p.getId() == id) {
                encontrado = p;
                break;
            }
        }

        if (encontrado != null) {
            pagos.remove(encontrado);
        }

        if (conectadoBD) {

            try (Connection con = Conexion.obtener();
                    PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM pagos WHERE id=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();

            } catch (SQLException ex) {

                System.err.println(
                        "No se pudo eliminar el pago de la base "
                        + "de datos: " + ex.getMessage()
                );
            }
        }
    }

    public Pago buscarPago(int id) {

        for (Pago p : pagos) {

            if (p.getId() == id) {
                return p;
            }
        }

        return null;
    }

    // ==========================================
    // ESTADISTICAS
    // ==========================================

    public int totalSocios() {
        return socios.size();
    }

    public int sociosVencenPronto(int dias) {

        int total = 0;

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Calendar limite = java.util.Calendar.getInstance();
        limite.add(java.util.Calendar.DAY_OF_MONTH, dias);

        for (Socio s : socios) {

            try {

                java.util.Date fv = fmt.parse(s.getFechaVencimiento());

                if (fv.before(limite.getTime())) {
                    total++;
                }

            } catch (Exception ex) {
                // fecha no disponible, se ignora
            }
        }

        return total;
    }

    /** Socios cuya membresía ya venció (fecha de vencimiento anterior a hoy). */
    public int sociosVencidos() {
        return listaSociosVencidos().size();
    }

    public java.util.List<Socio> listaSociosVencidos() {

        java.util.List<Socio> lista = new ArrayList<>();

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Date hoy = new java.util.Date();

        for (Socio s : socios) {

            try {

                java.util.Date fv = fmt.parse(s.getFechaVencimiento());

                if (fv.before(hoy)) {
                    lista.add(s);
                }

            } catch (Exception ex) {
                // fecha no disponible, se ignora
            }
        }

        return lista;
    }

    /** Socios cuya membresía vence en los próximos "dias" días (sin contar los ya vencidos). */
    public java.util.List<Socio> listaSociosPorVencer(int dias) {

        java.util.List<Socio> lista = new ArrayList<>();

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Date hoy = new java.util.Date();

        java.util.Calendar limite = java.util.Calendar.getInstance();
        limite.add(java.util.Calendar.DAY_OF_MONTH, dias);

        for (Socio s : socios) {

            try {

                java.util.Date fv = fmt.parse(s.getFechaVencimiento());

                if (!fv.before(hoy) && fv.before(limite.getTime())) {
                    lista.add(s);
                }

            } catch (Exception ex) {
                // fecha no disponible, se ignora
            }
        }

        return lista;
    }

    public int totalEntrenadores() {
        return entrenadores.size();
    }

    public int entrenadoresActivos() {

        int c = 0;

        for (Entrenador e : entrenadores) {

            if (e.isActivo()) {
                c++;
            }
        }

        return c;
    }

    public int entrenadoresInactivos() {

        return totalEntrenadores() - entrenadoresActivos();
    }

    public double salarioPromedioEntrenadores() {

        if (entrenadores.isEmpty()) {
            return 0;
        }

        double suma = 0;

        for (Entrenador e : entrenadores) {
            suma += e.getSalario();
        }

        return suma / entrenadores.size();
    }

    public int totalMembresias() {
        return membresias.size();
    }

    public int totalPagos() {
        return pagos.size();
    }

    public double totalIngresos() {

        double total = 0;

        for (Pago pago : pagos) {
            total += pago.getMonto();
        }

        return total;
    }
}