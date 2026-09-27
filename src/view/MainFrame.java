package view;

import control.GimnasioControl;
import control.RegexControl;
import model.Socio;
import model.Entrenador;
import model.Membresia;
import model.Pago;
import com.toedter.calendar.JCalendar;
import control.CorreoSimple;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;

public class MainFrame extends JFrame {

    // =========================================================
    // COLORES
    // =========================================================

    private Color BG = new Color(10, 18, 28);
    private Color SIDE = new Color(11, 20, 30);
    private Color HEADER = new Color(17, 29, 41);
    private Color PANEL = new Color(24, 35, 48);
    private Color PANEL2 = new Color(29, 42, 57);
    private Color INPUT = new Color(13, 24, 36);

    private Color GOLD = new Color(245, 183, 35);
    private Color TEXT = new Color(241, 244, 248);
    private Color MUTED = new Color(164, 177, 191);

    private Color GREEN = new Color(62, 205, 123);
    private Color RED = new Color(225, 82, 82);
    private Color BLUE = new Color(76, 166, 235);
    private Color PURPLE = new Color(164, 105, 235);
    private Color BORDER = new Color(49, 65, 82);

    // Fondo del menú lateral al pasar el mouse. Antes era un azul
    // fijo que no cambiaba con el tema y, sobre el tema claro
    // (fondo blanco), se veía como un bloque oscuro fuera de lugar.
    // Ahora cambia junto con el tema: verde-crema suave en claro,
    // el mismo tono sutil de antes en oscuro.
    private Color HOVER_SIDE = new Color(30, 44, 59);

    private boolean cfgMostrarNotificaciones = true;
    // Apagada por defecto al iniciar el programa; se puede activar
    // desde Configuración > Apariencia si se quiere el efecto.
    private boolean cfgAnimaciones = false;
    private String cfgNivelSeguridad = "Alto";
    private boolean cfgPermitirSinMembresia = true;
    private boolean cfgMostrarHistorialPagos = true;
    private boolean cfgModoMantenimiento = false;

    private boolean cfgTemaClaro = false;
    private double cfgFontScale = 1.0;

    /**
     * Reemplaza a "f(...)" en toda la interfaz. Con esto,
     * cambiar el tamaño de fuente en Configuración sí afecta a
     * todos los textos, no solo a los nuevos.
     */
    private Font f(String familia, int estilo, int tamano) {

        int escalado =
                Math.max(8, (int) Math.round(tamano * cfgFontScale));

        return new Font(familia, estilo, escalado);
    }

    /**
     * Cambia la paleta completa (oscuro/claro) y reconstruye toda
     * la interfaz (sidebar, header y la página actual) para que el
     * cambio se note de verdad, no solo en las pantallas nuevas.
     */
    private void aplicarTema(boolean claro) {

        fijarPaleta(claro);

        String seccionActual = seccionVisible;

        construirInterfaz();

        iniciarRelojYCierreAutomatico();

        irASeccion(seccionActual);

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    /**
     * Solo cambia los colores (sin reconstruir la interfaz). Se usa
     * en el constructor, antes de que exista ninguna pantalla que
     * reconstruir; y desde aplicarTema(), que sí reconstruye todo
     * después de llamar a este método.
     */
    private void fijarPaleta(boolean claro) {

        cfgTemaClaro = claro;

        if (claro) {

            // Tema claro en tonos crema/hueso, no blanco puro: así
            // el fondo, las tarjetas y los bordes se distinguen
            // entre sí (con blanco puro sobre blanco puro no se
            // notaba dónde empezaba cada tarjeta ni las líneas).
            BG = new Color(240, 237, 229);        // fondo general: crema
            SIDE = new Color(250, 248, 243);      // menú lateral: hueso muy claro
            HEADER = new Color(250, 248, 243);    // barra superior: igual al menú
            PANEL = new Color(255, 255, 255);     // tarjetas: blanco, resalta sobre el crema
            PANEL2 = new Color(233, 229, 218);    // paneles internos: crema más marcado
            INPUT = new Color(255, 255, 255);     // campos de texto: blanco

            TEXT = new Color(35, 38, 33);
            MUTED = new Color(112, 112, 100);
            BORDER = new Color(199, 193, 178);    // líneas: crema oscuro, ya se ven bien

            // Hover del menú: verde-crema suave, no azul.
            HOVER_SIDE = new Color(224, 238, 222);

        } else {

            BG = new Color(10, 18, 28);
            SIDE = new Color(11, 20, 30);
            HEADER = new Color(17, 29, 41);
            PANEL = new Color(24, 35, 48);
            PANEL2 = new Color(29, 42, 57);
            INPUT = new Color(13, 24, 36);

            TEXT = new Color(241, 244, 248);
            MUTED = new Color(164, 177, 191);
            BORDER = new Color(49, 65, 82);

            HOVER_SIDE = new Color(30, 44, 59);
        }

        // GOLD, GREEN, RED, BLUE, PURPLE se mantienen: son colores
        // de acento y deben verse igual en ambos temas.
    }

    /** Recuerda qué pantalla está abierta para poder redibujarla tras un cambio de tema. */
    private String seccionVisible = "inicio";

    // ---- Paginación de la tabla de socios ----
    private static final int SOCIOS_POR_PAGINA = 10;
    private int paginaSocios = 1;
    private int totalSociosFiltrados = 0;

    private void irASeccion(String seccion) {

        switch (seccion) {

            case "socios":
                mostrarSocios();
                break;

            case "membresias":
                mostrarMembresias();
                break;

            case "pagos":
                mostrarPagos();
                break;

            case "entrenadores":
                mostrarEntrenadores();
                break;

            case "reportes":
                mostrarReportes();
                break;

            case "configuracion":
                mostrarConfiguracion();
                break;

            default:
                mostrarInicio();
        }
    }


    // =========================================================
    // VARIABLES
    // =========================================================

    private final GimnasioControl control = GimnasioControl.obtenerInstancia();

    private JPanel content;

    private java.util.Map<String, ImageIcon> fotoCache =
            new java.util.HashMap<>();
    private JLabel pageTitle;
    private JButton[] navButtons;

    // ---- Configuración persistente de la sesión ----

    private String cfgNombreGym = "Golden Gym";
    private String cfgDireccion = "Av. Principal 123, Ciudad";
    private String cfgTelefono = "1234-5678";
    private String cfgCorreo = "info@gym.com";
    private String cfgHorario = "Lun - Vie 6:00 - 22:00 | Sáb 7:00 - 18:00";

    private boolean cfgMostrarHora = true;
    private boolean cfgCierreAutomatico = true;
    private int cfgMinutosInactividad = 5;

    private String cfgCorreoRespaldo = "";

    // ---- Respaldo automático ----
    private boolean cfgRespaldoAuto = true;
    private String cfgRespaldoFrecuencia = "Diario";
    // Hora guardada en formato 24 h (0-23); en pantalla se muestra con AM/PM.
    private int cfgRespaldoHora = 2;
    private int cfgRespaldoMinuto = 0;
    private String ultimoRespaldoClave = "";
    private javax.swing.Timer timerRespaldo;

    // ---- Envío de tickets por correo ----

    private boolean cfgImprimirComprobantes = true;
    private String cfgSmtpCorreo = "";
    private String cfgSmtpPassword = "";
    private String cfgSmtpHost = "smtp.gmail.com";
    private String cfgSmtpPuerto = "587";

    private JLabel headerClock;
    private JLabel sidebarTitulo1;
    private JLabel sidebarTitulo2;

    private long ultimaActividad = System.currentTimeMillis();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MainFrame() {

        setTitle("GYM SYSTEM - Administración");
        setSize(1450, 850);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Antes de dibujar nada: si hay una configuración guardada
        // de una sesión anterior (correo SMTP, tema, datos del
        // gimnasio, etc.), se recupera aquí. Así no hay que volver
        // a escribirla cada vez que se abre el programa.
        cargarConfiguracionGuardada();

        fijarPaleta(cfgTemaClaro);

        Login.MODO_MANTENIMIENTO = cfgModoMantenimiento;

        construirInterfaz();

        inicializarFade();

        mostrarInicio();

        iniciarRelojYCierreAutomatico();

        iniciarRespaldoAutomatico();
    }

    // =========================================================
    // CONFIGURACIÓN GUARDADA EN DISCO
    // =========================================================
    //
    // Antes, toda la configuración (correo SMTP, datos del
    // respaldo, tema, nombre del gimnasio...) vivía solo en
    // memoria y se perdía al cerrar el programa. Ahora se guarda
    // en un archivo de texto en la carpeta del usuario, y se
    // recupera automáticamente cada vez que se abre el programa.
    // Se mantiene hasta que alguien la borre a mano: apagando el
    // programa no desaparece.
    //
    // La contraseña del correo queda guardada en texto plano en
    // ese archivo. Para un proyecto escolar es aceptable, pero no
    // es así como se haría en un sistema real (ahí se cifraría).

    private static final java.io.File ARCHIVO_CONFIG = new java.io.File(
            System.getProperty("user.home"),
            ".golden_gym" + java.io.File.separator + "config.properties"
    );

    private void guardarConfiguracionEnDisco() {

        java.util.Properties p = new java.util.Properties();

        p.setProperty("nombreGym", cfgNombreGym);
        p.setProperty("direccion", cfgDireccion);
        p.setProperty("telefono", cfgTelefono);
        p.setProperty("correo", cfgCorreo);
        p.setProperty("horario", cfgHorario);

        p.setProperty("mostrarHora", String.valueOf(cfgMostrarHora));
        p.setProperty("cierreAutomatico", String.valueOf(cfgCierreAutomatico));
        p.setProperty("minutosInactividad", String.valueOf(cfgMinutosInactividad));

        p.setProperty("correoRespaldo", cfgCorreoRespaldo);
        p.setProperty("respaldoAuto", String.valueOf(cfgRespaldoAuto));
        p.setProperty("respaldoFrecuencia", cfgRespaldoFrecuencia);
        p.setProperty("respaldoHora", String.valueOf(cfgRespaldoHora));
        p.setProperty("respaldoMinuto", String.valueOf(cfgRespaldoMinuto));

        p.setProperty("imprimirComprobantes", String.valueOf(cfgImprimirComprobantes));
        p.setProperty("smtpCorreo", cfgSmtpCorreo);
        p.setProperty("smtpPassword", cfgSmtpPassword);
        p.setProperty("smtpHost", cfgSmtpHost);
        p.setProperty("smtpPuerto", cfgSmtpPuerto);

        p.setProperty("mostrarNotificaciones", String.valueOf(cfgMostrarNotificaciones));
        p.setProperty("animaciones", String.valueOf(cfgAnimaciones));
        p.setProperty("nivelSeguridad", cfgNivelSeguridad);
        p.setProperty("permitirSinMembresia", String.valueOf(cfgPermitirSinMembresia));
        p.setProperty("mostrarHistorialPagos", String.valueOf(cfgMostrarHistorialPagos));
        p.setProperty("modoMantenimiento", String.valueOf(cfgModoMantenimiento));
        p.setProperty("temaClaro", String.valueOf(cfgTemaClaro));
        p.setProperty("fontScale", String.valueOf(cfgFontScale));

        try {

            ARCHIVO_CONFIG.getParentFile().mkdirs();

            try (java.io.OutputStream out =
                    new java.io.FileOutputStream(ARCHIVO_CONFIG)) {

                p.store(out, "Configuración de Golden Gym");
            }

        } catch (java.io.IOException ex) {

            // Si no se pudo guardar en disco, el programa sigue
            // funcionando con la configuración solo en memoria.
            System.err.println(
                    "No se pudo guardar la configuración: "
                    + ex.getMessage()
            );
        }
    }

    private void cargarConfiguracionGuardada() {

        if (!ARCHIVO_CONFIG.exists()) {
            return;
        }

        java.util.Properties p = new java.util.Properties();

        try (java.io.InputStream in =
                new java.io.FileInputStream(ARCHIVO_CONFIG)) {

            p.load(in);

        } catch (java.io.IOException ex) {

            System.err.println(
                    "No se pudo leer la configuración guardada: "
                    + ex.getMessage()
            );

            return;
        }

        cfgNombreGym = p.getProperty("nombreGym", cfgNombreGym);
        cfgDireccion = p.getProperty("direccion", cfgDireccion);
        cfgTelefono = p.getProperty("telefono", cfgTelefono);
        cfgCorreo = p.getProperty("correo", cfgCorreo);
        cfgHorario = p.getProperty("horario", cfgHorario);

        cfgMostrarHora = Boolean.parseBoolean(
                p.getProperty("mostrarHora", String.valueOf(cfgMostrarHora)));
        cfgCierreAutomatico = Boolean.parseBoolean(
                p.getProperty("cierreAutomatico", String.valueOf(cfgCierreAutomatico)));

        cfgMinutosInactividad = parseIntSeguro(
                p.getProperty("minutosInactividad"), cfgMinutosInactividad);

        cfgCorreoRespaldo = p.getProperty("correoRespaldo", cfgCorreoRespaldo);
        cfgRespaldoAuto = Boolean.parseBoolean(
                p.getProperty("respaldoAuto", String.valueOf(cfgRespaldoAuto)));
        cfgRespaldoFrecuencia = p.getProperty("respaldoFrecuencia", cfgRespaldoFrecuencia);
        cfgRespaldoHora = parseIntSeguro(p.getProperty("respaldoHora"), cfgRespaldoHora);
        cfgRespaldoMinuto = parseIntSeguro(p.getProperty("respaldoMinuto"), cfgRespaldoMinuto);

        cfgImprimirComprobantes = Boolean.parseBoolean(
                p.getProperty("imprimirComprobantes", String.valueOf(cfgImprimirComprobantes)));
        cfgSmtpCorreo = p.getProperty("smtpCorreo", cfgSmtpCorreo);
        cfgSmtpPassword = p.getProperty("smtpPassword", cfgSmtpPassword);
        cfgSmtpHost = p.getProperty("smtpHost", cfgSmtpHost);
        cfgSmtpPuerto = p.getProperty("smtpPuerto", cfgSmtpPuerto);

        cfgMostrarNotificaciones = Boolean.parseBoolean(
                p.getProperty("mostrarNotificaciones", String.valueOf(cfgMostrarNotificaciones)));
        cfgAnimaciones = Boolean.parseBoolean(
                p.getProperty("animaciones", String.valueOf(cfgAnimaciones)));
        cfgNivelSeguridad = p.getProperty("nivelSeguridad", cfgNivelSeguridad);
        cfgPermitirSinMembresia = Boolean.parseBoolean(
                p.getProperty("permitirSinMembresia", String.valueOf(cfgPermitirSinMembresia)));
        cfgMostrarHistorialPagos = Boolean.parseBoolean(
                p.getProperty("mostrarHistorialPagos", String.valueOf(cfgMostrarHistorialPagos)));
        cfgModoMantenimiento = Boolean.parseBoolean(
                p.getProperty("modoMantenimiento", String.valueOf(cfgModoMantenimiento)));
        cfgTemaClaro = Boolean.parseBoolean(
                p.getProperty("temaClaro", String.valueOf(cfgTemaClaro)));

        try {
            cfgFontScale = Double.parseDouble(
                    p.getProperty("fontScale", String.valueOf(cfgFontScale)));
        } catch (NumberFormatException ignorado) {
            // se deja el valor por defecto
        }
    }

    private int parseIntSeguro(String valor, int porDefecto) {

        if (valor == null) {
            return porDefecto;
        }

        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    // =========================================================
    // ANIMACIÓN DE TRANSICIÓN ENTRE PANTALLAS
    // =========================================================

    private float fadeAlpha = 0f;
    private JPanel fadeGlass;

    private void inicializarFade() {

        fadeGlass = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                if (fadeAlpha <= 0f) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setComposite(
                        AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER, fadeAlpha
                        )
                );

                g2.setColor(BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();
            }
        };

        fadeGlass.setOpaque(false);

        setGlassPane(fadeGlass);
    }

    /** Se activa cada vez que se cambia de sección, solo si el
     *  interruptor "Animaciones" de Configuración está encendido. */
    private void animarTransicion() {

        if (!cfgAnimaciones || fadeGlass == null) {
            return;
        }

        fadeAlpha = 0.55f;

        fadeGlass.setVisible(true);
        fadeGlass.repaint();

        // Animación basada en tiempo real (no en "pasos" fijos), con
        // una curva de salida suave (ease-out) y un timer más rápido
        // (~120 fps). Antes bajaba 0.1 de alpha cada 15ms sin importar
        // cuánto tardara realmente cada fotograma, así que en equipos
        // lentos se sentía a trompicones; ahora la duración total es
        // siempre la misma (220ms) y se ve fluida sin importar el fps
        // real que logre la máquina.

        final long inicio = System.nanoTime();
        final long duracionNs = 220_000_000L;
        final float alphaInicial = fadeAlpha;

        javax.swing.Timer t = new javax.swing.Timer(8, null);

        t.addActionListener(e -> {

            double progreso =
                    (System.nanoTime() - inicio) / (double) duracionNs;

            if (progreso >= 1.0) {

                fadeAlpha = 0f;
                fadeGlass.setVisible(false);

                ((javax.swing.Timer) e.getSource()).stop();

            } else {

                // Ease-out cúbico: arranca rápido y frena suave.
                double avance = 1 - Math.pow(1 - progreso, 3);

                fadeAlpha = (float) (alphaInicial * (1 - avance));
            }

            fadeGlass.repaint();
        });

        t.start();
    }

    // =========================================================
    // RELOJ DE HEADER + CIERRE POR INACTIVIDAD
    // =========================================================

    private void iniciarRelojYCierreAutomatico() {

        try {

            Toolkit.getDefaultToolkit().addAWTEventListener(
                    ev -> ultimaActividad = System.currentTimeMillis(),
                    java.awt.AWTEvent.MOUSE_EVENT_MASK
                    | java.awt.AWTEvent.KEY_EVENT_MASK
                    | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK
            );

        } catch (SecurityException ignored) {
        }

        javax.swing.Timer reloj = new javax.swing.Timer(1000, e -> {

            headerClock.setText(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                            .format(new Date())
            );

            headerClock.setVisible(cfgMostrarHora);

            if (cfgCierreAutomatico) {

                long inactivoMs =
                        System.currentTimeMillis() - ultimaActividad;

                if (inactivoMs > (long) cfgMinutosInactividad * 60_000L) {

                    ((javax.swing.Timer) e.getSource()).stop();

                    cerrarSesionPorInactividad();
                }
            }
        });

        reloj.start();
    }

    private void cerrarSesionPorInactividad() {

        dispose();

        JOptionPane.showMessageDialog(
                null,
                "Sesión cerrada por inactividad.",
                "Sesión finalizada",
                JOptionPane.INFORMATION_MESSAGE
        );

        new Login().setVisible(true);
    }

    // =========================================================
    // CONSTRUIR INTERFAZ
    // =========================================================

    private void construirInterfaz() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        root.add(crearSidebar(), BorderLayout.WEST);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(BG);

        centro.add(crearHeader(), BorderLayout.NORTH);

        content = new JPanel(new BorderLayout());
        content.setBackground(BG);

        centro.add(content, BorderLayout.CENTER);

        root.add(centro, BorderLayout.CENTER);

        setContentPane(root);
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private JPanel crearSidebar() {

        JPanel side = new JPanel(new BorderLayout());

        side.setPreferredSize(new Dimension(260, 0));
        side.setBackground(SIDE);

        side.setBorder(
                new LineBorder(
                        new Color(31, 46, 61),
                        1
                )
        );

        JPanel top = new JPanel();

        top.setOpaque(false);

        top.setLayout(
                new BoxLayout(
                        top,
                        BoxLayout.Y_AXIS
                )
        );

        top.setBorder(
                new EmptyBorder(
                        24,
                        18,
                        15,
                        18
                )
        );

        // -----------------------------------------------------
        // LOGO
        // -----------------------------------------------------

        JPanel logo = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        7,
                        0
                )
        );

        logo.setOpaque(false);

        logo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        logo.add(
                new JLabel(
                        new Icono(
                                Icono.DUMBBELL,
                                GOLD,
                                32
                        )
                )
        );

        sidebarTitulo1 = new JLabel("GOLDEN ");

        sidebarTitulo1.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        sidebarTitulo1.setForeground(TEXT);

        sidebarTitulo2 = new JLabel("GYM");

        sidebarTitulo2.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        sidebarTitulo2.setForeground(GOLD);

        logo.add(sidebarTitulo1);
        logo.add(sidebarTitulo2);

        JLabel sub = new JLabel(
                "SISTEMA DE GESTIÓN"
        );

        sub.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        9
                )
        );

        sub.setForeground(MUTED);

        sub.setBorder(
                new EmptyBorder(
                        3,
                        40,
                        0,
                        0
                )
        );

        sub.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        top.add(logo);
        top.add(sub);

        top.add(
                Box.createVerticalStrut(30)
        );

        // -----------------------------------------------------
        // BOTONES
        // -----------------------------------------------------

        String[] nombres = {
            "Inicio",
            "Socios",
            "Membresías",
            "Pagos",
            "Entrenadores",
            "Reportes",
            "Configuración"
        };

        int[] iconos = {
            Icono.HOME,
            Icono.USER,
            Icono.CARD,
            Icono.MONEY,
            Icono.TRAINER,
            Icono.CHART,
            Icono.GEAR
        };

        navButtons = new JButton[nombres.length];

        for (int i = 0; i < nombres.length; i++) {

            final String nombre = nombres[i];

            navButtons[i] =
                    crearNav(
                            nombre,
                            iconos[i]
                    );

            navButtons[i].addActionListener(
                    e -> navegar(nombre)
            );

            top.add(navButtons[i]);

            top.add(
                    Box.createVerticalStrut(7)
            );
        }

        side.add(
                top,
                BorderLayout.NORTH
        );

        // -----------------------------------------------------
        // CERRAR SESIÓN
        // -----------------------------------------------------

        JPanel bottom =
                new JPanel(
                        new BorderLayout()
                );

        bottom.setOpaque(false);

        bottom.setBorder(
                new EmptyBorder(
                        15,
                        18,
                        22,
                        18
                )
        );

        JButton salir =
                crearNav(
                        "Cerrar sesión",
                        Icono.LOGOUT
                );

        salir.addActionListener(
                e -> cerrarSesion()
        );

        bottom.add(
                salir,
                BorderLayout.CENTER
        );

        side.add(
                bottom,
                BorderLayout.SOUTH
        );

        return side;
    }

    // =========================================================
    // CREAR BOTÓN SIDEBAR
    // =========================================================

    private JButton crearNav(
            String texto,
            int icono
    ) {

        JButton b =
                new JButton(
                        texto,
                        new Icono(
                                icono,
                                MUTED,
                                20
                        )
                );

        b.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        46
                )
        );

        b.setPreferredSize(
                new Dimension(
                        220,
                        46
                )
        );

        b.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        b.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        b.setIconTextGap(14);

        b.setBorder(
                new EmptyBorder(
                        0,
                        13,
                        0,
                        10
                )
        );

        b.setFocusPainted(false);
        b.setBorderPainted(false);

        b.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );

        b.setForeground(TEXT);
        b.setBackground(SIDE);
        b.setOpaque(true);

        b.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            MouseEvent e
                    ) {

                        if (b.getBackground().equals(SIDE)) {

                            b.setBackground(HOVER_SIDE);
                        }
                    }

                    @Override
                    public void mouseExited(
                            MouseEvent e
                    ) {

                        if (!b.getBackground().equals(GOLD)) {

                            b.setBackground(SIDE);
                        }
                    }
                }
        );

        return b;
    }

    // =========================================================
    // ACTIVAR BOTÓN
    // =========================================================

    private void activarNav(int indice) {

        for (int i = 0;
                i < navButtons.length;
                i++) {

            boolean activo =
                    i == indice;

            navButtons[i].setBackground(
                    activo
                            ? GOLD
                            : SIDE
            );

            navButtons[i].setForeground(
                    activo
                            ? Color.BLACK
                            : TEXT
            );

            if (navButtons[i].getIcon()
                    instanceof Icono) {

                ((Icono) navButtons[i]
                        .getIcon())
                        .setColor(
                                activo
                                        ? Color.BLACK
                                        : MUTED
                        );
            }
        }
    }

    // =========================================================
    // NAVEGACIÓN
    // =========================================================

    private void navegar(
            String nombre
    ) {

        if (nombre.equals("Inicio")) {

            mostrarInicio();

        } else if (nombre.equals("Socios")) {

            paginaSocios = 1;

            mostrarSocios();

        } else if (nombre.equals("Membresías")) {

            mostrarMembresias();

        } else if (nombre.equals("Pagos")) {

            mostrarPagos();

        } else if (nombre.equals("Entrenadores")) {

            mostrarEntrenadores();

        } else if (nombre.equals("Reportes")) {

            mostrarReportes();

        } else if (nombre.equals("Configuración")) {

            mostrarConfiguracion();
        }
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel crearHeader() {

        JPanel bar =
                new JPanel(
                        new BorderLayout()
                );

        bar.setPreferredSize(
                new Dimension(
                        0,
                        74
                )
        );

        bar.setBackground(HEADER);

        bar.setBorder(
                new EmptyBorder(
                        0,
                        25,
                        0,
                        25
                )
        );

        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        left.setOpaque(false);

        JButton menu =
                new JButton(
                        new Icono(
                                Icono.MENU,
                                TEXT,
                                23
                        )
                );

        menu.setPreferredSize(
                new Dimension(
                        42,
                        42
                )
        );

        menu.setContentAreaFilled(false);
        menu.setBorderPainted(false);
        menu.setFocusPainted(false);

        menu.addActionListener(
                e -> {

                    Component side =
                            getContentPane()
                                    .getComponent(0);

                    side.setVisible(
                            !side.isVisible()
                    );

                    revalidate();
                }
        );

        pageTitle =
                new JLabel(
                        "Panel principal"
                );

        pageTitle.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        pageTitle.setForeground(TEXT);

        pageTitle.setBorder(
                new EmptyBorder(
                        0,
                        12,
                        0,
                        0
                )
        );

        left.add(menu);
        left.add(pageTitle);

        JPanel user =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );

        user.setOpaque(false);

        headerClock = new JLabel();

        headerClock.setForeground(MUTED);

        headerClock.setFont(
                f("SansSerif", Font.PLAIN, 12)
        );

        headerClock.setBorder(
                new EmptyBorder(0, 0, 0, 14)
        );

        headerClock.setVisible(cfgMostrarHora);

        user.add(headerClock);

        user.add(
                new JLabel(
                        new Icono(
                                Icono.USER_CIRCLE,
                                MUTED,
                                36
                        )
                )
        );

        JPanel userText =
                new JPanel();

        userText.setOpaque(false);

        userText.setLayout(
                new BoxLayout(
                        userText,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel admin =
                new JLabel(
                        "Administrador"
                );

        admin.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        admin.setForeground(TEXT);

        JLabel usuario =
                new JLabel(Login.USUARIO_ACTUAL);

        usuario.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        usuario.setForeground(MUTED);

        userText.add(admin);
        userText.add(usuario);

        user.add(userText);

        user.add(
                new JLabel(
                        new Icono(
                                Icono.DOWN,
                                MUTED,
                                14
                        )
                )
        );

        user.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPopupMenu menuUsuario = new JPopupMenu();

        JMenuItem itemCrear = new JMenuItem("Crear nueva cuenta");
        itemCrear.addActionListener(e -> dialogoCrearCuenta());

        JMenuItem itemPass = new JMenuItem("Cambiar mi contraseña");
        itemPass.addActionListener(e -> dialogoCambiarPassword());

        JMenuItem itemSalir = new JMenuItem("Cerrar sesión");
        itemSalir.addActionListener(e -> cerrarSesion());

        menuUsuario.add(itemCrear);
        menuUsuario.add(itemPass);
        menuUsuario.addSeparator();
        menuUsuario.add(itemSalir);

        user.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        menuUsuario.show(user, 0, user.getHeight());
                    }
                }
        );

        bar.add(
                left,
                BorderLayout.WEST
        );

        bar.add(
                user,
                BorderLayout.EAST
        );

        return bar;
    }

    // =========================================================
    // INICIO
    // =========================================================

    private void mostrarInicio() {

        seccionVisible = "inicio";

        activarNav(0);

        pageTitle.setText(
                "Panel principal"
        );

        JPanel page = base();

        // -----------------------------------------------------
        // BIENVENIDA (BANNER)
        // -----------------------------------------------------

        JPanel banner = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D x = (Graphics2D) g.create();

                x.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                int w = getWidth();
                int h = getHeight();

                // Antes este degradado quedaba fijo en tonos oscuros
                // azulados sin importar el tema, así que en "Claro"
                // el banner de bienvenida se quedaba viéndose azul
                // oscuro en vez de adaptarse. Ahora cambia con
                // cfgTemaClaro: oscuro normal, o dorado hacia blanco
                // en el tema claro.

                GradientPaint fondo = cfgTemaClaro
                        ? new GradientPaint(
                                0, 0, new Color(255, 236, 178),
                                w, h, Color.WHITE
                        )
                        : new GradientPaint(
                                0, 0, new Color(22, 28, 37),
                                w, h, new Color(9, 12, 17)
                        );

                x.setPaint(fondo);
                x.fillRoundRect(0, 0, w, h, 14, 14);

                x.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 14, 14));

                int bandaX = Math.max(w - 230, w * 3 / 5);

                x.setColor(new Color(245, 183, 35, cfgTemaClaro ? 90 : 70));
                x.setStroke(new BasicStroke(28f));
                x.drawLine(bandaX + 20, h + 20, bandaX + 250, -20);

                x.setColor(new Color(245, 183, 35, cfgTemaClaro ? 160 : 130));
                x.setStroke(new BasicStroke(3f));
                x.drawLine(bandaX, h + 10, bandaX + 220, -10);

                x.setColor(BORDER);
                x.setStroke(new BasicStroke(1f));
                x.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                x.dispose();
            }
        };

        banner.setOpaque(false);

        banner.setBorder(
                new EmptyBorder(22, 26, 22, 26)
        );

        banner.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 155)
        );

        banner.setPreferredSize(
                new Dimension(1000, 155)
        );

        banner.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        // ---- Izquierda: ícono + textos ----

        JPanel bIzq = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 18, 0)
        );

        bIzq.setOpaque(false);

        JLabel dumbbell = new JLabel(
                new Icono(Icono.DUMBBELL, GOLD, 46)
        );

        bIzq.add(dumbbell);

        JPanel bienvenida = new JPanel();

        bienvenida.setOpaque(false);

        bienvenida.setLayout(
                new BoxLayout(bienvenida, BoxLayout.Y_AXIS)
        );

        JLabel bTag = new JLabel("BIENVENIDO,");

        bTag.setForeground(MUTED);

        bTag.setFont(
                f("SansSerif", Font.BOLD, 11)
        );

        JLabel b2 = new JLabel("Administrador");

        b2.setFont(
                f("SansSerif", Font.BOLD, 27)
        );

        b2.setForeground(TEXT);

        JLabel desc = new JLabel(
                "Aquí tienes un resumen de lo que sucede en tu gimnasio."
        );

        desc.setFont(
                f("SansSerif", Font.PLAIN, 13)
        );

        desc.setForeground(MUTED);

        JLabel tagline = new JLabel(
                "Disciplina hoy, resultados mañana."
        );

        tagline.setFont(
                f("SansSerif", Font.ITALIC + Font.BOLD, 12)
        );

        tagline.setForeground(GOLD);

        bienvenida.add(bTag);
        bienvenida.add(b2);
        bienvenida.add(Box.createVerticalStrut(4));
        bienvenida.add(desc);
        bienvenida.add(Box.createVerticalStrut(6));
        bienvenida.add(tagline);

        bIzq.add(bienvenida);

        banner.add(bIzq, BorderLayout.WEST);

        // ---- Derecha: fecha/hora + estado ----

        JPanel bDer = new JPanel();

        bDer.setOpaque(false);

        bDer.setLayout(
                new BoxLayout(bDer, BoxLayout.Y_AXIS)
        );

        SimpleDateFormat sdfBanner =
                new SimpleDateFormat("dd/MM/yyyy HH:mm");

        JLabel fechaPill = new JLabel(
                sdfBanner.format(new Date()),
                new Icono(Icono.CALENDAR, MUTED, 14),
                SwingConstants.LEFT
        ) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D x = (Graphics2D) g.create();

                x.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                x.setColor(new Color(255, 255, 255, 18));

                x.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                x.dispose();

                super.paintComponent(g);
            }
        };

        fechaPill.setForeground(TEXT);

        fechaPill.setFont(
                f("SansSerif", Font.BOLD, 12)
        );

        fechaPill.setIconTextGap(8);

        fechaPill.setBorder(
                new EmptyBorder(8, 14, 8, 16)
        );

        fechaPill.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel estadoLinea = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 6, 0)
        );

        estadoLinea.setOpaque(false);

        estadoLinea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel dot = new JLabel(
                new Icono(Icono.CHECK, GREEN, 10)
        );

        JLabel sistemaActivo = new JLabel("Sistema activo");

        sistemaActivo.setForeground(GREEN);

        sistemaActivo.setFont(
                f("SansSerif", Font.BOLD, 11)
        );

        estadoLinea.add(dot);
        estadoLinea.add(sistemaActivo);

        bDer.add(fechaPill);
        bDer.add(Box.createVerticalStrut(10));
        bDer.add(estadoLinea);

        banner.add(bDer, BorderLayout.EAST);

        page.add(banner);

        page.add(
                Box.createVerticalStrut(20)
        );

        // -----------------------------------------------------
        // TARJETAS
        // -----------------------------------------------------

        JPanel stats =
                new JPanel(
                        new GridLayout(
                                1,
                                6,
                                14,
                                0
                        )
                );

        stats.setOpaque(false);

        stats.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        stats.setPreferredSize(
                new Dimension(
                        1000,
                        130
                )
        );

        stats.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        130
                )
        );

        stats.add(
                stat(
                        "Total de Socios",
                        control.totalSocios(),
                        deltaMes(altasSociosEsteMes()),
                        GOLD,
                        Icono.USER_GROUP
                )
        );

        stats.add(
                stat(
                        "Socios Activos",
                        contarActivos(),
                        contarActivos() + " de " + control.totalSocios(),
                        GREEN,
                        Icono.CHECK_USER
                )
        );

        stats.add(
                stat(
                        "Socios Suspendidos",
                        control.totalSocios()
                                - contarActivos(),
                        "Sin cambios",
                        RED,
                        Icono.BLOCK
                )
        );

        stats.add(
                stat(
                        "Entrenadores",
                        control.totalEntrenadores(),
                        control.entrenadoresActivos() + " activos",
                        BLUE,
                        Icono.TRAINER
                )
        );

        stats.add(
                stat(
                        "Membresías",
                        control.totalMembresias(),
                        "Planes disponibles",
                        PURPLE,
                        Icono.CARD
                )
        );

        stats.add(
                stat(
                        "Ingresos Totales",
                        String.format(
                                "$ %.2f",
                                control.totalIngresos()
                        ),
                        String.format(
                                "$ %.2f este mes",
                                ingresosEsteMes()
                        ),
                        GREEN,
                        Icono.MONEY
                )
        );

        page.add(stats);

        page.add(
                Box.createVerticalStrut(20)
        );

        // -----------------------------------------------------
        // GRÁFICA + PAGOS
        // -----------------------------------------------------

        JPanel medio =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                17,
                                0
                        )
                );

        medio.setOpaque(false);

        medio.setPreferredSize(
                new Dimension(
                        1000,
                        300
                )
        );

        medio.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        300
                )
        );

        medio.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JPanel grafica = card();

        grafica.setLayout(
                new BorderLayout()
        );

        grafica.setBorder(
                new EmptyBorder(
                        16,
                        18,
                        12,
                        18
                )
        );

        grafica.add(
                sectionTitle(
                        "Ingresos de los últimos 6 meses",
                        ""
                ),
                BorderLayout.NORTH
        );

        grafica.add(
                new Chart(),
                BorderLayout.CENTER
        );

        medio.add(grafica);

        JPanel pagos = card();

        pagos.setLayout(
                new BorderLayout()
        );

        pagos.setBorder(
                new EmptyBorder(
                        16,
                        18,
                        12,
                        18
                )
        );

        JPanel ph =
                new JPanel(
                        new BorderLayout()
                );

        ph.setOpaque(false);

        ph.add(
                sectionTitle(
                        "Últimos Pagos",
                        ""
                ),
                BorderLayout.WEST
        );

        JLabel ver =
                new JLabel(
                        "Ver todos"
                );

        ver.setForeground(GOLD);

        ver.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        ver.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        ver.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            MouseEvent e
                    ) {

                        mostrarPagos();
                    }
                }
        );

        ph.add(
                ver,
                BorderLayout.EAST
        );

        pagos.add(
                ph,
                BorderLayout.NORTH
        );

        String[] columnas = {
            "Socio",
            "Membresía",
            "Monto",
            "Fecha"
        };

        DefaultTableModel modelo =
                modelo(columnas);

        int inicio =
                Math.max(
                        0,
                        control.getPagos().size() - 5
                );

        for (
                int i =
                control.getPagos().size() - 1;
                i >= inicio;
                i--
        ) {

            Pago p =
                    control.getPagos().get(i);

            modelo.addRow(
                    new Object[]{
                        p.getSocioNombre(),
                        p.getMembresia(),
                        String.format(
                                "$%.2f",
                                p.getMonto()
                        ),
                        p.getFecha()
                    }
            );
        }

        JTable tablaUltimosPagos = tabla(modelo);

        java.util.Map<String, Color> coloresMembresia = new java.util.HashMap<>();

        coloresMembresia.put("Premium", GOLD);
        coloresMembresia.put("Básica", BLUE);
        coloresMembresia.put("Trimestral", PURPLE);
        coloresMembresia.put("Semestral", GREEN);
        coloresMembresia.put("Mensual", BLUE);
        coloresMembresia.put("Anual", GREEN);
        coloresMembresia.put("Estudiante", RED);

        tablaUltimosPagos.getColumnModel()
                .getColumn(1)
                .setCellRenderer(
                        new PillRenderer(coloresMembresia)
                );

        // Antes esta tabla tenía su propio JScrollPane: al poner el
        // cursor encima y bajar la rueda, ese scroll interno
        // atrapaba el evento (aunque no tuviera nada que desplazar)
        // y la página completa dejaba de moverse. Con tablaSinScroll
        // (mismo helper que ya usan Pagos/Entrenadores/Reportes) la
        // tabla no tiene scroll propio y la rueda siempre sube al
        // scroll de la página.

        JPanel pagosWrap = tablaSinScroll(tablaUltimosPagos);

        pagosWrap.setBackground(PANEL);

        pagos.add(
                pagosWrap,
                BorderLayout.CENTER
        );

        medio.add(pagos);

        page.add(medio);

        page.add(
                Box.createVerticalStrut(18)
        );

        // -----------------------------------------------------
        // PARTE INFERIOR
        // -----------------------------------------------------

        JPanel inferior =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                17,
                                0
                        )
                );

        inferior.setOpaque(false);

        inferior.setPreferredSize(
                new Dimension(
                        1000,
                        300
                )
        );

        inferior.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        300
                )
        );

        inferior.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        inferior.add(
                inicioMembresias()
        );

        inferior.add(
                actividad()
        );

        page.add(inferior);

        mostrarContenido(page);
    }

    // =========================================================
    // MEMBRESÍAS EN INICIO
    // =========================================================

    private JPanel inicioMembresias() {

        JPanel p = card();

        p.setLayout(
                new BorderLayout()
        );

        p.setBorder(
                new EmptyBorder(
                        15,
                        18,
                        15,
                        18
                )
        );

        JPanel mh = new JPanel(new BorderLayout());

        mh.setOpaque(false);

        mh.add(
                sectionTitle(
                        "Membresías",
                        ""
                ),
                BorderLayout.WEST
        );

        JLabel verTodas = new JLabel("Ver todas");

        verTodas.setForeground(GOLD);

        verTodas.setFont(
                f("SansSerif", Font.BOLD, 11)
        );

        verTodas.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        verTodas.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        mostrarMembresias();
                    }
                }
        );

        mh.add(verTodas, BorderLayout.EAST);

        p.add(mh, BorderLayout.NORTH);

        int cantidad =
                Math.max(
                        1,
                        control.getMembresias().size()
                );

        int columnas = Math.min(cantidad, 4);

        JPanel grid =
                new JPanel(
                        new GridLayout(
                                0,
                                columnas,
                                9,
                                9
                        )
                );

        grid.setOpaque(false);

        grid.setBorder(
                new EmptyBorder(
                        12,
                        0,
                        0,
                        0
                )
        );

        for (
                Membresia m :
                control.getMembresias()
        ) {

            JPanel c = card();

            c.setBorder(
                    new EmptyBorder(
                            11,
                            11,
                            11,
                            11
                    )
            );

            c.setLayout(
                    new BoxLayout(
                            c,
                            BoxLayout.Y_AXIS
                    )
            );

            JLabel icono =
                    new JLabel(
                            new Icono(
                                    Icono.CALENDAR,
                                    GOLD,
                                    20
                            )
                    );

            icono.setAlignmentX(Component.LEFT_ALIGNMENT);

            c.add(icono);

            JLabel n =
                    new JLabel(
                            m.getNombre()
                    );

            n.setForeground(TEXT);

            n.setFont(
                    f(
                            "SansSerif",
                            Font.BOLD,
                            12
                    )
            );

            n.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel precio =
                    new JLabel(
                            String.format(
                                    "$%.0f",
                                    m.getPrecio()
                            )
                    );

            precio.setForeground(GOLD);

            precio.setFont(
                    f(
                            "SansSerif",
                            Font.BOLD,
                            18
                    )
            );

            precio.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel dias =
                    new JLabel(
                            m.getDuracionDias()
                            + " días"
                    );

            dias.setForeground(MUTED);

            dias.setFont(
                    f(
                            "SansSerif",
                            Font.PLAIN,
                            10
                    )
            );

            dias.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel descripcion =
                    new JLabel(
                            "<html>" + m.getDescripcion() + "</html>"
                    );

            descripcion.setForeground(MUTED);

            descripcion.setFont(
                    f("SansSerif", Font.PLAIN, 10)
            );

            descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);

            descripcion.setBorder(
                    new EmptyBorder(4, 0, 8, 0)
            );

            JButton verDetalles =
                    new JButton("Ver detalles");

            verDetalles.setAlignmentX(Component.LEFT_ALIGNMENT);

            verDetalles.setFont(
                    f("SansSerif", Font.BOLD, 10)
            );

            verDetalles.setForeground(GOLD);

            verDetalles.setBackground(PANEL);

            verDetalles.setFocusPainted(false);

            verDetalles.setBorder(
                    new LineBorder(GOLD)
            );

            verDetalles.setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
            );

            verDetalles.addActionListener(
                    e -> mensaje(
                            m.getNombre() + "\n\n"
                            + m.getDescripcion() + "\n\n"
                            + "Duración: " + m.getDuracionDias() + " días\n"
                            + "Precio: $" + String.format("%.2f", m.getPrecio())
                    )
            );

            c.add(
                    Box.createVerticalStrut(5)
            );

            c.add(n);

            c.add(
                    Box.createVerticalStrut(5)
            );

            c.add(precio);

            c.add(dias);

            c.add(descripcion);

            c.add(Box.createVerticalGlue());

            c.add(verDetalles);

            grid.add(c);
        }

        p.add(
                grid,
                BorderLayout.CENTER
        );

        return p;
    }

    // =========================================================
    // ACTIVIDAD
    // =========================================================

    private JPanel actividad() {

        JPanel p = card();

        p.setLayout(
                new BorderLayout()
        );

        p.setBorder(
                new EmptyBorder(
                        15,
                        18,
                        15,
                        18
                )
        );

        p.add(
                sectionTitle(
                        "Actividad Reciente",
                        ""
                ),
                BorderLayout.NORTH
        );

        JPanel lista =
                new JPanel();

        lista.setOpaque(false);

        lista.setLayout(
                new BoxLayout(
                        lista,
                        BoxLayout.Y_AXIS
                )
        );

        if (control.getActividades().isEmpty()) {

            addActivity(
                    lista,
                    Icono.USER,
                    BLUE,
                    "Sin actividad reciente",
                    "Aún no se ha registrado ninguna acción en esta sesión."
            );

        } else {

            int cantidad =
                    Math.min(
                            5,
                            control.getActividades().size()
                    );

            for (int i = 0; i < cantidad; i++) {

                GimnasioControl.Actividad act =
                        control.getActividades().get(i);

                int icono;
                Color color;

                switch (act.tipo) {

                    case "socio_nuevo":
                        icono = Icono.USER;
                        color = GREEN;
                        break;

                    case "socio_renovado":
                        icono = Icono.REFRESH;
                        color = GOLD;
                        break;

                    case "socio_editado":
                        icono = Icono.USER;
                        color = BLUE;
                        break;

                    case "socio_activado":
                        icono = Icono.CHECK_USER;
                        color = GREEN;
                        break;

                    case "socio_suspendido":
                        icono = Icono.BLOCK;
                        color = RED;
                        break;

                    case "socio_eliminado":
                        icono = Icono.USER;
                        color = RED;
                        break;

                    case "pago":
                        icono = Icono.CARD;
                        color = GREEN;
                        break;

                    case "entrenador_nuevo":
                    case "entrenador_editado":
                        icono = Icono.TRAINER;
                        color = PURPLE;
                        break;

                    case "entrenador_eliminado":
                        icono = Icono.TRAINER;
                        color = RED;
                        break;

                    case "membresia_nueva":
                    case "membresia_editada":
                        icono = Icono.CALENDAR;
                        color = GOLD;
                        break;

                    case "membresia_eliminada":
                        icono = Icono.CALENDAR;
                        color = RED;
                        break;

                    default:
                        icono = Icono.INFO;
                        color = BLUE;
                        break;
                }

                addActivity(
                        lista,
                        icono,
                        color,
                        act.titulo,
                        act.descripcion + " - " + act.fechaHora
                );
            }
        }

        p.add(
                lista,
                BorderLayout.CENTER
        );

        return p;
    }

    private void addActivity(
            JPanel lista,
            int icono,
            Color color,
            String titulo,
            String descripcion
    ) {

        JPanel fila =
                new JPanel(
                        new BorderLayout(
                                9,
                                0
                        )
                );

        fila.setOpaque(false);

        fila.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        44
                )
        );

        fila.add(
                new JLabel(
                        new Icono(
                                icono,
                                color,
                                25
                        )
                ),
                BorderLayout.WEST
        );

        JPanel texto =
                new JPanel();

        texto.setOpaque(false);

        texto.setLayout(
                new BoxLayout(
                        texto,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel a =
                new JLabel(
                        titulo
                );

        a.setForeground(TEXT);

        a.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        JLabel b =
                new JLabel(
                        descripcion
                );

        b.setForeground(MUTED);

        b.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        texto.add(a);
        texto.add(b);

        fila.add(
                texto,
                BorderLayout.CENTER
        );

        lista.add(fila);

        lista.add(
                Box.createVerticalStrut(7)
        );
    }

    // =========================================================
    // TARJETA ESTADÍSTICA
    // =========================================================

    private JPanel stat(
            String titulo,
            Object valor,
            String subtitulo,
            Color color,
            int icono
    ) {

        JPanel p = card();

        p.setLayout(
                new BorderLayout(0, 6)
        );

        p.setBorder(
                new EmptyBorder(
                        16,
                        16,
                        14,
                        16
                )
        );

        JPanel top =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        top.setOpaque(false);

        top.add(
                new JLabel(
                        new Icono(
                                icono,
                                color,
                                24
                        )
                )
        );

        JLabel t =
                new JLabel(
                        titulo
                );

        t.setForeground(TEXT);

        t.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        top.add(t);

        p.add(
                top,
                BorderLayout.NORTH
        );

        JLabel v =
                new JLabel(
                        String.valueOf(valor)
                );

        v.setForeground(TEXT);

        v.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        25
                )
        );

        p.add(
                v,
                BorderLayout.CENTER
        );

        boolean positivo =
                subtitulo != null
                && subtitulo.startsWith("+");

        boolean negativo =
                subtitulo != null
                && subtitulo.startsWith("-");

        Color deltaColor =
                positivo
                        ? GREEN
                        : negativo
                                ? RED
                                : MUTED;

        JPanel deltaRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                4,
                                0
                        )
                );

        deltaRow.setOpaque(false);

        JLabel arrow =
                new JLabel(
                        positivo
                                ? "\u2191"
                                : negativo
                                        ? "\u2193"
                                        : "\u25CF"
                );

        arrow.setForeground(deltaColor);

        arrow.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        positivo || negativo ? 11 : 7
                )
        );

        deltaRow.add(arrow);

        JLabel sub =
                new JLabel(
                        subtitulo
                );

        sub.setForeground(deltaColor);

        sub.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        10
                )
        );

        deltaRow.add(sub);

        p.add(
                deltaRow,
                BorderLayout.SOUTH
        );

        return p;
    }

    // =========================================================
    // SOCIOS
    // =========================================================

    private void mostrarSocios() {

        seccionVisible = "socios";

        activarNav(1);

        pageTitle.setText(
                "Gestión de Socios"
        );

        JPanel page = base();

        JPanel head =
                moduleHead(
                        "Socios",
                        "Administra la información de los miembros del gimnasio.",
                        "Nuevo socio",
                        Icono.PLUS,
                        Icono.USER
                );

        JButton boton =
                (JButton) head.getClientProperty(
                        "actionButton"
                );

        boton.addActionListener(
                e -> dialogoSocio(null)
        );

        page.add(head);

        page.add(
                Box.createVerticalStrut(16)
        );

        // -----------------------------------------------------
        // ESTADÍSTICAS
        // -----------------------------------------------------

        JPanel stats =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                12,
                                0
                        )
                );

        stats.setOpaque(false);

        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        stats.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        130
                )
        );

        JPanel statTotal =
                stat(
                        "Total de Socios",
                        control.totalSocios(),
                        deltaMes(altasSociosEsteMes()),
                        GOLD,
                        Icono.USER_GROUP
                );

        JPanel statActivos =
                stat(
                        "Socios Activos",
                        contarActivos(),
                        contarActivos() + " de " + control.totalSocios(),
                        GREEN,
                        Icono.CHECK_USER
                );

        JPanel statInactivos =
                stat(
                        "Socios Inactivos",
                        control.totalSocios()
                                - contarActivos(),
                        "Sin cambios",
                        RED,
                        Icono.BLOCK
                );

        stats.add(statTotal);
        stats.add(statActivos);
        stats.add(statInactivos);

        JPanel statVencidas =
                stat(
                        "Vencidas",
                        control.sociosVencidos(),
                        "Ya no están vigentes",
                        RED,
                        Icono.BLOCK
                );

        JPanel statVencenPronto =
                stat(
                        "Vencen Pronto",
                        control.listaSociosPorVencer(7).size(),
                        "En los próximos 7 días",
                        BLUE,
                        Icono.CALENDAR
                );

        stats.add(statVencidas);
        stats.add(statVencenPronto);

        page.add(stats);

        page.add(
                Box.createVerticalStrut(14)
        );

        // -----------------------------------------------------
        // BUSCADOR Y FILTROS
        // -----------------------------------------------------

        JPanel searchBar =
                new JPanel(
                        new BorderLayout(
                                10,
                                0
                        )
                );

        searchBar.setOpaque(false);

        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchBar.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        38
                )
        );

        JTextField search =
                field();

        search.setToolTipText(
                "Nombre, DNI, teléfono o membresía"
        );

        searchBar.add(
                search,
                BorderLayout.CENTER
        );

        JPanel filtros =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );

        filtros.setOpaque(false);

        JComboBox<String> estadoFiltro =
                combo(new String[]{"Todos los estados", "Activo", "Suspendido"});

        filtros.add(estadoFiltro);

        java.util.List<String> nombresPlanes = new java.util.ArrayList<>();

        nombresPlanes.add("Todos los planes");

        for (Membresia m : control.getMembresias()) {

            nombresPlanes.add(m.getNombre());
        }

        JComboBox<String> planFiltro =
                combo(nombresPlanes.toArray(new String[0]));

        filtros.add(planFiltro);

        JComboBox<String> vencFiltro =
                combo(new String[]{
                    "Todas",
                    "Vigentes",
                    "Por vencer",
                    "Vencidas"
                });

        filtros.add(vencFiltro);

        JButton filtrarBtn = gold("Filtrar", Icono.CHECK);

        filtros.add(filtrarBtn);

        searchBar.add(
                filtros,
                BorderLayout.EAST
        );

        page.add(searchBar);

        page.add(
                Box.createVerticalStrut(6)
        );

        JLabel avisoRenovar = new JLabel(
                "<html>Los socios vencidos o por vencer muestran un botón "
                + "extra de <b>renovar</b> (ícono dorado) junto a editar; "
                + "ahí puedes dejar sus datos igual o cambiarlos y "
                + "renovar su membresía.</html>"
        );

        avisoRenovar.setForeground(MUTED);

        avisoRenovar.setFont(
                f("SansSerif", Font.ITALIC, 10)
        );

        avisoRenovar.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(avisoRenovar);

        page.add(
                Box.createVerticalStrut(10)
        );

        // -----------------------------------------------------
        // TABLA
        // -----------------------------------------------------

        JPanel tableCard = card();

        tableCard.setLayout(
                new BorderLayout()
        );

        tableCard.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        0,
                        0
                )
        );

        String[] columnas = {
            "ID",
            "NOMBRE",
            "TELÉFONO",
            "MEMBRESÍA",
            "ESTADO",
            "FECHA DE ALTA",
            "FECHA DE VENCIMIENTO",
            "ACCIONES"
        };

        DefaultTableModel tm =
                modelo(columnas);

        cargarSocios(
                tm,
                ""
        );

        JTable table =
                crearTablaSocios(tm);

        JPanel piePaginacion = new JPanel(new BorderLayout());

        piePaginacion.setOpaque(false);

        Runnable[] recargarTabla = new Runnable[1];

        recargarTabla[0] = () -> {

            cargarSocios(
                    tm,
                    search.getText()
                            .trim()
                            .toLowerCase(),
                    (String) estadoFiltro.getSelectedItem(),
                    (String) planFiltro.getSelectedItem(),
                    (String) vencFiltro.getSelectedItem()
            );

            piePaginacion.removeAll();

            piePaginacion.add(
                    paginacionSocios(pagina -> {

                        paginaSocios = pagina;

                        recargarTabla[0].run();
                    }),
                    BorderLayout.CENTER
            );

            piePaginacion.revalidate();
            piePaginacion.repaint();
        };

        // Cualquier cambio de búsqueda o filtro vuelve a la página 1.
        Runnable filtrar = () -> {

            paginaSocios = 1;

            recargarTabla[0].run();
        };

        filtrarBtn.addActionListener(e -> filtrar.run());

        estadoFiltro.addActionListener(e -> filtrar.run());

        planFiltro.addActionListener(e -> filtrar.run());

        vencFiltro.addActionListener(e -> filtrar.run());

        // Las tarjetas de "Vencidas" y "Vencen Pronto" son atajos:
        // con un clic filtran la tabla de abajo sin tener que ir a
        // buscar el combo manualmente.

        statVencidas.setCursor(new Cursor(Cursor.HAND_CURSOR));

        statVencidas.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                vencFiltro.setSelectedItem("Vencidas");

                filtrar.run();
            }
        });

        statVencenPronto.setCursor(new Cursor(Cursor.HAND_CURSOR));

        statVencenPronto.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                vencFiltro.setSelectedItem("Por vencer");

                filtrar.run();
            }
        });

        // Total / Activos / Inactivos también son atajos: filtran
        // por estado sin tocar el filtro de vencimiento.

        statTotal.setCursor(new Cursor(Cursor.HAND_CURSOR));

        statTotal.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                estadoFiltro.setSelectedItem("Todos los estados");
                vencFiltro.setSelectedItem("Todas");

                filtrar.run();
            }
        });

        statActivos.setCursor(new Cursor(Cursor.HAND_CURSOR));

        statActivos.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                estadoFiltro.setSelectedItem("Activo");

                filtrar.run();
            }
        });

        statInactivos.setCursor(new Cursor(Cursor.HAND_CURSOR));

        statInactivos.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                estadoFiltro.setSelectedItem("Suspendido");

                filtrar.run();
            }
        });

        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {

                            @Override
                            public void insertUpdate(
                                    DocumentEvent e
                            ) {
                                filtrar.run();
                            }

                            @Override
                            public void removeUpdate(
                                    DocumentEvent e
                            ) {
                                filtrar.run();
                            }

                            @Override
                            public void changedUpdate(
                                    DocumentEvent e
                            ) {
                                filtrar.run();
                            }
                        }
                );

        tableCard.add(
                tablaSinScroll(table),
                BorderLayout.CENTER
        );

        tableCard.add(
                piePaginacion,
                BorderLayout.SOUTH
        );

        recargarTabla[0].run();

        page.add(tableCard);

        mostrarContenido(page);
    }

    // =========================================================
    // PIE DE PAGINACIÓN DE SOCIOS (10 por página)
    // =========================================================

    private JPanel paginacionSocios(java.util.function.IntConsumer irAPagina) {

        JPanel foot = new JPanel(new BorderLayout());

        foot.setOpaque(true);

        foot.setBackground(PANEL);

        foot.setBorder(
                new javax.swing.border.CompoundBorder(
                        new javax.swing.border.MatteBorder(1, 0, 0, 0, BORDER),
                        new EmptyBorder(12, 18, 12, 18)
                )
        );

        int total = totalSociosFiltrados;

        int totalPaginas = Math.max(
                1,
                (total + SOCIOS_POR_PAGINA - 1) / SOCIOS_POR_PAGINA
        );

        int desde = total == 0
                ? 0
                : (paginaSocios - 1) * SOCIOS_POR_PAGINA + 1;

        int hasta = Math.min(paginaSocios * SOCIOS_POR_PAGINA, total);

        JLabel texto = new JLabel(
                total == 0
                        ? "Sin socios para mostrar"
                        : "Mostrando " + desde + " - " + hasta
                                + " de " + total + " socios"
        );

        texto.setForeground(MUTED);

        texto.setFont(f("SansSerif", Font.PLAIN, 11));

        foot.add(texto, BorderLayout.WEST);

        JPanel paginas =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        paginas.setOpaque(false);

        JButton prev = dark("<");
        prev.setPreferredSize(new Dimension(30, 28));
        prev.setEnabled(paginaSocios > 1);
        prev.addActionListener(e -> irAPagina.accept(paginaSocios - 1));

        paginas.add(prev);

        // Con muchas páginas se muestran la primera, la última y las
        // cercanas a la actual, con "…" en los huecos.
        int ultimoMostrado = 0;

        for (int n = 1; n <= totalPaginas; n++) {

            boolean mostrar =
                    n == 1
                    || n == totalPaginas
                    || Math.abs(n - paginaSocios) <= 2;

            if (!mostrar) {
                continue;
            }

            if (ultimoMostrado != 0 && n - ultimoMostrado > 1) {

                JLabel puntos = new JLabel("…");

                puntos.setForeground(MUTED);

                paginas.add(puntos);
            }

            ultimoMostrado = n;

            final int destino = n;

            JButton b;

            if (n == paginaSocios) {

                b = gold(String.valueOf(n), -1);

                b.setIcon(null);

            } else {

                b = dark(String.valueOf(n));

                b.addActionListener(e -> irAPagina.accept(destino));
            }

            b.setPreferredSize(new Dimension(34, 28));
            b.setBorder(new EmptyBorder(0, 0, 0, 0));

            paginas.add(b);
        }

        JButton next = dark(">");
        next.setPreferredSize(new Dimension(30, 28));
        next.setEnabled(paginaSocios < totalPaginas);
        next.addActionListener(e -> irAPagina.accept(paginaSocios + 1));

        paginas.add(next);

        foot.add(paginas, BorderLayout.EAST);

        return foot;
    }

    // =========================================================
    // PIE DE PAGINACIÓN
    // =========================================================

    private JPanel paginacionFooter(String resumen) {

        JPanel foot = new JPanel(new BorderLayout());

        foot.setOpaque(true);

        foot.setBackground(PANEL);

        foot.setBorder(
                new javax.swing.border.CompoundBorder(
                        new javax.swing.border.MatteBorder(1, 0, 0, 0, BORDER),
                        new EmptyBorder(12, 18, 12, 18)
                )
        );

        JLabel texto = new JLabel(resumen);

        texto.setForeground(MUTED);

        texto.setFont(f("SansSerif", Font.PLAIN, 11));

        foot.add(texto, BorderLayout.WEST);

        JPanel paginas =
                new JPanel(
                        new FlowLayout(FlowLayout.RIGHT, 6, 0)
                );

        paginas.setOpaque(false);

        JButton prev = dark("<");
        prev.setPreferredSize(new Dimension(30, 28));

        paginas.add(prev);

        JButton pagina1 = gold("1", -1);
        pagina1.setIcon(null);
        pagina1.setPreferredSize(new Dimension(30, 28));
        pagina1.setBorder(new EmptyBorder(0, 0, 0, 0));

        paginas.add(pagina1);

        JButton next = dark(">");
        next.setPreferredSize(new Dimension(30, 28));

        paginas.add(next);

        foot.add(paginas, BorderLayout.EAST);

        return foot;
    }

    // =========================================================
    // DATOS REALES PARA LAS TARJETAS ("este mes")
    // =========================================================

    private String mesActual() {

        return new SimpleDateFormat("MM/yyyy").format(new Date());
    }

    /** Devuelve "MM/yyyy" de una fecha con formato dd/MM/yyyy. */
    private String mesDe(String fecha) {

        if (fecha == null || fecha.length() < 10) {
            return "";
        }

        return fecha.substring(3);
    }

    private int altasSociosEsteMes() {

        int total = 0;

        for (Socio s : control.getSocios()) {

            if (mesActual().equals(mesDe(s.getFechaAlta()))) {
                total++;
            }
        }

        return total;
    }

    private int pagosEsteMes() {

        int total = 0;

        for (Pago p : control.getPagos()) {

            if (mesActual().equals(mesDe(p.getFecha()))) {
                total++;
            }
        }

        return total;
    }

    private double ingresosEsteMes() {

        double total = 0;

        for (Pago p : control.getPagos()) {

            if (mesActual().equals(mesDe(p.getFecha()))) {
                total += p.getMonto();
            }
        }

        return total;
    }

    /** Texto tipo "+3 este mes" o "Sin cambios" con datos reales. */
    private String deltaMes(int cantidad) {

        return cantidad > 0
                ? "+" + cantidad + " este mes"
                : "Sin cambios";
    }

    // =========================================================
    // CONTAR ACTIVOS
    // =========================================================

    private int contarActivos() {

        int total = 0;

        for (
                Socio s :
                control.getSocios()
        ) {

            if (s.isActivo()) {
                total++;
            }
        }

        return total;
    }

    // =========================================================
    // AGREGACIONES PARA REPORTES
    // =========================================================

    private java.util.LinkedHashMap<String, Integer> sociosPorMembresia() {

        java.util.LinkedHashMap<String, Integer> mapa =
                new java.util.LinkedHashMap<>();

        for (Membresia m : control.getMembresias()) {

            mapa.put(m.getNombre(), 0);
        }

        for (Socio s : control.getSocios()) {

            mapa.merge(s.getMembresia(), 1, Integer::sum);
        }

        return mapa;
    }

    private java.util.LinkedHashMap<String, Double> ingresosPorMembresia() {

        java.util.LinkedHashMap<String, Double> mapa =
                new java.util.LinkedHashMap<>();

        for (Membresia m : control.getMembresias()) {

            mapa.put(m.getNombre(), 0.0);
        }

        for (Pago p : control.getPagos()) {

            mapa.merge(p.getMembresia(), p.getMonto(), Double::sum);
        }

        return mapa;
    }

    private java.util.LinkedHashMap<String, Double> pagosPorMetodo() {

        java.util.LinkedHashMap<String, Double> mapa =
                new java.util.LinkedHashMap<>();

        mapa.put("Efectivo", 0.0);
        mapa.put("Tarjeta", 0.0);
        mapa.put("Transferencia", 0.0);

        for (Pago p : control.getPagos()) {

            mapa.merge(
                    p.getMetodoPago(),
                    p.getMonto(),
                    Double::sum
            );
        }

        return mapa;
    }

    private Color colorParaMembresia(String nombre, int indice) {

        Color[] paleta = {GOLD, BLUE, PURPLE, GREEN, new Color(230, 126, 34), RED, MUTED};

        return paleta[indice % paleta.length];
    }

    // =========================================================
    // CARGAR SOCIOS
    // =========================================================

    private void cargarSocios(
            DefaultTableModel tm,
            String texto
    ) {

        cargarSocios(tm, texto, "Todos los estados", "Todos los planes", "Todas");
    }

    private void cargarSocios(
            DefaultTableModel tm,
            String texto,
            String estadoFiltro,
            String planFiltro
    ) {

        cargarSocios(tm, texto, estadoFiltro, planFiltro, "Todas");
    }

    private void cargarSocios(
            DefaultTableModel tm,
            String texto,
            String estadoFiltro,
            String planFiltro,
            String vencFiltro
    ) {

        tm.setRowCount(0);

        java.util.List<Socio> coincidencias = new java.util.ArrayList<>();

        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("dd/MM/yyyy");

        java.util.Date hoy = new java.util.Date();

        java.util.Calendar limite7 = java.util.Calendar.getInstance();
        limite7.add(java.util.Calendar.DAY_OF_MONTH, 7);

        for (
                Socio s :
                control.getSocios()
        ) {

            String datos =
                    (
                            s.getNombre()
                            + " "
                            + s.getDni()
                            + " "
                            + s.getTelefono()
                            + " "
                            + s.getMembresia()
                    )
                    .toLowerCase();

            boolean coincideTexto =
                    texto.isEmpty()
                    || datos.contains(texto);

            boolean coincideEstado =
                    estadoFiltro == null
                    || estadoFiltro.equals("Todos los estados")
                    || (estadoFiltro.equals("Activo") && s.isActivo())
                    || (estadoFiltro.equals("Suspendido") && !s.isActivo());

            boolean coincidePlan =
                    planFiltro == null
                    || planFiltro.equals("Todos los planes")
                    || planFiltro.equals(s.getMembresia());

            boolean coincideVenc = true;

            if (
                    vencFiltro != null
                    && !vencFiltro.equals("Todas")
            ) {

                try {

                    java.util.Date fv = fmt.parse(s.getFechaVencimiento());

                    if (vencFiltro.equals("Vencidas")) {

                        coincideVenc = fv.before(hoy);

                    } else if (vencFiltro.equals("Por vencer")) {

                        coincideVenc =
                                !fv.before(hoy)
                                && fv.before(limite7.getTime());

                    } else if (vencFiltro.equals("Vigentes")) {

                        coincideVenc = !fv.before(hoy);
                    }

                } catch (Exception ex) {

                    coincideVenc = false;
                }
            }

            if (
                    coincideTexto
                    && coincideEstado
                    && coincidePlan
                    && coincideVenc
            ) {

                coincidencias.add(s);
            }
        }

        // Paginación: solo se cargan en la tabla los socios de la
        // página actual (SOCIOS_POR_PAGINA por página).

        totalSociosFiltrados = coincidencias.size();

        int totalPaginas = Math.max(
                1,
                (totalSociosFiltrados + SOCIOS_POR_PAGINA - 1)
                / SOCIOS_POR_PAGINA
        );

        if (paginaSocios > totalPaginas) {
            paginaSocios = totalPaginas;
        }

        if (paginaSocios < 1) {
            paginaSocios = 1;
        }

        int desde = (paginaSocios - 1) * SOCIOS_POR_PAGINA;

        int hasta = Math.min(
                desde + SOCIOS_POR_PAGINA,
                totalSociosFiltrados
        );

        for (int i = desde; i < hasta; i++) {

            Socio s = coincidencias.get(i);

            tm.addRow(
                    new Object[]{
                        s.getId(),
                        s.getNombre(),
                        s.getTelefono(),
                        s.getMembresia(),
                        s.isActivo()
                                ? "Activo"
                                : "Suspendido",
                        s.getFechaAlta(),
                        s.getFechaVencimiento(),
                        "acciones"
                    }
            );
        }
    }

    // =========================================================
    // TABLA SOCIOS
    // =========================================================

    private JTable crearTablaSocios(
            DefaultTableModel tm
    ) {

        JTable t =
                tabla(tm);

        t.setRowHeight(52);

        int[] anchos = {
            50,
            190,
            110,
            120,
            100,
            120,
            140,
            165
        };

        for (int i = 0;
                i < anchos.length;
                i++) {

            t.getColumnModel()
                .getColumn(i)
                        .setPreferredWidth(
                                anchos[i]
                        );
        }

        t.getColumnModel()
                .getColumn(1)
                .setCellRenderer(
                        new NombreAvatarRenderer(
                                id -> {
                                    Socio s = control.buscarSocio(id);
                                    return s == null ? "" : s.getFotoPath();
                                }
                        )
                );

        for (int col :
                new int[]{0, 2, 3, 5, 6}) {

            DefaultTableCellRenderer center =
                    new DefaultTableCellRenderer();

            center.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            center.setForeground(TEXT);
            center.setBackground(PANEL);

            t.getColumnModel()
                    .getColumn(col)
                    .setCellRenderer(center);
        }

        t.getColumnModel()
                .getColumn(4)
                .setCellRenderer(
                        new StatusRenderer()
                );

        t.getColumnModel()
                .getColumn(7)
                .setCellRenderer(
                        new ActionRenderer()
                );

        t.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            MouseEvent e
                    ) {

                        int fila =
                                t.rowAtPoint(
                                        e.getPoint()
                                );

                        int columna =
                                t.columnAtPoint(
                                        e.getPoint()
                                );

                        if (
                                fila < 0
                                || columna != 7
                        ) {
                            return;
                        }

                        int id =
                                Integer.parseInt(
                                        t.getValueAt(
                                                fila,
                                                0
                                        ).toString()
                                );

                        Socio socio =
                                control.buscarSocio(id);

                        if (socio == null) {
                            return;
                        }

                        Rectangle r =
                                t.getCellRect(
                                        fila,
                                        columna,
                                        true
                                );

                        boolean necesitaRenovar =
                                socioNecesitaRenovar(socio);

                        int cantidadBotones =
                                necesitaRenovar ? 5 : 4;

                        int zona =
                                zonaAccion(
                                        e.getX() - r.x,
                                        r.width,
                                        cantidadBotones
                                );

                        if (necesitaRenovar) {

                            if (zona == 0) {

                                dialogoSocio(socio, true);

                                return;
                            }

                            zona = zona - 1;
                        }

                        if (zona == 0) {

                            dialogoSocio(socio);

                        } else if (zona == 1) {

                            perfilSocio(socio);

                        } else if (zona == 2) {

                            cambiarEstadoSocio(socio);

                        } else {

                            eliminarSocio(socio);
                        }
                    }

                }
        );

        return t;
    }

    // =========================================================
    // PERFIL SOCIO
    // =========================================================

    private void perfilSocio(
            Socio s
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                20,
                                0
                        )
                );

        panel.setBackground(PANEL);

        panel.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        JPanel izquierda =
                new JPanel();

        izquierda.setOpaque(false);

        izquierda.setLayout(
                new BoxLayout(
                        izquierda,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel avatar =
                new JLabel(
                        fotoCircular(
                                s.getFotoPath(),
                                80
                        )
                );

        avatar.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel nombre =
                new JLabel(
                        s.getNombre()
                );

        nombre.setForeground(TEXT);

        nombre.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        nombre.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel estado =
                new JLabel(
                        s.isActivo()
                                ? "ACTIVO"
                                : "SUSPENDIDO"
                );

        estado.setForeground(
                s.isActivo()
                        ? GREEN
                        : RED
        );

        estado.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        estado.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        izquierda.add(avatar);

        izquierda.add(
                Box.createVerticalStrut(7)
        );

        izquierda.add(nombre);

        izquierda.add(
                Box.createVerticalStrut(5)
        );

        izquierda.add(estado);

        JPanel datos =
                new JPanel();

        datos.setOpaque(false);

        datos.setLayout(
                new BoxLayout(
                        datos,
                        BoxLayout.Y_AXIS
                )
        );

        dato(
                datos,
                "ID",
                String.valueOf(
                        s.getId()
                )
        );

        dato(
                datos,
                "DNI",
                s.getDni()
        );

        dato(
                datos,
                "Correo",
                s.getCorreo()
        );

        dato(
                datos,
                "Teléfono",
                s.getTelefono()
        );

        dato(
                datos,
                "Nacimiento",
                s.getFechaNacimiento()
        );

        dato(
                datos,
                "Membresía",
                s.getMembresia()
        );

        dato(
                datos,
                "Peso",
                s.getPeso() + " kg"
        );

        dato(
                datos,
                "Altura",
                s.getAltura() + " m"
        );

        panel.add(
                izquierda,
                BorderLayout.WEST
        );

        panel.add(
                datos,
                BorderLayout.CENTER
        );

        if (cfgMostrarHistorialPagos) {

            panel.add(
                    panelHistorialPagos(s),
                    BorderLayout.EAST
            );
        }

        JOptionPane.showMessageDialog(
                this,
                panel,
                "Perfil del socio",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    /**
     * Lista de pagos de un socio en su perfil. Se muestra solo si
     * "Mostrar historial de pagos" está activo en Configuración.
     */
    private JPanel panelHistorialPagos(Socio s) {

        JPanel wrap = new JPanel();

        wrap.setOpaque(false);

        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        wrap.setBorder(new EmptyBorder(0, 20, 0, 0));

        JLabel titulo = new JLabel("Historial de pagos");

        titulo.setForeground(TEXT);

        titulo.setFont(f("SansSerif", Font.BOLD, 13));

        wrap.add(titulo);

        wrap.add(Box.createVerticalStrut(8));

        java.util.List<Pago> propios = new java.util.ArrayList<>();

        for (Pago p : control.getPagos()) {

            if (p.getSocioId() == s.getId()) {
                propios.add(p);
            }
        }

        if (propios.isEmpty()) {

            JLabel vacio = new JLabel("Sin pagos registrados.");

            vacio.setForeground(MUTED);

            vacio.setFont(f("SansSerif", Font.PLAIN, 12));

            wrap.add(vacio);

        } else {

            for (Pago p : propios) {

                JLabel linea = new JLabel(
                        "<html>" + p.getFecha() + " — $"
                        + String.format("%.2f", p.getMonto())
                        + " (" + p.getMetodoPago() + ")</html>"
                );

                linea.setForeground(MUTED);

                linea.setFont(f("SansSerif", Font.PLAIN, 12));

                wrap.add(linea);
                wrap.add(Box.createVerticalStrut(4));
            }
        }

        return wrap;
    }

    private void dato(
            JPanel panel,
            String etiqueta,
            String valor
    ) {

        JPanel fila =
                new JPanel(
                        new BorderLayout(
                                20,
                                0
                        )
                );

        fila.setOpaque(false);

        fila.setMaximumSize(
                new Dimension(
                        420,
                        34
                )
        );

        JLabel a =
                new JLabel(
                        etiqueta
                );

        a.setForeground(MUTED);

        a.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        JLabel b =
                new JLabel(
                        valor
                );

        b.setForeground(TEXT);

        b.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        fila.add(
                a,
                BorderLayout.WEST
        );

        fila.add(
                b,
                BorderLayout.CENTER
        );

        panel.add(fila);
    }

    // =========================================================
    // SUSPENDER / ACTIVAR
    // =========================================================

    private void cambiarEstadoSocio(
            Socio socio
    ) {

        String pregunta;

        if (socio.isActivo()) {

            pregunta =
                    "¿Deseas suspender a "
                    + socio.getNombre()
                    + "?";

        } else {

            pregunta =
                    "¿Deseas activar nuevamente a "
                    + socio.getNombre()
                    + "?";
        }

        if (confirm(pregunta)) {

            boolean quedaraActivo = !socio.isActivo();

            socio.setActivo(
                    quedaraActivo
            );

            control.registrarActividad(
                    quedaraActivo ? "socio_activado" : "socio_suspendido",
                    quedaraActivo ? "Socio activado" : "Socio suspendido",
                    socio.getNombre()
            );

            mostrarSocios();
        }
    }

    // =========================================================
    // ELIMINAR SOCIO
    // =========================================================

    private void eliminarSocio(
            Socio socio
    ) {

        if (
                confirm(
                        "¿Eliminar al socio "
                        + socio.getNombre()
                        + "?"
                )
        ) {

            control.eliminarSocio(
                    socio.getId()
            );

            control.registrarActividad(
                    "socio_eliminado",
                    "Socio eliminado",
                    socio.getNombre()
            );

            mostrarSocios();
        }
    }

    // =========================================================
    // DIALOGO SOCIO
    // =========================================================

    private void dialogoSocio(
            Socio socio
    ) {
        dialogoSocio(socio, false);
    }

    private void dialogoSocio(
            Socio socio,
            boolean renovacion
    ) {

        boolean editar =
                socio != null;

        JDialog dialogo =
                new JDialog(
                        this,
                        renovacion
                                ? "Renovar membresía"
                                : (editar
                                        ? "Editar socio"
                                        : "Nuevo socio"),
                        true
                );

        dialogo.setSize(
                480,
                660
        );

        dialogo.setLocationRelativeTo(
                this
        );

        JPanel panel =
                formPanel();

        // Columna angosta y centrada: antes los campos usaban todo
        // el ancho del diálogo (se veían "estirados"). Con "col"
        // limitado a 340px y centrado dentro de "panel", el
        // formulario (título, foto, campos y botón) queda de un
        // tamaño más chico y parejo, y se ve completo sin tener
        // que maximizar la ventana.

        JPanel col = new JPanel();

        col.setOpaque(false);

        col.setLayout(
                new BoxLayout(col, BoxLayout.Y_AXIS)
        );

        col.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.setMaximumSize(
                new Dimension(340, Integer.MAX_VALUE)
        );

        JLabel tituloDialogo = new JLabel(
                "<html><div style='text-align:center;'>"
                + "<font size='6'>"
                + (renovacion
                        ? "Renovar membresía"
                        : (editar ? "Actualizar socio" : "Registrar socio"))
                + "</font>"
                + "<br>"
                + "<font color='#A4B1BF'>DATOS DEL CLIENTE</font>"
                + "</div></html>"
        );

        tituloDialogo.setForeground(TEXT);

        tituloDialogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        tituloDialogo.setBorder(
                new EmptyBorder(0, 0, 15, 0)
        );

        col.add(tituloDialogo);

        String[] rutaFotoSocio = {
            editar ? socio.getFotoPath() : ""
        };

        JPanel fotoPanel = selectorFoto(rutaFotoSocio);

        fotoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.add(fotoPanel);

        col.add(Box.createVerticalStrut(10));

        JTextField nombre =
                field();

        JTextField dni =
                field();

        soloDigitos(dni, 8);

        JTextField correo =
                field();

        JTextField telefono =
                field();

        soloDigitos(telefono, 10);

        JTextField fecha =
                field();

        JTextField peso =
                field();

        JTextField altura =
                field();

        JComboBox<String> membresia =
                new JComboBox<>();

        membresia.addItem("Sin membresía");

        for (
                Membresia m :
                control.getMembresias()
        ) {

            membresia.addItem(
                    m.getNombre()
            );
        }

        if (!editar) {

            // Por defecto, que caiga en una membresía real; "Sin
            // membresía" solo se deja elegir si está permitido en
            // Configuración > Otras opciones.
            membresia.setSelectedIndex(
                    membresia.getItemCount() > 1 ? 1 : 0
            );
        }

        if (editar) {

            nombre.setText(
                    socio.getNombre()
            );

            dni.setText(
                    socio.getDni()
            );

            correo.setText(
                    socio.getCorreo()
            );

            telefono.setText(
                    socio.getTelefono()
            );

            fecha.setText(
                    socio.getFechaNacimiento()
            );

            peso.setText(
                    String.valueOf(
                            socio.getPeso()
                    )
            );

            altura.setText(
                    String.valueOf(
                            socio.getAltura()
                    )
            );

            membresia.setSelectedItem(
                    socio.getMembresia()
            );
        }

        addField(
                col,
                "Nombre",
                nombre
        );

        addField(
                col,
                "DNI",
                dni
        );

        addField(
                col,
                "Correo",
                correo
        );

        addField(
                col,
                "Teléfono",
                telefono
        );

        JPanel fechaPanel =
                new JPanel(
                        new BorderLayout(
                                5,
                                0
                        )
                );

        fechaPanel.setOpaque(false);

        fechaPanel.add(
                fecha,
                BorderLayout.CENTER
        );

        JButton calendario =
                new JButton(
                        new Icono(
                                Icono.CALENDAR,
                                GOLD,
                                17
                        )
                );

        calendario.setPreferredSize(
                new Dimension(
                        40,
                        30
                )
        );

        calendario.setFocusPainted(false);

        calendario.setBackground(PANEL2);

        calendario.setBorder(
                new LineBorder(
                        BORDER
                )
        );

        calendario.addActionListener(
                e -> mostrarJCalendarPopup(fecha)
        );

        fechaPanel.add(
                calendario,
                BorderLayout.EAST
        );

        addField(
                col,
                "Fecha de nacimiento",
                fechaPanel
        );

        addField(
                col,
                "Membresía",
                membresia
        );

        JLabel previewMembresia = new JLabel(" ");

        previewMembresia.setForeground(GOLD);

        previewMembresia.setFont(
                f("SansSerif", Font.BOLD, 11)
        );

        previewMembresia.setAlignmentX(Component.CENTER_ALIGNMENT);

        previewMembresia.setBorder(
                new EmptyBorder(4, 2, 10, 0)
        );

        Runnable actualizarPreview = () -> {

            String sel = (String) membresia.getSelectedItem();

            for (Membresia m : control.getMembresias()) {

                if (m.getNombre().equals(sel)) {

                    // Texto plano (antes era HTML): el HTML calculaba
                    // mal su alto y la segunda línea ("Precio: $...")
                    // se veía cortada.
                    previewMembresia.setText(
                            "Duración: " + m.getDuracionDias()
                            + " días — Precio: $"
                            + String.format("%.2f", m.getPrecio())
                    );

                    break;
                }
            }
        };

        membresia.addItemListener(e -> actualizarPreview.run());

        actualizarPreview.run();

        col.add(previewMembresia);

        addField(
                col,
                "Peso",
                peso
        );

        addField(
                col,
                "Altura",
                altura
        );

        // -----------------------------------------------------
        // AVISO DE RENOVACIÓN
        // -----------------------------------------------------
        // El método de pago y el envío del comprobante ya NO se
        // eligen aquí: se registra un pago con método "Efectivo"
        // por ahora, y se ajusta el método real (y se envía el
        // comprobante si se quiere) desde Pagos > Editar pago.

        if (renovacion) {

            col.add(Box.createVerticalStrut(6));
            col.add(new LineSep());
            col.add(Box.createVerticalStrut(10));

            JLabel avisoPago = new JLabel(
                    "<html><body style='width:280px'>"
                    + "Al guardar se renovará la membresía "
                    + "seleccionada desde hoy y se registrará un pago. "
                    + "Para elegir el método de pago real y enviar el "
                    + "comprobante al socio, ve a <b>Pagos</b> y edita "
                    + "ese pago.</html>"
            );

            avisoPago.setForeground(MUTED);

            avisoPago.setFont(
                    f("SansSerif", Font.ITALIC, 10)
            );

            avisoPago.setAlignmentX(Component.CENTER_ALIGNMENT);

            avisoPago.setBorder(
                    new EmptyBorder(2, 2, 10, 0)
            );

            col.add(avisoPago);
        }

        JButton guardar =
                gold(
                        renovacion ? "RENOVAR" : "GUARDAR",
                        Icono.CHECK
                );

        final Socio[] socioGuardado = { socio };

        guardar.addActionListener(
                e -> {

                    String n =
                            nombre.getText()
                                    .trim();

                    String d =
                            dni.getText()
                                    .trim();

                    String c =
                            correo.getText()
                                    .trim();

                    String t =
                            telefono.getText()
                                    .trim();

                    String f =
                            fecha.getText()
                                    .trim();

                    String p =
                            peso.getText()
                                    .trim();

                    String a =
                            altura.getText()
                                    .trim();

                    // -----------------------------------------
                    // REGEX NOMBRE
                    // -----------------------------------------

                    if (!RegexControl.nombre(n)) {

                        mensajeEn(
                                dialogo,
                                "Nombre inválido. Usa solo letras y espacios."
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX DNI
                    // -----------------------------------------

                    if (!RegexControl.dni(d)) {

                        mensajeEn(
                                dialogo,
                                "DNI inválido. Debe contener exactamente 8 números."
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX CORREO
                    // -----------------------------------------

                    if (!RegexControl.correo(c)) {

                        mensajeEn(
                                dialogo,
                                "Correo electrónico inválido."
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX TELEFONO
                    // -----------------------------------------

                    if (!RegexControl.telefono(t)) {

                        mensajeEn(
                                dialogo,
                                "Teléfono inválido. Debe contener exactamente 10 números."
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX FECHA
                    // -----------------------------------------

                    if (!RegexControl.fecha(f)) {

                        mensajeEn(
                                dialogo,
                                "Fecha inválida. Usa el formato dd/MM/yyyy."
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX PESO
                    // -----------------------------------------

                    if (!RegexControl.peso(p)) {

                        mensajeEn(
                                dialogo,
                                "Peso inválido. Ejemplo: 75.5"
                        );

                        return;
                    }

                    // -----------------------------------------
                    // REGEX ALTURA
                    // -----------------------------------------

                    if (!RegexControl.altura(a)) {

                        mensajeEn(
                                dialogo,
                                "Altura inválida. Ejemplo: 1.75"
                        );

                        return;
                    }

                    if (
                            "Sin membresía".equals(
                                    membresia.getSelectedItem()
                            )
                            && !cfgPermitirSinMembresia
                    ) {

                        mensajeEn(
                                dialogo,
                                "Selecciona una membresía (o activa "
                                + "\"Permitir registro de socios sin "
                                + "membresía\" en Configuración)."
                        );

                        return;
                    }

                    try {

                        double pesoNumero =
                                Double.parseDouble(p);

                        double alturaNumero =
                                Double.parseDouble(a);

                        String membresiaTexto =
                                membresia
                                        .getSelectedItem()
                                        .toString();

                        if (editar) {

                            socio.setNombre(n);
                            socio.setDni(d);
                            socio.setCorreo(c);
                            socio.setTelefono(t);
                            socio.setFechaNacimiento(f);
                            socio.setMembresia(
                                    membresiaTexto
                            );
                            socio.setPeso(
                                    pesoNumero
                            );
                            socio.setAltura(
                                    alturaNumero
                            );

                            socio.setFotoPath(
                                    rutaFotoSocio[0]
                            );

                            // La fecha de vencimiento SOLO se toca
                            // cuando esto es explícitamente una
                            // renovación (botón "Renovar" desde
                            // Vencidas / Vencen Pronto). Una edición
                            // normal de datos no debe mover fechas
                            // ni generar pagos por su cuenta.
                            if (
                                    renovacion
                                    && !"Sin membresía".equals(membresiaTexto)
                            ) {

                                socio.setFechaVencimiento(
                                        control.renovarVencimiento(
                                                membresiaTexto
                                        )
                                );
                            }

                            socioGuardado[0] = socio;

                        } else {

                            if (control.getSocios().size() >= 1000) {

                                mensajeEn(
                                        dialogo,
                                        "Se alcanzó el límite máximo de 1000 socios registrados."
                                );

                                return;
                            }

                            if (control.getSocios().size() >= 950
                                    && cfgMostrarNotificaciones) {

                                JOptionPane.showMessageDialog(
                                        dialogo,
                                        "Atención: estás cerca del límite de 1000 socios ("
                                        + control.getSocios().size()
                                        + "/1000).",
                                        "Aviso de capacidad",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            }

                            Socio nuevo =
                                    control.agregarSocio(
                                            n,
                                            d,
                                            c,
                                            t,
                                            f,
                                            membresiaTexto,
                                            pesoNumero,
                                            alturaNumero
                                    );

                            nuevo.setFotoPath(
                                    rutaFotoSocio[0]
                            );

                            socioGuardado[0] = nuevo;
                        }

                        Socio s = socioGuardado[0];

                        // Persiste en la base de datos (si hay
                        // conexión) lo que se acaba de editar,
                        // crear o renovar.
                        control.actualizarSocio(s);

                        // -----------------------------------------
                        // PAGO (solo en Alta nueva o en Renovación)
                        // -----------------------------------------
                        // El método de pago real se ajusta y el
                        // comprobante se envía desde Pagos > Editar
                        // pago. Aquí solo se deja registrado con
                        // "Efectivo" por defecto.

                        boolean generaPago = !editar || renovacion;

                        double precioPlan =
                                control.precioMembresia(membresiaTexto);

                        if (generaPago && precioPlan > 0) {

                            String conceptoPago =
                                    editar
                                            ? "Renovación de membresía"
                                            : "Alta";

                            String fechaPago =
                                    new SimpleDateFormat("dd/MM/yyyy")
                                            .format(new Date());

                            control.agregarPago(
                                    s.getId(),
                                    s.getNombre(),
                                    membresiaTexto,
                                    precioPlan,
                                    fechaPago,
                                    "Efectivo",
                                    conceptoPago
                            );

                            control.registrarActividad(
                                    editar ? "socio_renovado" : "socio_nuevo",
                                    editar
                                            ? "Membresía renovada"
                                            : "Nuevo socio registrado",
                                    s.getNombre() + " - " + membresiaTexto
                                    + " - $" + String.format("%.2f", precioPlan)
                            );

                        } else {

                            control.registrarActividad(
                                    !editar ? "socio_nuevo" : "socio_editado",
                                    !editar
                                            ? "Nuevo socio registrado"
                                            : "Socio actualizado",
                                    s.getNombre()
                            );
                        }

                        dialogo.dispose();

                        mostrarSocios();

                    } catch (
                            NumberFormatException ex
                    ) {

                        mensajeEn(
                                dialogo,
                                "Peso o altura inválidos."
                        );
                    }
                }
        );

        col.add(
                Box.createVerticalStrut(8)
        );

        col.add(guardar);

        // Renovar, Editar y Nuevo socio usan exactamente el mismo
        // tamaño de columna. Antes, en "Renovar" se mezclaban
        // componentes alineados a la izquierda (separador y aviso)
        // con otros centrados dentro del mismo BoxLayout, y Java
        // encogía toda la columna: los campos y textos se veían
        // mucho más chicos que en "Editar". Ahora todos se alinean
        // igual y la columna tiene un ancho fijo.
        for (Component hijo : col.getComponents()) {

            if (hijo instanceof JComponent) {

                ((JComponent) hijo).setAlignmentX(
                        Component.CENTER_ALIGNMENT
                );
            }
        }

        col.setPreferredSize(
                new Dimension(340, col.getPreferredSize().height)
        );

        col.setMinimumSize(new Dimension(340, 0));

        panel.add(col);

        // "panel" ya no ocupa todo el ancho (su contenido real es
        // "col", limitado a 340px), así que se centra dentro del
        // diálogo en vez de quedar pegado a la izquierda con un
        // hueco vacío a la derecha.

        JPanel centradoSocio = new Pagina();

        centradoSocio.setLayout(new GridBagLayout());

        centradoSocio.setBackground(PANEL);

        centradoSocio.add(panel);

        JScrollPane scrollSocio = new JScrollPane(centradoSocio);

        scrollSocio.setBorder(null);

        scrollSocio.getViewport().setBackground(PANEL);

        scrollSocio.getVerticalScrollBar().setUnitIncrement(16);

        dialogo.add(scrollSocio);

        dialogo.setVisible(true);
    }

    // =========================================================
    // MEMBRESÍAS
    // =========================================================

    private void mostrarMembresias() {

        seccionVisible = "membresias";

        activarNav(2);

        pageTitle.setText(
                "Gestión de Membresías"
        );

        JPanel page = base();

        JPanel head =
                moduleHead(
                        "Membresías",
                        "Administra los planes, precios y duración.",
                        "Nueva membresía",
                        Icono.PLUS,
                        Icono.CARD
                );

        JButton boton =
                (JButton) head.getClientProperty(
                        "actionButton"
                );

        boton.addActionListener(
                e -> dialogoMembresia(null)
        );

        page.add(head);

        page.add(
                Box.createVerticalStrut(18)
        );

        JPanel searchBarMem =
                new JPanel(new BorderLayout());

        searchBarMem.setOpaque(false);

        searchBarMem.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchBarMem.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        JTextField searchMem = field();

        searchMem.setToolTipText(
                "Buscar por nombre o descripción..."
        );

        searchBarMem.add(searchMem, BorderLayout.CENTER);

        page.add(searchBarMem);

        page.add(
                Box.createVerticalStrut(14)
        );

        JPanel grid =
                new JPanel(
                        new GridLayout(
                                0,
                                3,
                                15,
                                15
                        )
                );

        grid.setOpaque(false);

        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable cargarGrid = () -> {

            grid.removeAll();

            String texto =
                    searchMem.getText().trim().toLowerCase();

            for (
                    Membresia m :
                    control.getMembresias()
            ) {

                String datos =
                        (m.getNombre() + " " + m.getDescripcion())
                                .toLowerCase();

                if (texto.isEmpty() || datos.contains(texto)) {

                    grid.add(
                            membershipCard(m)
                    );
                }
            }

            grid.revalidate();
            grid.repaint();
        };

        cargarGrid.run();

        searchMem.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        cargarGrid.run();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        cargarGrid.run();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        cargarGrid.run();
                    }
                }
        );

        page.add(grid);

        mostrarContenido(page);
    }

    // =========================================================
    // TARJETA MEMBRESIA
    // =========================================================

    private JPanel membershipCard(
            Membresia m
    ) {

        JPanel p = card();

        p.setBorder(
                new EmptyBorder(
                        18,
                        18,
                        18,
                        18
                )
        );

        p.setLayout(
                new BorderLayout(
                        0,
                        10
                )
        );

        p.add(
                new JLabel(
                        new Icono(
                                Icono.CALENDAR,
                                GOLD,
                                27
                        )
                ),
                BorderLayout.NORTH
        );

        JPanel centro =
                new JPanel();

        centro.setOpaque(false);

        centro.setLayout(
                new BoxLayout(
                        centro,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel nombre =
                new JLabel(
                        m.getNombre()
                );

        nombre.setForeground(TEXT);

        nombre.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        19
                )
        );

        JLabel descripcion =
                new JLabel(
                        "<html>"
                        + m.getDescripcion()
                        + "</html>"
                );

        descripcion.setForeground(MUTED);

        JLabel precio =
                new JLabel(
                        String.format(
                                "$%.2f",
                                m.getPrecio()
                        )
                );

        precio.setForeground(GOLD);

        precio.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        25
                )
        );

        JLabel dias =
                new JLabel(
                        m.getDuracionDias()
                        + " días"
                );

        dias.setForeground(MUTED);

        centro.add(nombre);

        centro.add(
                Box.createVerticalStrut(6)
        );

        centro.add(descripcion);

        centro.add(
                Box.createVerticalStrut(10)
        );

        centro.add(precio);

        centro.add(dias);

        p.add(
                centro,
                BorderLayout.CENTER
        );

        JPanel botones =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                7,
                                0
                        )
                );

        botones.setOpaque(false);

        JButton editar =
                dark("EDITAR");

        JButton eliminar =
                dark("ELIMINAR");

        editar.addActionListener(
                e -> dialogoMembresia(m)
        );

        eliminar.addActionListener(
                e -> {

                    if (
                            confirm(
                                    "¿Eliminar la membresía "
                                    + m.getNombre()
                                    + "?"
                            )
                    ) {

                        control.eliminarMembresia(
                                m.getId()
                        );

                        control.registrarActividad(
                                "membresia_eliminada",
                                "Membresía eliminada",
                                m.getNombre()
                        );

                        mostrarMembresias();
                    }
                }
        );

        botones.add(editar);
        botones.add(eliminar);

        p.add(
                botones,
                BorderLayout.SOUTH
        );

        return p;
    }

    // =========================================================
    // DIALOGO MEMBRESIA
    // =========================================================

    private void dialogoMembresia(
            Membresia membresia
    ) {

        boolean editar =
                membresia != null;

        JDialog d =
                new JDialog(
                        this,
                        editar
                                ? "Editar membresía"
                                : "Nueva membresía",
                        true
                );

        d.setSize(
                500,
                480
        );

        d.setLocationRelativeTo(this);

        JPanel p =
                formPanel();

        p.add(
                bigTitle(
                        editar
                                ? "Actualizar membresía"
                                : "Nueva membresía",
                        "DATOS DEL PLAN"
                )
        );

        JTextField nombre =
                field();

        JTextField descripcion =
                field();

        JTextField dias =
                field();

        JTextField precio =
                field();

        if (editar) {

            nombre.setText(
                    membresia.getNombre()
            );

            descripcion.setText(
                    membresia.getDescripcion()
            );

            dias.setText(
                    String.valueOf(
                            membresia
                                    .getDuracionDias()
                    )
            );

            precio.setText(
                    String.valueOf(
                            membresia
                                    .getPrecio()
                    )
            );

            java.util.Set<String> diasEditables =
                    new java.util.HashSet<>(
                            java.util.Arrays.asList(
                                    "Día", "Estudiante", "Básica", "Premium"
                            )
                    );

            if (!diasEditables.contains(membresia.getNombre())) {

                dias.setEditable(false);

                dias.setToolTipText(
                        "La duración de este plan es fija y no se puede modificar."
                );

                dias.setBackground(PANEL2);

                dias.setForeground(MUTED);
            }
        }

        addField(
                p,
                "Nombre",
                nombre
        );

        addField(
                p,
                "Descripción",
                descripcion
        );

        addField(
                p,
                "Duración en días",
                dias
        );

        addField(
                p,
                "Precio",
                precio
        );

        JButton guardar =
                gold(
                        "GUARDAR",
                        Icono.CHECK
                );

        guardar.addActionListener(
                e -> {

                    String n =
                            nombre.getText()
                                    .trim();

                    String desc =
                            descripcion.getText()
                                    .trim();

                    String dstr =
                            dias.getText()
                                    .trim();

                    String pr =
                            precio.getText()
                                    .trim();

                    if (
                            n.length() < 2
                            || n.length() > 40
                    ) {

                        mensajeEn(
                                d,
                                "El nombre debe tener entre 2 y 40 caracteres."
                        );

                        return;
                    }

                    if (
                            desc.length() < 3
                    ) {

                        mensajeEn(
                                d,
                                "Escribe una descripción."
                        );

                        return;
                    }

                    if (
                            !RegexControl.numero(dstr)
                    ) {

                        mensajeEn(
                                d,
                                "La duración debe ser un número entero."
                        );

                        return;
                    }

                    if (
                            !RegexControl.dinero(pr)
                    ) {

                        mensajeEn(
                                d,
                                "Precio inválido. Ejemplo: 650.00"
                        );

                        return;
                    }

                    try {

                        int diasNumero =
                                Integer.parseInt(dstr);

                        double precioNumero =
                                Double.parseDouble(pr);

                        if (editar) {

                            membresia.setNombre(n);
                            membresia.setDescripcion(
                                    desc
                            );
                            membresia.setDuracionDias(
                                    diasNumero
                            );
                            membresia.setPrecio(
                                    precioNumero
                            );

                            control.actualizarMembresia(membresia);

                        } else {

                            control.agregarMembresia(
                                    n,
                                    desc,
                                    diasNumero,
                                    precioNumero
                            );
                        }

                        control.registrarActividad(
                                editar ? "membresia_editada" : "membresia_nueva",
                                editar ? "Membresía actualizada" : "Nueva membresía",
                                n + " - $" + String.format("%.2f", precioNumero)
                        );

                        d.dispose();

                        mostrarMembresias();

                    } catch (
                            NumberFormatException ex
                    ) {

                        mensajeEn(
                                d,
                                "Duración o precio inválidos."
                        );
                    }
                }
        );

        p.add(
                Box.createVerticalStrut(10)
        );

        p.add(guardar);

        d.add(p);

        d.setVisible(true);
    }

    // =========================================================
    // PAGOS
    // =========================================================

    private void mostrarPagos() {

        seccionVisible = "pagos";

        activarNav(3);

        pageTitle.setText(
                "Gestión de Pagos"
        );

        JPanel page = base();

        JPanel head =
                moduleHead(
                        "Pagos",
                        "Registra y consulta los pagos realizados por los socios.",
                        "Registrar pago",
                        Icono.PLUS,
                        Icono.MONEY
                );

        JButton boton =
                (JButton) head.getClientProperty(
                        "actionButton"
                );

        boton.addActionListener(
                e -> dialogoPago()
        );

        page.add(head);

        page.add(
                Box.createVerticalStrut(18)
        );

        JPanel resumen =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                12,
                                0
                        )
                );

        resumen.setOpaque(false);

        resumen.setAlignmentX(Component.LEFT_ALIGNMENT);

        resumen.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
                )
        );

        resumen.add(
                smallStat(
                        "Pagos registrados",
                        control.totalPagos(),
                        GOLD,
                        Icono.CARD
                )
        );

        resumen.add(
                smallStat(
                        "Ingresos",
                        String.format(
                                "$ %.2f",
                                control.totalIngresos()
                        ),
                        GREEN,
                        Icono.MONEY
                )
        );

        page.add(resumen);

        page.add(
                Box.createVerticalStrut(14)
        );

        // -----------------------------------------------------
        // BUSCADOR Y FILTROS
        // -----------------------------------------------------

        JPanel searchBarPagos =
                new JPanel(new BorderLayout(10, 0));

        searchBarPagos.setOpaque(false);

        searchBarPagos.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchBarPagos.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        JTextField searchPagos = field();

        searchPagos.setToolTipText(
                "Buscar por socio, membresía o concepto..."
        );

        searchBarPagos.add(searchPagos, BorderLayout.CENTER);

        JPanel filtrosPagos =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        filtrosPagos.setOpaque(false);

        JComboBox<String> metodoFiltro =
                combo(new String[]{
                    "Todos los métodos",
                    "Efectivo",
                    "Tarjeta",
                    "Transferencia"
                });

        filtrosPagos.add(metodoFiltro);

        java.util.LinkedHashSet<String> mesesSet =
                new java.util.LinkedHashSet<>();

        mesesSet.add("Todos los meses");

        for (Pago p : control.getPagos()) {

            String f = p.getFecha();

            if (f != null && f.length() >= 7) {

                mesesSet.add(f.substring(3));
            }
        }

        JComboBox<String> mesFiltro =
                combo(mesesSet.toArray(new String[0]));

        filtrosPagos.add(mesFiltro);

        searchBarPagos.add(filtrosPagos, BorderLayout.EAST);

        page.add(searchBarPagos);

        page.add(
                Box.createVerticalStrut(10)
        );

        JPanel tablaCard = card();

        tablaCard.setLayout(
                new BorderLayout()
        );

        tablaCard.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        0,
                        0
                )
        );

        String[] columnas = {
            "ID",
            "SOCIO",
            "MEMBRESÍA",
            "MONTO",
            "FECHA",
            "MÉTODO",
            "CONCEPTO",
            "ACCIONES"
        };

        DefaultTableModel tm =
                modelo(columnas);

        Runnable cargarPagos = () -> {

            tm.setRowCount(0);

            String texto =
                    searchPagos.getText()
                            .trim()
                            .toLowerCase();

            String metodoSel =
                    (String) metodoFiltro.getSelectedItem();

            String mesSel =
                    (String) mesFiltro.getSelectedItem();

            for (
                    Pago p :
                    control.getPagos()
            ) {

                String datos =
                        (
                                p.getSocioNombre()
                                + " "
                                + p.getMembresia()
                                + " "
                                + p.getConcepto()
                        ).toLowerCase();

                boolean coincideTexto =
                        texto.isEmpty()
                        || datos.contains(texto);

                boolean coincideMetodo =
                        metodoSel == null
                        || metodoSel.equals("Todos los métodos")
                        || metodoSel.equals(p.getMetodoPago());

                String mesPago =
                        (p.getFecha() != null
                        && p.getFecha().length() >= 7)
                                ? p.getFecha().substring(3)
                                : "";

                boolean coincideMes =
                        mesSel == null
                        || mesSel.equals("Todos los meses")
                        || mesSel.equals(mesPago);

                if (coincideTexto && coincideMetodo && coincideMes) {

                    tm.addRow(
                            new Object[]{
                                p.getId(),
                                p.getSocioNombre(),
                                p.getMembresia(),
                                String.format(
                                        "$%.2f",
                                        p.getMonto()
                                ),
                                p.getFecha(),
                                p.getMetodoPago(),
                                p.getConcepto(),
                                "acciones"
                            }
                    );
                }
            }
        };

        cargarPagos.run();

        JTable tablaPagos = tabla(tm);

        java.util.Map<String, Color> coloresMetodo = new java.util.HashMap<>();

        coloresMetodo.put("Tarjeta", BLUE);
        coloresMetodo.put("Efectivo", GREEN);
        coloresMetodo.put("Transferencia", PURPLE);

        tablaPagos.getColumnModel()
                .getColumn(5)
                .setCellRenderer(
                        new PillRenderer(coloresMetodo)
                );

        tablaPagos.setRowHeight(44);

        tablaPagos.getColumnModel()
                .getColumn(1)
                .setCellRenderer(
                        new NombreAvatarRenderer(
                                pagoId -> {

                                    Pago p = control.buscarPago(pagoId);

                                    if (p == null) {
                                        return "";
                                    }

                                    Socio s = control.buscarSocio(p.getSocioId());

                                    return s == null ? "" : s.getFotoPath();
                                }
                        )
                );

        tablaPagos.getColumnModel()
                .getColumn(7)
                .setCellRenderer(
                        new PagoActionRenderer()
                );

        int[] anchosPagos = {
            50, 190, 120, 100, 100, 120, 140, 130
        };

        for (int i = 0; i < anchosPagos.length; i++) {

            tablaPagos.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(anchosPagos[i]);
        }

        tablaPagos.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        int fila = tablaPagos.rowAtPoint(e.getPoint());
                        int col = tablaPagos.columnAtPoint(e.getPoint());

                        if (fila < 0 || col != 7) {
                            return;
                        }

                        int pagoId =
                                (int) tablaPagos.getValueAt(fila, 0);

                        Pago p = control.buscarPago(pagoId);

                        if (p == null) {
                            return;
                        }

                        java.awt.Rectangle celda =
                                tablaPagos.getCellRect(fila, col, false);

                        int zona =
                                zonaAccion(
                                        e.getX() - celda.x,
                                        celda.width,
                                        3
                                );

                        if (zona == 0) {

                            mensaje(
                                    "Socio: " + p.getSocioNombre() + "\n"
                                    + "Membresía: " + p.getMembresia() + "\n"
                                    + "Monto: $" + String.format("%.2f", p.getMonto()) + "\n"
                                    + "Fecha: " + p.getFecha() + "\n"
                                    + "Método: " + p.getMetodoPago() + "\n"
                                    + "Concepto: " + p.getConcepto()
                            );

                        } else if (zona == 1) {

                            dialogoEditarPago(p, cargarPagos);

                        } else {

                            int r = JOptionPane.showConfirmDialog(
                                    MainFrame.this,
                                    "¿Eliminar este pago de forma permanente?",
                                    "Confirmar eliminación",
                                    JOptionPane.YES_NO_OPTION
                            );

                            if (r == JOptionPane.YES_OPTION) {

                                control.eliminarPago(pagoId);

                                control.registrarActividad(
                                        "pago",
                                        "Pago eliminado",
                                        p.getSocioNombre() + " - $"
                                        + String.format("%.2f", p.getMonto())
                                );

                                cargarPagos.run();

                                mostrarPagos();
                            }
                        }
                    }
                }
        );

        searchPagos.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        cargarPagos.run();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        cargarPagos.run();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        cargarPagos.run();
                    }
                }
        );

        metodoFiltro.addActionListener(e -> cargarPagos.run());
        mesFiltro.addActionListener(e -> cargarPagos.run());

        tablaCard.add(
                tablaSinScroll(tablaPagos),
                BorderLayout.CENTER
        );

        page.add(tablaCard);

        mostrarContenido(page);
    }

    // =========================================================
    // DIALOGO PAGO
    // =========================================================

    // =========================================================
    // EDITAR PAGO (método, concepto y fecha)
    // =========================================================

    private void dialogoEditarPago(Pago pago, Runnable recargar) {

        JDialog d =
                new JDialog(this, "Editar pago", true);

        d.setSize(360, 440);
        d.setLocationRelativeTo(this);

        JPanel panel = formPanel();

        // Antes método de pago, concepto y fecha estiraban su
        // combo/textfield a todo el ancho del diálogo (se veían
        // muy gruesos). "col" los limita a 260px y los centra.

        JPanel col = new JPanel();

        col.setOpaque(false);

        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.setMaximumSize(new Dimension(260, Integer.MAX_VALUE));

        JComboBox<String> metodo =
                combo(new String[]{"Efectivo", "Tarjeta", "Transferencia"});

        metodo.setSelectedItem(pago.getMetodoPago());

        addField(col, "Método de pago", metodo);

        JTextField concepto = field();

        concepto.setText(pago.getConcepto());

        addField(col, "Concepto", concepto);

        JPanel fechaPanel = new JPanel(new BorderLayout(6, 0));

        fechaPanel.setOpaque(false);

        JTextField fecha = field();

        fecha.setText(pago.getFecha());
        fecha.setEditable(false);

        fechaPanel.add(fecha, BorderLayout.CENTER);

        JButton calBtn = dark("", Icono.CALENDAR);

        calBtn.setPreferredSize(new Dimension(38, 32));

        calBtn.addActionListener(e -> mostrarJCalendarPopup(fecha));

        fechaPanel.add(calBtn, BorderLayout.EAST);

        addField(col, "Fecha", fechaPanel);

        Socio socioDelPago = control.buscarSocio(pago.getSocioId());

        JCheckBox chkComprobante = new JCheckBox(
                "Enviar comprobante al correo del socio"
        );

        chkComprobante.setSelected(false);

        chkComprobante.setOpaque(false);

        chkComprobante.setForeground(TEXT);

        chkComprobante.setFont(
                f("SansSerif", Font.BOLD, 11)
        );

        chkComprobante.setAlignmentX(Component.LEFT_ALIGNMENT);

        chkComprobante.setFocusPainted(false);

        chkComprobante.setBorder(
                new EmptyBorder(6, 2, 4, 0)
        );

        col.add(chkComprobante);

        JLabel avisoCorreo = new JLabel(
                socioDelPago == null
                        ? "<html>No se encontró al socio de este pago.</html>"
                        : (
                                socioDelPago.getCorreo() == null
                                || socioDelPago.getCorreo().trim().isEmpty()
                        )
                                ? "<html>Este socio no tiene correo registrado.</html>"
                                : "<html>Se enviará a: " + socioDelPago.getCorreo() + "</html>"
        );

        avisoCorreo.setForeground(MUTED);

        avisoCorreo.setFont(
                f("SansSerif", Font.ITALIC, 10)
        );

        avisoCorreo.setAlignmentX(Component.LEFT_ALIGNMENT);

        avisoCorreo.setBorder(
                new EmptyBorder(0, 2, 8, 0)
        );

        col.add(avisoCorreo);

        JButton guardar = gold("Guardar cambios", Icono.CHECK);

        guardar.addActionListener(e -> {

            pago.setMetodoPago(
                    (String) metodo.getSelectedItem()
            );

            pago.setConcepto(
                    concepto.getText().trim()
            );

            pago.setFecha(fecha.getText());

            control.actualizarPago(pago);

            if (chkComprobante.isSelected() && socioDelPago != null) {

                generarYEnviarComprobante(
                        socioDelPago,
                        pago,
                        true
                );
            }

            recargar.run();

            d.dispose();

            mostrarPagos();
        });

        col.add(guardar);

        panel.add(col);

        JPanel centradoPago = new Pagina();

        centradoPago.setLayout(new GridBagLayout());

        centradoPago.setBackground(PANEL);

        centradoPago.add(panel);

        JScrollPane scrollPago = new JScrollPane(centradoPago);

        scrollPago.setBorder(null);

        // El fondo blanco que se veía debajo de "Guardar cambios" era
        // el color por defecto del viewport del JScrollPane (se
        // asoma cuando el contenido es más corto que el diálogo).
        scrollPago.getViewport().setBackground(PANEL);

        scrollPago.setBackground(PANEL);

        scrollPago.getVerticalScrollBar().setUnitIncrement(16);

        d.setContentPane(scrollPago);

        // Además, el diálogo se ajusta al alto real del contenido
        // (en vez de un alto fijo de 440px que dejaba mucho vacío).
        d.pack();

        d.setSize(360, d.getHeight());

        d.setLocationRelativeTo(this);

        d.setVisible(true);
    }

    private void dialogoPago() {

        if (
                control.getSocios().isEmpty()
        ) {

            mensaje(
                    "Primero registra un socio."
            );

            return;
        }

        JDialog d =
                new JDialog(
                        this,
                        "Registrar pago",
                        true
                );

        d.setSize(
                540,
                530
        );

        d.setLocationRelativeTo(this);

        JPanel p =
                formPanel();

        p.add(
                bigTitle(
                        "Nuevo pago",
                        "REGISTRO FINANCIERO"
                )
        );

        JComboBox<String> socio =
                new JComboBox<>();

        for (
                Socio s :
                control.getSocios()
        ) {

            if (s.isActivo()) {

                socio.addItem(
                        s.getNombre()
                );
            }
        }

        JComboBox<String> membresia =
                new JComboBox<>();

        for (
                Membresia m :
                control.getMembresias()
        ) {

            if (m.isActiva()) {

                membresia.addItem(
                        m.getNombre()
                );
            }
        }

        JComboBox<String> metodo =
                new JComboBox<>(
                        new String[]{
                            "Efectivo",
                            "Tarjeta",
                            "Transferencia"
                        }
                );

        JTextField monto =
                field();

        JTextField fecha =
                field();

        JTextField concepto =
                field();

        fecha.setText(
                new SimpleDateFormat(
                        "dd/MM/yyyy"
                ).format(
                        new Date()
                )
        );

        concepto.setText(
                "Membresía"
        );

        addField(
                p,
                "Socio",
                socio
        );

        addField(
                p,
                "Membresía",
                membresia
        );

        addField(
                p,
                "Monto",
                monto
        );

        monto.setEditable(false);

        monto.setBackground(PANEL2);

        monto.setForeground(GOLD);

        Runnable actualizarMonto = () -> {

            String seleccion = (String) membresia.getSelectedItem();

            for (Membresia m : control.getMembresias()) {

                if (m.getNombre().equals(seleccion)) {

                    monto.setText(
                            String.format("%.2f", m.getPrecio())
                    );

                    break;
                }
            }
        };

        membresia.addItemListener(e -> actualizarMonto.run());

        actualizarMonto.run();

        JPanel fechaPanel =
                new JPanel(
                        new BorderLayout(
                                5,
                                0
                        )
                );

        fechaPanel.setOpaque(false);

        fechaPanel.add(
                fecha,
                BorderLayout.CENTER
        );

        JButton cal =
                new JButton(
                        new Icono(
                                Icono.CALENDAR,
                                GOLD,
                                17
                        )
                );

        cal.setPreferredSize(
                new Dimension(
                        40,
                        30
                )
        );

        cal.setFocusPainted(false);

        cal.setBackground(PANEL2);

        cal.setBorder(
                new LineBorder(
                        BORDER
                )
        );

        cal.addActionListener(
                e -> mostrarJCalendarPopup(fecha)
        );

        fechaPanel.add(
                cal,
                BorderLayout.EAST
        );

        addField(
                p,
                "Fecha",
                fechaPanel
        );

        addField(
                p,
                "Método",
                metodo
        );

        addField(
                p,
                "Concepto",
                concepto
        );

        JButton guardar =
                gold(
                        "REGISTRAR PAGO",
                        Icono.CHECK
                );

        guardar.addActionListener(
                e -> {

                    String montoTexto =
                            monto.getText()
                                    .trim();

                    String fechaTexto =
                            fecha.getText()
                                    .trim();

                    if (
                            !RegexControl.dinero(
                                    montoTexto
                            )
                    ) {

                        mensajeEn(
                                d,
                                "Monto inválido."
                        );

                        return;
                    }

                    if (
                            !RegexControl.fecha(
                                    fechaTexto
                            )
                    ) {

                        mensajeEn(
                                d,
                                "Fecha inválida. Usa dd/MM/yyyy."
                        );

                        return;
                    }

                    if (
                            socio.getSelectedItem()
                            == null
                            ||
                            membresia
                                    .getSelectedItem()
                            == null
                    ) {

                        mensajeEn(
                                d,
                                "Selecciona socio y membresía."
                        );

                        return;
                    }

                    try {

                        double montoNumero =
                                Double.parseDouble(
                                        montoTexto
                                );

                        String socioNombre =
                                socio
                                        .getSelectedItem()
                                        .toString();

                        String membresiaTexto =
                                membresia
                                        .getSelectedItem()
                                        .toString();

                        String metodoTexto =
                                metodo
                                        .getSelectedItem()
                                        .toString();

                        String conceptoTexto =
                                concepto
                                        .getText()
                                        .trim();

                        Socio encontrado =
                                null;

                        for (
                                Socio s :
                                control.getSocios()
                        ) {

                            if (
                                    s.getNombre()
                                            .equals(
                                                    socioNombre
                                            )
                            ) {

                                encontrado = s;
                                break;
                            }
                        }

                        if (encontrado == null) {

                            mensajeEn(
                                    d,
                                    "No se encontró el socio seleccionado."
                            );

                            return;
                        }

                        control.agregarPago(
                                encontrado.getId(),
                                socioNombre,
                                membresiaTexto,
                                montoNumero,
                                fechaTexto,
                                metodoTexto,
                                conceptoTexto
                        );

                        if (!control.getPagos().isEmpty()) {

                            Pago ultimoPago =
                                    control.getPagos().get(
                                            control.getPagos().size() - 1
                                    );

                            if (cfgImprimirComprobantes) {

                                generarYEnviarComprobante(
                                        encontrado,
                                        ultimoPago,
                                        true
                                );
                            }

                            control.registrarActividad(
                                    "pago",
                                    "Pago registrado",
                                    socioNombre + " - $"
                                    + String.format("%.2f", montoNumero)
                            );
                        }

                        d.dispose();

                        mostrarPagos();

                    } catch (
                            NumberFormatException ex
                    ) {

                        mensajeEn(
                                d,
                                "Monto inválido."
                        );
                    }
                }
        );

        p.add(
                Box.createVerticalStrut(8)
        );

        p.add(guardar);

        d.add(
                new JScrollPane(p)
        );

        d.setVisible(true);
    }

    // =========================================================
    // ENTRENADORES
    // =========================================================

    private void mostrarEntrenadores() {

        seccionVisible = "entrenadores";

        activarNav(4);

        pageTitle.setText(
                "Gestión de Entrenadores"
        );

        JPanel page = base();

        JPanel head =
                moduleHead(
                        "Entrenadores",
                        "Administra el personal encargado del gimnasio.",
                        "Nuevo entrenador",
                        Icono.PLUS,
                        Icono.TRAINER
                );

        JButton boton =
                (JButton) head.getClientProperty(
                        "actionButton"
                );

        boton.addActionListener(
                e -> dialogoEntrenador(null)
        );

        page.add(head);

        page.add(
                Box.createVerticalStrut(18)
        );

        // -----------------------------------------------------
        // TARJETAS
        // -----------------------------------------------------

        JPanel statsEnt =
                new JPanel(new GridLayout(1, 4, 14, 0));

        statsEnt.setOpaque(false);

        statsEnt.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsEnt.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 110)
        );

        statsEnt.add(
                stat(
                        "Total de Entrenadores",
                        control.totalEntrenadores(),
                        "Registrados",
                        GOLD,
                        Icono.TRAINER
                )
        );

        statsEnt.add(
                stat(
                        "Activos",
                        control.entrenadoresActivos(),
                        control.entrenadoresActivos()
                                + " de " + control.totalEntrenadores(),
                        GREEN,
                        Icono.CHECK
                )
        );

        statsEnt.add(
                stat(
                        "Inactivos",
                        control.entrenadoresInactivos(),
                        "Sin cambios",
                        RED,
                        Icono.BLOCK
                )
        );

        statsEnt.add(
                stat(
                        "Salario Promedio",
                        String.format(
                                "$ %.2f",
                                control.salarioPromedioEntrenadores()
                        ),
                        "Promedio actual",
                        PURPLE,
                        Icono.MONEY
                )
        );

        page.add(statsEnt);

        page.add(
                Box.createVerticalStrut(16)
        );

        // -----------------------------------------------------
        // BUSCADOR Y FILTROS
        // -----------------------------------------------------

        JPanel searchBarEnt =
                new JPanel(new BorderLayout(10, 0));

        searchBarEnt.setOpaque(false);

        searchBarEnt.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchBarEnt.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        JTextField searchEnt = field();

        searchEnt.setToolTipText(
                "Buscar por nombre, especialidad, teléfono o correo..."
        );

        searchBarEnt.add(searchEnt, BorderLayout.CENTER);

        JPanel filtrosEnt =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        filtrosEnt.setOpaque(false);

        JComboBox<String> estadoFiltroEnt =
                combo(new String[]{"Todos los estados", "Activo", "Inactivo"});

        filtrosEnt.add(estadoFiltroEnt);

        java.util.LinkedHashSet<String> especialidadesSet =
                new java.util.LinkedHashSet<>();

        especialidadesSet.add("Todas las especialidades");

        for (Entrenador e : control.getEntrenadores()) {
            especialidadesSet.add(e.getEspecialidad());
        }

        JComboBox<String> especialidadFiltro =
                combo(especialidadesSet.toArray(new String[0]));

        filtrosEnt.add(especialidadFiltro);

        JButton filtrarEntBtn = gold("Filtrar", Icono.CHECK);

        filtrosEnt.add(filtrarEntBtn);

        searchBarEnt.add(filtrosEnt, BorderLayout.EAST);

        page.add(searchBarEnt);

        page.add(
                Box.createVerticalStrut(10)
        );

        JPanel tablaCard = card();

        tablaCard.setLayout(
                new BorderLayout()
        );

        tablaCard.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        0,
                        0
                )
        );

        String[] columnas = {
            "ID",
            "ENTRENADOR",
            "ESPECIALIDAD",
            "TELÉFONO",
            "CORREO",
            "SALARIO",
            "ESTADO",
            "ACCIONES"
        };

        DefaultTableModel tm =
                modelo(columnas);

        Runnable cargarEntrenadores = () -> {

            tm.setRowCount(0);

            String texto =
                    searchEnt.getText().trim().toLowerCase();

            String estadoSel =
                    (String) estadoFiltroEnt.getSelectedItem();

            String espSel =
                    (String) especialidadFiltro.getSelectedItem();

            for (
                    Entrenador e :
                    control.getEntrenadores()
            ) {

                String datos =
                        (
                                e.getNombre() + " "
                                + e.getEspecialidad() + " "
                                + e.getTelefono() + " "
                                + e.getCorreo()
                        ).toLowerCase();

                boolean coincideTexto =
                        texto.isEmpty() || datos.contains(texto);

                boolean coincideEstado =
                        estadoSel == null
                        || estadoSel.equals("Todos los estados")
                        || (estadoSel.equals("Activo") && e.isActivo())
                        || (estadoSel.equals("Inactivo") && !e.isActivo());

                boolean coincideEsp =
                        espSel == null
                        || espSel.equals("Todas las especialidades")
                        || espSel.equals(e.getEspecialidad());

                if (coincideTexto && coincideEstado && coincideEsp) {

                    tm.addRow(
                            new Object[]{
                                e.getId(),
                                e.getNombre(),
                                e.getEspecialidad(),
                                e.getTelefono(),
                                e.getCorreo(),
                                String.format(
                                        "$%.2f",
                                        e.getSalario()
                                ),
                                e.isActivo()
                                        ? "Activo"
                                        : "Inactivo",
                                "acciones"
                            }
                    );
                }
            }
        };

        cargarEntrenadores.run();

        searchEnt.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        cargarEntrenadores.run();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        cargarEntrenadores.run();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        cargarEntrenadores.run();
                    }
                }
        );

        filtrarEntBtn.addActionListener(e -> cargarEntrenadores.run());
        estadoFiltroEnt.addActionListener(e -> cargarEntrenadores.run());
        especialidadFiltro.addActionListener(e -> cargarEntrenadores.run());

        JTable t =
                tabla(tm);

        t.setRowHeight(45);

        int[] anchosEnt = {
            50, 170, 140, 110, 170, 110, 100, 130
        };

        for (int i = 0; i < anchosEnt.length; i++) {

            t.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(anchosEnt[i]);
        }

        t.getColumnModel()
                .getColumn(1)
                .setCellRenderer(
                        new NombreAvatarRenderer(
                                id -> {
                                    Entrenador en = control.buscarEntrenador(id);
                                    return en == null ? "" : en.getFotoPath();
                                }
                        )
                );

        t.getColumnModel()
                .getColumn(6)
                .setCellRenderer(
                        new StatusRenderer()
                );

        t.getColumnModel()
                .getColumn(7)
                .setCellRenderer(
                        new SimpleActionRenderer()
                );

        t.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            MouseEvent e
                    ) {

                        int fila =
                                t.rowAtPoint(
                                        e.getPoint()
                                );

                        int col =
                                t.columnAtPoint(
                                        e.getPoint()
                                );

                        if (
                                fila < 0
                                || col != 7
                        ) {
                            return;
                        }

                        int id =
                                Integer.parseInt(
                                        t.getValueAt(
                                                fila,
                                                0
                                        ).toString()
                                );

                        Entrenador entrenador =
                                control.buscarEntrenador(
                                        id
                                );

                        if (
                                entrenador
                                == null
                        ) {
                            return;
                        }

                        Rectangle r =
                                t.getCellRect(
                                        fila,
                                        col,
                                        true
                                );

                        int zona =
                                zonaAccion(
                                        e.getX() - r.x,
                                        r.width,
                                        3
                                );

                        if (zona == 0) {

                            dialogoEntrenador(entrenador);

                        } else if (zona == 1) {

                            boolean activo = entrenador.isActivo();

                            String pregunta =
                                    activo
                                            ? "¿Suspender a "
                                              + entrenador.getNombre() + "?"
                                            : "¿Reactivar a "
                                              + entrenador.getNombre() + "?";

                            if (confirm(pregunta)) {

                                entrenador.setActivo(!activo);

                                mostrarEntrenadores();
                            }

                        } else if (
                                confirm(
                                        "¿Eliminar a "
                                        + entrenador.getNombre()
                                        + "?"
                                )
                        ) {

                            control.eliminarEntrenador(id);

                            control.registrarActividad(
                                    "entrenador_eliminado",
                                    "Entrenador eliminado",
                                    entrenador.getNombre()
                            );

                            mostrarEntrenadores();
                        }
                    }
                }
        );

        tablaCard.add(
                tablaSinScroll(t),
                BorderLayout.CENTER
        );

        page.add(tablaCard);

        mostrarContenido(page);
    }

    // =========================================================
    // DIALOGO ENTRENADOR
    // =========================================================

    private void dialogoEntrenador(
            Entrenador entrenador
    ) {

        boolean editar =
                entrenador != null;

        JDialog d =
                new JDialog(
                        this,
                        editar
                                ? "Editar entrenador"
                                : "Nuevo entrenador",
                        true
                );

        d.setSize(
                530,
                540
        );

        d.setLocationRelativeTo(this);

        JPanel p =
                formPanel();

        p.add(
                bigTitle(
                        editar
                                ? "Actualizar entrenador"
                                : "Nuevo entrenador",
                        "DATOS DEL PERSONAL"
                )
        );

        String[] rutaFotoEnt = {
            editar ? entrenador.getFotoPath() : ""
        };

        JPanel fotoPanelEnt = selectorFoto(rutaFotoEnt);

        fotoPanelEnt.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(fotoPanelEnt);

        p.add(Box.createVerticalStrut(10));

        JTextField nombre =
                field();

        JTextField especialidad =
                field();

        JTextField telefono =
                field();

        soloDigitos(telefono, 10);

        JTextField correo =
                field();

        JTextField salario =
                field();

        if (editar) {

            nombre.setText(
                    entrenador.getNombre()
            );

            especialidad.setText(
                    entrenador.getEspecialidad()
            );

            telefono.setText(
                    entrenador.getTelefono()
            );

            correo.setText(
                    entrenador.getCorreo()
            );

            salario.setText(
                    String.valueOf(
                            entrenador.getSalario()
                    )
            );
        }

        addField(
                p,
                "Nombre",
                nombre
        );

        addField(
                p,
                "Especialidad",
                especialidad
        );

        addField(
                p,
                "Teléfono",
                telefono
        );

        addField(
                p,
                "Correo",
                correo
        );

        addField(
                p,
                "Salario",
                salario
        );

        JButton guardar =
                gold(
                        "GUARDAR",
                        Icono.CHECK
                );

        guardar.addActionListener(
                e -> {

                    String n =
                            nombre.getText()
                                    .trim();

                    String esp =
                            especialidad
                                    .getText()
                                    .trim();

                    String tel =
                            telefono.getText()
                                    .trim();

                    String cor =
                            correo.getText()
                                    .trim();

                    String sal =
                            salario.getText()
                                    .trim();

                    if (
                            !RegexControl.nombre(n)
                    ) {

                        mensajeEn(
                                d,
                                "Nombre inválido."
                        );

                        return;
                    }

                    if (
                            esp.length() < 3
                            || esp.length() > 60
                    ) {

                        mensajeEn(
                                d,
                                "Especialidad inválida."
                        );

                        return;
                    }

                    if (
                            !RegexControl.telefono(
                                    tel
                            )
                    ) {

                        mensajeEn(
                                d,
                                "Teléfono inválido."
                        );

                        return;
                    }

                    if (
                            !RegexControl.correo(
                                    cor
                            )
                    ) {

                        mensajeEn(
                                d,
                                "Correo inválido."
                        );

                        return;
                    }

                    if (
                            !RegexControl.dinero(
                                    sal
                            )
                    ) {

                        mensajeEn(
                                d,
                                "Salario inválido."
                        );

                        return;
                    }

                    try {

                        double salarioNumero =
                                Double.parseDouble(
                                        sal
                                );

                        if (editar) {

                            entrenador.setNombre(n);

                            entrenador
                                    .setEspecialidad(
                                            esp
                                    );

                            entrenador
                                    .setTelefono(
                                            tel
                                    );

                            entrenador
                                    .setCorreo(
                                            cor
                                    );

                            entrenador
                                    .setSalario(
                                            salarioNumero
                                    );

                            entrenador.setFotoPath(
                                    rutaFotoEnt[0]
                            );

                            control.actualizarEntrenador(entrenador);

                        } else {

                            Entrenador nuevo =
                                    control.agregarEntrenador(
                                            n,
                                            esp,
                                            tel,
                                            cor,
                                            salarioNumero
                                    );

                            nuevo.setFotoPath(
                                    rutaFotoEnt[0]
                            );
                        }

                        control.registrarActividad(
                                editar ? "entrenador_editado" : "entrenador_nuevo",
                                editar ? "Entrenador actualizado" : "Entrenador agregado",
                                n + " - " + esp
                        );

                        d.dispose();

                        mostrarEntrenadores();

                    } catch (
                            NumberFormatException ex
                    ) {

                        mensajeEn(
                                d,
                                "Salario inválido."
                        );
                    }
                }
        );

        p.add(
                Box.createVerticalStrut(8)
        );

        p.add(guardar);

        d.add(
                new JScrollPane(p)
        );

        d.setVisible(true);
    }

    // =========================================================
    // REPORTES
    // =========================================================

    private void mostrarReportes() {

        seccionVisible = "reportes";

        activarNav(5);

        pageTitle.setText(
                "Reportes"
        );

        JPanel page = base();

        JPanel head =
                moduleHead(
                        "Reportes",
                        "Resumen general de la información registrada.",
                        "Actualizar",
                        Icono.REFRESH,
                        Icono.CHART
                );

        JButton boton =
                (JButton) head.getClientProperty(
                        "actionButton"
                );

        boton.addActionListener(
                e -> mostrarReportes()
        );

        page.add(head);

        page.add(
                Box.createVerticalStrut(18)
        );

        JPanel cards =
                new JPanel(
                        new GridLayout(
                                1,
                                6,
                                12,
                                0
                        )
                );

        cards.setOpaque(false);

        cards.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 100)
        );

        cards.setAlignmentX(Component.LEFT_ALIGNMENT);

        cards.add(
                reportCard(
                        "Socios",
                        String.valueOf(
                                control.totalSocios()
                        ),
                        "Registrados",
                        GOLD,
                        Icono.USER_GROUP
                )
        );

        cards.add(
                reportCard(
                        "Activos",
                        String.valueOf(
                                contarActivos()
                        ),
                        "Socios activos",
                        GREEN,
                        Icono.CHECK_USER
                )
        );

        cards.add(
                reportCard(
                        "Suspendidos",
                        String.valueOf(
                                control.totalSocios()
                                - contarActivos()
                        ),
                        "Socios suspendidos",
                        RED,
                        Icono.BLOCK
                )
        );

        cards.add(
                reportCard(
                        "Entrenadores",
                        String.valueOf(
                                control.totalEntrenadores()
                        ),
                        "Personal",
                        BLUE,
                        Icono.TRAINER
                )
        );

        cards.add(
                reportCard(
                        "Membresías",
                        String.valueOf(
                                control.totalMembresias()
                        ),
                        "Planes disponibles",
                        PURPLE,
                        Icono.CALENDAR
                )
        );

        cards.add(
                reportCard(
                        "Ingresos",
                        String.format(
                                "$ %.2f",
                                control.totalIngresos()
                        ),
                        "Total registrado",
                        GOLD,
                        Icono.MONEY
                )
        );

        page.add(cards);

        page.add(
                Box.createVerticalStrut(18)
        );

        // -----------------------------------------------------
        // FILA: INGRESOS POR MES + DISTRIBUCIÓN POR MEMBRESÍA
        // -----------------------------------------------------

        JPanel filaGraf1 = new JPanel(new GridLayout(1, 2, 16, 0));

        filaGraf1.setOpaque(false);

        filaGraf1.setAlignmentX(Component.LEFT_ALIGNMENT);

        filaGraf1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel ingresosMesCard = card();

        ingresosMesCard.setLayout(new BorderLayout());

        ingresosMesCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        ingresosMesCard.add(
                sectionTitle("Ingresos por Mes", ""),
                BorderLayout.NORTH
        );

        ingresosMesCard.add(new Chart(), BorderLayout.CENTER);

        filaGraf1.add(ingresosMesCard);

        JPanel distMembresiaCard = card();

        distMembresiaCard.setLayout(new BorderLayout(12, 0));

        distMembresiaCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        distMembresiaCard.add(
                sectionTitle("Distribución de Socios por Membresía", ""),
                BorderLayout.NORTH
        );

        java.util.LinkedHashMap<String, Integer> sociosMap = sociosPorMembresia();

        java.util.LinkedHashMap<String, Double> sociosMapD = new java.util.LinkedHashMap<>();

        java.util.LinkedHashMap<String, Color> coloresMembresias = new java.util.LinkedHashMap<>();

        int idxColor = 0;

        for (java.util.Map.Entry<String, Integer> e : sociosMap.entrySet()) {

            sociosMapD.put(e.getKey(), (double) e.getValue());

            coloresMembresias.put(e.getKey(), colorParaMembresia(e.getKey(), idxColor));

            idxColor++;
        }

        JPanel donutWrap = new JPanel(new BorderLayout());

        donutWrap.setOpaque(false);

        RingChart donut = new RingChart(
                sociosMapD,
                coloresMembresias,
                true,
                control.totalSocios() + "\nSocios"
        );

        donut.setPreferredSize(new Dimension(150, 150));

        donutWrap.add(donut, BorderLayout.CENTER);

        distMembresiaCard.add(donutWrap, BorderLayout.CENTER);

        JScrollPane leyendaScroll = new JScrollPane(
                leyenda(sociosMapD, coloresMembresias, true)
        );

        leyendaScroll.setBorder(null);
        leyendaScroll.setOpaque(false);
        leyendaScroll.getViewport().setOpaque(false);
        leyendaScroll.setPreferredSize(new Dimension(170, 100));
        leyendaScroll.getVerticalScrollBar().setUnitIncrement(12);

        distMembresiaCard.add(leyendaScroll, BorderLayout.EAST);

        filaGraf1.add(distMembresiaCard);

        page.add(filaGraf1);

        page.add(Box.createVerticalStrut(16));

        // -----------------------------------------------------
        // FILA: INGRESOS POR TIPO + MÉTODOS DE PAGO + ÚLTIMOS 5 PAGOS
        // -----------------------------------------------------

        JPanel filaGraf2 = new JPanel(new GridLayout(1, 3, 16, 0));

        filaGraf2.setOpaque(false);

        filaGraf2.setAlignmentX(Component.LEFT_ALIGNMENT);

        filaGraf2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Ingresos por tipo de membresía (barras horizontales)

        JPanel ingTipoCard = card();

        ingTipoCard.setLayout(new BorderLayout());

        ingTipoCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        ingTipoCard.add(
                sectionTitle("Ingresos por Tipo de Membresía", ""),
                BorderLayout.NORTH
        );

        java.util.LinkedHashMap<String, Double> ingMemMap = ingresosPorMembresia();

        double maxIngMem = 1;

        for (double v : ingMemMap.values()) {
            maxIngMem = Math.max(maxIngMem, v);
        }

        JPanel barrasWrap = new JPanel();

        barrasWrap.setOpaque(false);

        barrasWrap.setLayout(new BoxLayout(barrasWrap, BoxLayout.Y_AXIS));

        idxColor = 0;

        for (java.util.Map.Entry<String, Double> e : ingMemMap.entrySet()) {

            Color cBarra = colorParaMembresia(e.getKey(), idxColor);

            idxColor++;

            JLabel nom = new JLabel(e.getKey());

            nom.setForeground(TEXT);

            nom.setFont(f("SansSerif", Font.PLAIN, 11));

            nom.setAlignmentX(Component.LEFT_ALIGNMENT);

            barrasWrap.add(nom);

            JPanel filaBarra = new JPanel(new BorderLayout(8, 0));

            filaBarra.setOpaque(false);

            filaBarra.setAlignmentX(Component.LEFT_ALIGNMENT);

            filaBarra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

            double pctW = e.getValue() / maxIngMem;

            final double pctWF = pctW;
            final Color cBarraF = cBarra;

            JPanel barra = new JPanel() {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D x = (Graphics2D) g.create();

                    x.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    x.setColor(new Color(255, 255, 255, 18));

                    x.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                    x.setColor(cBarraF);

                    int w = (int) (getWidth() * pctWF);

                    x.fillRoundRect(0, 0, Math.max(4, w), getHeight(), 6, 6);

                    x.dispose();
                }
            };

            barra.setOpaque(false);

            filaBarra.add(barra, BorderLayout.CENTER);

            JLabel monto = new JLabel(
                    "$" + String.format("%.0f", e.getValue())
            );

            monto.setForeground(MUTED);

            monto.setFont(f("SansSerif", Font.BOLD, 10));

            filaBarra.add(monto, BorderLayout.EAST);

            barrasWrap.add(filaBarra);

            barrasWrap.add(Box.createVerticalStrut(8));
        }

        ingTipoCard.add(barrasWrap, BorderLayout.CENTER);

        filaGraf2.add(ingTipoCard);

        // Métodos de pago (pastel)

        JPanel metodoCard = card();

        metodoCard.setLayout(new BorderLayout(10, 0));

        metodoCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        metodoCard.add(
                sectionTitle("Métodos de Pago", ""),
                BorderLayout.NORTH
        );

        java.util.LinkedHashMap<String, Double> metodoMap = pagosPorMetodo();

        java.util.LinkedHashMap<String, Color> coloresMetodo = new java.util.LinkedHashMap<>();

        coloresMetodo.put("Efectivo", GOLD);
        coloresMetodo.put("Tarjeta", BLUE);
        coloresMetodo.put("Transferencia", PURPLE);

        JPanel pieWrap = new JPanel(new BorderLayout());

        pieWrap.setOpaque(false);

        RingChart pie = new RingChart(
                metodoMap,
                coloresMetodo,
                false,
                null
        );

        pieWrap.add(pie, BorderLayout.CENTER);

        metodoCard.add(pieWrap, BorderLayout.CENTER);

        metodoCard.add(
                leyenda(metodoMap, coloresMetodo, false),
                BorderLayout.SOUTH
        );

        filaGraf2.add(metodoCard);

        // Últimos 5 pagos

        JPanel ultimos5Card = card();

        ultimos5Card.setLayout(new BorderLayout());

        ultimos5Card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel u5Head = new JPanel(new BorderLayout());

        u5Head.setOpaque(false);

        u5Head.add(sectionTitle("Últimos 5 Pagos", ""), BorderLayout.WEST);

        ultimos5Card.add(u5Head, BorderLayout.NORTH);

        String[] colU5 = {"Socio", "Membresía", "Monto", "Fecha"};

        DefaultTableModel modeloU5 = modelo(colU5);

        int inicioU5 = Math.max(0, control.getPagos().size() - 5);

        for (int i = control.getPagos().size() - 1; i >= inicioU5; i--) {

            Pago p = control.getPagos().get(i);

            modeloU5.addRow(
                    new Object[]{
                        p.getSocioNombre(),
                        p.getMembresia(),
                        String.format("$%.2f", p.getMonto()),
                        p.getFecha()
                    }
            );
        }

        JTable tablaU5 = tabla(modeloU5);

        tablaU5.setRowHeight(30);

        tablaU5.getColumnModel()
                .getColumn(1)
                .setCellRenderer(new PillRenderer(coloresMembresias));

        JScrollPane u5Scroll = new JScrollPane(tablaU5);

        u5Scroll.setBorder(null);

        u5Scroll.getViewport().setBackground(PANEL);

        ultimos5Card.add(u5Scroll, BorderLayout.CENTER);

        filaGraf2.add(ultimos5Card);

        page.add(filaGraf2);

        page.add(
                Box.createVerticalStrut(18)
        );

        // -----------------------------------------------------
        // RESUMEN GENERAL + EXPORTAR PDF
        // -----------------------------------------------------

        JPanel resumen = card();

        resumen.setLayout(
                new BorderLayout()
        );

        resumen.setBorder(
                new EmptyBorder(
                        18,
                        20,
                        18,
                        20
                )
        );

        JPanel resumenHead = new JPanel(new BorderLayout());

        resumenHead.setOpaque(false);

        resumenHead.add(
                sectionTitle("Resumen General", ""),
                BorderLayout.WEST
        );

        JButton exportarPdf = gold("Exportar PDF", Icono.SAVE);

        exportarPdf.addActionListener(e -> exportarReportePdf(page));

        JPanel exportarWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        exportarWrap.setOpaque(false);

        exportarWrap.add(exportarPdf);

        resumenHead.add(exportarWrap, BorderLayout.EAST);

        resumen.add(
                resumenHead,
                BorderLayout.NORTH
        );

        String metodoTop = "N/A";

        double metodoTopVal = -1;

        for (java.util.Map.Entry<String, Double> e : metodoMap.entrySet()) {

            if (e.getValue() > metodoTopVal) {
                metodoTopVal = e.getValue();
                metodoTop = e.getKey();
            }
        }

        String planTop = "N/A";

        int planTopVal = -1;

        for (java.util.Map.Entry<String, Integer> e : sociosMap.entrySet()) {

            if (e.getValue() > planTopVal) {
                planTopVal = e.getValue();
                planTop = e.getKey();
            }
        }

        JLabel texto =
                new JLabel(
                        "<html>Se registraron "
                        + control.totalPagos()
                        + " pagos por un total de "
                        + String.format("$ %.2f", control.totalIngresos())
                        + ". El plan más popular es "
                        + planTop
                        + " y el método de pago más utilizado es "
                        + metodoTop
                        + ".</html>"
                );

        texto.setForeground(TEXT);

        texto.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );

        resumen.add(
                texto,
                BorderLayout.CENTER
        );

        page.add(resumen);

        mostrarContenido(page);
    }

    // =========================================================
    // EXPORTAR REPORTE A PDF (impresión del sistema)
    // =========================================================

    private void exportarReportePdf(JPanel contenido) {

        java.awt.print.PrinterJob job =
                java.awt.print.PrinterJob.getPrinterJob();

        job.setJobName("Reporte_GoldenGym");

        job.setPrintable((g, pf, pageIndex) -> {

            if (pageIndex > 0) {
                return java.awt.print.Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) g;

            g2.translate(
                    (int) pf.getImageableX(),
                    (int) pf.getImageableY()
            );

            double escala = Math.min(
                    pf.getImageableWidth() / contenido.getWidth(),
                    pf.getImageableHeight() / contenido.getHeight()
            );

            if (escala < 1) {
                g2.scale(escala, escala);
            }

            contenido.printAll(g2);

            return java.awt.print.Printable.PAGE_EXISTS;
        });

        try {

            if (job.printDialog()) {

                job.print();

                mensaje(
                        "Reporte enviado. Elige \"Guardar como PDF\" "
                        + "en el diálogo de impresión para exportarlo."
                );
            }

        } catch (java.awt.print.PrinterException ex) {

            mensaje("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // =========================================================
    // BASE
    // =========================================================

    /**
     * Panel de página que NUNCA se hace más ancho que el área visible.
     * Sin esto, el contenido se salía por la derecha y las tablas,
     * botones y gráficas quedaban cortados.
     */
    private class Pagina extends JPanel implements javax.swing.Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visible, int orientacion, int direccion) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel base() {

        JPanel page =
                new Pagina();

        page.setBackground(BG);

        page.setBorder(
                new EmptyBorder(
                        22,
                        6,
                        22,
                        14
                )
        );

        page.setLayout(
                new BoxLayout(
                        page,
                        BoxLayout.Y_AXIS
                )
        );

        return page;
    }

    // =========================================================
    // CABECERA DE MÓDULO
    // =========================================================

    private JPanel moduleHead(
            String titulo,
            String subtitulo,
            String botonTexto,
            int icono
    ) {

        return moduleHead(
                titulo,
                subtitulo,
                botonTexto,
                icono,
                -1
        );
    }

    private JPanel moduleHead(
            String titulo,
            String subtitulo,
            String botonTexto,
            int icono,
            int tituloIcono
    ) {

        JPanel head =
                new JPanel(
                        new BorderLayout()
                );

        head.setOpaque(false);

        head.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        head.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        68
                )
        );

        JPanel textos =
                new JPanel();

        textos.setOpaque(false);

        textos.setLayout(
                new BoxLayout(
                        textos,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel t =
                new JLabel(
                        titulo
                );

        t.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        29
                )
        );

        t.setForeground(TEXT);

        // Sin esto, BoxLayout centra el título sobre el subtítulo
        // y se ve desalineado cuando el título es corto (ej. "Socios").
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s =
                new JLabel(
                        subtitulo
                );

        s.setForeground(MUTED);

        s.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        textos.add(t);

        textos.add(
                Box.createVerticalStrut(4)
        );

        textos.add(s);

        JButton boton =
                gold(
                        botonTexto,
                        icono
                );

        boton.setPreferredSize(
                new Dimension(
                        165,
                        40
                )
        );

        head.putClientProperty(
                "actionButton",
                boton
        );

        if (tituloIcono >= 0) {

            JPanel izq =
                    new JPanel(new BorderLayout(14, 0));

            izq.setOpaque(false);

            JLabel iconoTitulo =
                    new JLabel(new Icono(tituloIcono, GOLD, 34));

            iconoTitulo.setVerticalAlignment(SwingConstants.TOP);

            iconoTitulo.setBorder(new EmptyBorder(4, 0, 0, 0));

            izq.add(iconoTitulo, BorderLayout.WEST);

            izq.add(textos, BorderLayout.CENTER);

            head.add(
                    izq,
                    BorderLayout.WEST
            );

        } else {

            head.add(
                    textos,
                    BorderLayout.WEST
            );
        }

        head.add(
                boton,
                BorderLayout.EAST
        );

        return head;
    }

    // =========================================================
    // CARD
    // =========================================================

    private JPanel card() {

        JPanel p =
                new JPanel();

        p.setBackground(PANEL);

        p.setBorder(
                new LineBorder(
                        BORDER,
                        1
                )
        );

        // Antes card() no fijaba esto, así que cuando se agregaba
        // directo a una página (BoxLayout Y_AXIS) — como las tablas
        // de Socios, Pagos, Entrenadores, o la fila de tarjetas de
        // Pagos — quedaba con su ancho natural (más angosto que la
        // página) y BoxLayout la CENTRABA, dejando huecos a los dos
        // lados en vez de pegarla a la izquierda y ocupar todo el
        // ancho. Dentro de un GridLayout (filas de tarjetas) esto
        // no cambia nada, porque GridLayout ignora alignmentX y
        // maximumSize por completo.
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );

        return p;
    }

    // =========================================================
    // SMALL STAT
    // =========================================================

    private JPanel smallStat(
            String titulo,
            Object valor,
            Color color,
            int icono
    ) {

        JPanel p = card();

        p.setLayout(
                new BorderLayout(
                        12,
                        0
                )
        );

        p.setBorder(
                new EmptyBorder(
                        12,
                        15,
                        12,
                        15
                )
        );

        p.add(
                new JLabel(
                        new Icono(
                                icono,
                                color,
                                28
                        )
                ),
                BorderLayout.WEST
        );

        JPanel texto =
                new JPanel();

        texto.setOpaque(false);

        texto.setLayout(
                new BoxLayout(
                        texto,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel t =
                new JLabel(
                        titulo
                );

        t.setForeground(MUTED);

        t.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        JLabel v =
                new JLabel(
                        String.valueOf(valor)
                );

        v.setForeground(TEXT);

        v.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        texto.add(t);
        texto.add(v);

        p.add(
                texto,
                BorderLayout.CENTER
        );

        return p;
    }

    // =========================================================
    // REPORT CARD
    // =========================================================

    private JPanel reportCard(
            String titulo,
            String valor,
            String sub,
            Color color,
            int icono
    ) {

        JPanel p = card();

        p.setBorder(
                new EmptyBorder(
                        12,
                        12,
                        12,
                        12
                )
        );

        p.setLayout(
                new BorderLayout(
                        10,
                        0
                )
        );

        p.add(
                new JLabel(
                        new Icono(
                                icono,
                                color,
                                26
                        )
                ),
                BorderLayout.WEST
        );

        JPanel text =
                new JPanel();

        text.setOpaque(false);

        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel t =
                new JLabel(
                        titulo
                );

        t.setForeground(MUTED);

        t.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        JLabel v =
                new JLabel(
                        valor
                );

        v.setForeground(TEXT);

        v.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        25
                )
        );

        JLabel s =
                new JLabel(
                        sub
                );

        s.setForeground(MUTED);

        s.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        text.add(t);
        text.add(v);
        text.add(s);

        p.add(
                text,
                BorderLayout.CENTER
        );

        return p;
    }

    // =========================================================
    // TITULO DE SECCIÓN
    // =========================================================

    private JLabel sectionTitle(
            String titulo,
            String sub
    ) {

        JLabel l =
                new JLabel(
                        titulo
                );

        l.setForeground(TEXT);

        l.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        16
                )
        );

        return l;
    }

    // =========================================================
    // BOTÓN GOLD
    // =========================================================

    private JButton gold(
            String texto,
            int icono
    ) {

        JButton b =
                new JButton(
                        texto,
                        new Icono(
                                icono,
                                Color.BLACK,
                                16
                        )
                );

        b.setBackground(GOLD);
        b.setForeground(Color.BLACK);

        b.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        b.setFocusPainted(false);

        b.setBorder(
                new EmptyBorder(
                        9,
                        14,
                        9,
                        14
                )
        );

        b.setIconTextGap(7);

        b.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        return b;
    }

    // =========================================================
    // BOTÓN OSCURO
    // =========================================================

    private JButton dark(
            String texto
    ) {

        JButton b =
                new JButton(
                        texto
                );

        b.setBackground(PANEL2);

        b.setForeground(TEXT);

        b.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        10
                )
        );

        b.setFocusPainted(false);

        b.setBorder(
                new LineBorder(
                        BORDER
                )
        );

        return b;
    }

    private JButton dark(
            String texto,
            int icono
    ) {

        JButton b =
                new JButton(
                        texto,
                        new Icono(
                                icono,
                                TEXT,
                                15
                        )
                );

        b.setBackground(PANEL2);

        b.setForeground(TEXT);

        b.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        b.setFocusPainted(false);

        b.setBorder(
                new EmptyBorder(
                        8,
                        12,
                        8,
                        12
                )
        );

        b.setIconTextGap(7);

        b.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        return b;
    }

    // =========================================================
    // CAMPO
    // =========================================================

    private JTextField field() {

        JTextField f =
                new JTextField();

        f.setBackground(INPUT);

        f.setForeground(TEXT);

        f.setCaretColor(GOLD);

        f.setBorder(
                new LineBorder(
                        BORDER
                )
        );

        f.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        f.setPreferredSize(
                new Dimension(
                        250,
                        32
                )
        );

        f.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        32
                )
        );

        return f;
    }

    // =========================================================
    // LIMITAR CAMPO A SOLO DÍGITOS (con máximo de caracteres)
    // =========================================================

    private void soloDigitos(JTextField campo, int maxLen) {

        ((javax.swing.text.AbstractDocument) campo.getDocument())
                .setDocumentFilter(
                        new javax.swing.text.DocumentFilter() {

                            @Override
                            public void insertString(
                                    FilterBypass fb,
                                    int offset,
                                    String string,
                                    javax.swing.text.AttributeSet attr
                            ) throws javax.swing.text.BadLocationException {

                                String filtrado =
                                        string.replaceAll("[^0-9]", "");

                                if (fb.getDocument().getLength()
                                        + filtrado.length() > maxLen) {

                                    filtrado = filtrado.substring(
                                            0,
                                            Math.max(
                                                    0,
                                                    maxLen - fb.getDocument().getLength()
                                            )
                                    );
                                }

                                super.insertString(fb, offset, filtrado, attr);
                            }

                            @Override
                            public void replace(
                                    FilterBypass fb,
                                    int offset,
                                    int length,
                                    String text,
                                    javax.swing.text.AttributeSet attrs
                            ) throws javax.swing.text.BadLocationException {

                                String filtrado =
                                        text.replaceAll("[^0-9]", "");

                                int largoFinal =
                                        fb.getDocument().getLength()
                                        - length
                                        + filtrado.length();

                                if (largoFinal > maxLen) {

                                    int permitido =
                                            maxLen
                                            - (fb.getDocument().getLength() - length);

                                    filtrado = filtrado.substring(
                                            0,
                                            Math.max(0, Math.min(permitido, filtrado.length()))
                                    );
                                }

                                super.replace(fb, offset, length, filtrado, attrs);
                            }
                        }
                );
    }

    // =========================================================
    // PANEL FORMULARIO
    // =========================================================

    private JPanel formPanel() {

        JPanel p =
                new JPanel();

        p.setBackground(PANEL);

        p.setBorder(
                new EmptyBorder(
                        20,
                        22,
                        20,
                        22
                )
        );

        p.setLayout(
                new BoxLayout(
                        p,
                        BoxLayout.Y_AXIS
                )
        );

        return p;
    }

    // =========================================================
    // TITULO GRANDE
    // =========================================================

    private JLabel bigTitle(
            String titulo,
            String subtitulo
    ) {

        JLabel l =
                new JLabel(
                        "<html>"
                        + "<font size='6'>"
                        + titulo
                        + "</font>"
                        + "<br>"
                        + "<font color='#A4B1BF'>"
                        + subtitulo
                        + "</font>"
                        + "</html>"
                );

        l.setForeground(TEXT);

        l.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        15,
                        0
                )
        );

        return l;
    }

    // =========================================================
    // AGREGAR CAMPO
    // =========================================================

    private void addField(
            JPanel panel,
            String nombre,
            JComponent componente
    ) {

        JLabel l =
                new JLabel(
                        nombre
                );

        l.setForeground(TEXT);

        l.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        11
                )
        );

        l.setBorder(
                new EmptyBorder(
                        3,
                        0,
                        3,
                        0
                )
        );

        panel.add(l);

        panel.add(componente);

        panel.add(
                Box.createVerticalStrut(5)
        );
    }

    // =========================================================
    // MODELO TABLA
    // =========================================================

    private DefaultTableModel modelo(
            String[] columnas
    ) {

        return new DefaultTableModel(
                columnas,
                0
        ) {

            @Override
            public boolean isCellEditable(
                    int fila,
                    int columna
            ) {

                return false;
            }
        };
    }

    // =========================================================
    // TABLA
    // =========================================================

    private JTable tabla(
            DefaultTableModel modelo
    ) {

        JTable t =
                new JTable(
                        modelo
                );

        t.setRowHeight(39);

        t.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        t.setForeground(TEXT);
        t.setBackground(PANEL);

        t.setGridColor(BORDER);

        t.setSelectionBackground(
                new Color(
                        42,
                        58,
                        76
                )
        );

        t.setSelectionForeground(TEXT);

        t.setShowVerticalLines(false);

        t.setIntercellSpacing(
                new Dimension(
                        0,
                        1
                )
        );

        t.setAutoResizeMode(
                JTable.AUTO_RESIZE_ALL_COLUMNS
        );

        t.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                0,
                                40
                        )
                );

        t.getTableHeader()
                .setFont(
                        f(
                                "SansSerif",
                                Font.BOLD,
                                10
                        )
                );

        t.getTableHeader()
                .setForeground(
                        MUTED
                );

        t.getTableHeader()
                .setBackground(
                        PANEL
                );

        t.getTableHeader()
                .setOpaque(true);

        t.getTableHeader()
                .setBorder(
                        new javax.swing.border.MatteBorder(
                                0, 0, 1, 0,
                                BORDER
                        )
                );

        t.setOpaque(true);

        t.getTableHeader()
                .setReorderingAllowed(false);

        return t;
    }

    // =========================================================
    // MOSTRAR CONTENIDO
    // =========================================================

    // =========================================================
    // TABLA SIN SCROLL PROPIO (usa el scroll de la página)
    // =========================================================

    // =========================================================
    // FOTO DE PERFIL (CARGA, RECORTE CIRCULAR Y CACHÉ)
    // =========================================================

    private ImageIcon fotoCircular(String path, int size) {

        String key = (path == null ? "" : path) + "_" + size;

        if (fotoCache.containsKey(key)) {

            return fotoCache.get(key);
        }

        BufferedImage base = null;

        if (path != null && !path.isEmpty()) {

            File f = new File(path);

            if (f.exists()) {

                try {

                    base = ImageIO.read(f);

                } catch (IOException ex) {

                    base = null;
                }
            }
        }

        BufferedImage circular =
                new BufferedImage(
                        size, size,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D g2 = circular.createGraphics();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setClip(new Ellipse2D.Float(0, 0, size, size));

        if (base != null) {

            g2.drawImage(base, 0, 0, size, size, null);

        } else {

            g2.setColor(PANEL2);

            g2.fillOval(0, 0, size, size);

            new Icono(Icono.USER_CIRCLE, MUTED, size)
                    .paintIcon(null, g2, 0, 0);
        }

        g2.dispose();

        ImageIcon icon = new ImageIcon(circular);

        fotoCache.put(key, icon);

        return icon;
    }

    /**
     * Crea un selector de foto (circular + botón) para usar en un
     * diálogo. rutaActual[0] guarda la ruta seleccionada; al abrir
     * el diálogo en modo editar, pásale la ruta ya guardada.
     */
    private JPanel selectorFoto(String[] rutaActual) {

        JPanel wrap = new JPanel();

        wrap.setOpaque(false);

        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel preview =
                new JLabel(
                        fotoCircular(rutaActual[0], 90)
                );

        preview.setAlignmentX(Component.CENTER_ALIGNMENT);

        wrap.add(preview);

        wrap.add(Box.createVerticalStrut(8));

        JButton subir = dark("Subir foto", Icono.USER_CIRCLE);

        subir.setAlignmentX(Component.CENTER_ALIGNMENT);

        subir.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Seleccionar foto de perfil");

            chooser.setFileFilter(
                    new javax.swing.filechooser.FileNameExtensionFilter(
                            "Imágenes (jpg, png, jpeg)",
                            "jpg", "jpeg", "png"
                    )
            );

            int r = chooser.showOpenDialog(this);

            if (r == JFileChooser.APPROVE_OPTION) {

                try {

                    File origen = chooser.getSelectedFile();

                    File carpeta = new File("fotos");

                    if (!carpeta.exists()) {

                        carpeta.mkdirs();
                    }

                    String ext = origen.getName().contains(".")
                            ? origen.getName().substring(
                                    origen.getName().lastIndexOf(".")
                            )
                            : ".png";

                    File destino = new File(
                            carpeta,
                            "foto_" + System.currentTimeMillis() + ext
                    );

                    java.nio.file.Files.copy(
                            origen.toPath(),
                            destino.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );

                    rutaActual[0] = destino.getPath();

                    preview.setIcon(
                            fotoCircular(rutaActual[0], 90)
                    );

                } catch (IOException ex) {

                    mensaje("No se pudo cargar la imagen: " + ex.getMessage());
                }
            }
        });

        wrap.add(subir);

        return wrap;
    }

    private JPanel tablaSinScroll(JTable t) {

        JPanel wrap = new JPanel(new BorderLayout());

        wrap.setOpaque(true);

        wrap.setBackground(PANEL);

        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Antes, al poner el cursor sobre esta tabla y usar la rueda
        // del mouse, el scroll de la página completa se quedaba
        // "atorado" (la tabla capturaba el evento de rueda para su
        // propio JViewport interno en vez de dejarlo subir al
        // JScrollPane de la página). Con esto, cualquier evento de
        // rueda sobre la tabla se reenvía al JScrollPane más cercano
        // que la contiene, así la página siempre se desplaza.

        t.addMouseWheelListener(e -> {

            java.awt.Container padre =
                    javax.swing.SwingUtilities.getAncestorOfClass(
                            JScrollPane.class,
                            t
                    );

            if (padre instanceof JScrollPane) {

                JScrollPane sp = (JScrollPane) padre;

                java.awt.event.MouseWheelEvent reenviado =
                        (java.awt.event.MouseWheelEvent)
                                javax.swing.SwingUtilities.convertMouseEvent(
                                        t,
                                        e,
                                        sp
                                );

                sp.dispatchEvent(reenviado);
            }
        });

        wrap.add(t.getTableHeader(), BorderLayout.NORTH);

        wrap.add(t, BorderLayout.CENTER);

        return wrap;
    }

    private void mostrarContenido(
            JPanel panel
    ) {

        content.removeAll();

        content.setLayout(
                new BorderLayout()
        );

        JScrollPane scroll =
                new JScrollPane(
                        panel,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        scroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        scroll.setBorder(null);

        scroll.getViewport()
                .setBackground(BG);

        content.add(
                scroll,
                BorderLayout.CENTER
        );

        content.revalidate();
        content.repaint();

        animarTransicion();
    }

    // =========================================================
    // CONFIRMAR
    // =========================================================

    private boolean confirm(
            String texto
    ) {

        return JOptionPane.showConfirmDialog(
                this,
                texto,
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }

    // =========================================================
    // ACTUALIZAR NOMBRE DEL GIMNASIO (SIDEBAR)
    // =========================================================

    private void actualizarNombreGimnasio() {

        String nombre = cfgNombreGym.trim();

        if (nombre.isEmpty()) {
            return;
        }

        int ultimoEspacio = nombre.lastIndexOf(' ');

        if (ultimoEspacio == -1) {

            sidebarTitulo1.setText("");
            sidebarTitulo2.setText(nombre.toUpperCase());

        } else {

            sidebarTitulo1.setText(
                    nombre.substring(0, ultimoEspacio + 1).toUpperCase()
            );

            sidebarTitulo2.setText(
                    nombre.substring(ultimoEspacio + 1).toUpperCase()
            );
        }
    }

    // =========================================================
    // MENSAJE
    // =========================================================

    private void mensaje(
            String texto
    ) {

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Validación",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mensajeEn(
            Component padre,
            String texto
    ) {

        JOptionPane.showMessageDialog(
                padre,
                texto,
                "Validación",
                JOptionPane.WARNING_MESSAGE
        );
    }

    // =========================================================
    // CALENDARIO
    // =========================================================

    // =========================================================
    // SELECTOR DE FECHA CON JCALENDAR (librería externa)
    // =========================================================

    private void mostrarJCalendarPopup(
            JTextField campo
    ) {

        SimpleDateFormat fmt =
                new SimpleDateFormat("dd/MM/yyyy");

        JCalendar jcal = new JCalendar();

        jcal.setWeekOfYearVisible(false);

        try {

            jcal.setDate(
                    fmt.parse(campo.getText())
            );

        } catch (Exception ignored) {

            jcal.setDate(new Date());
        }

        jcal.setBackground(PANEL);

        int r =
                JOptionPane.showConfirmDialog(
                        this,
                        jcal,
                        "Seleccionar fecha",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (r == JOptionPane.OK_OPTION) {

            campo.setText(
                    fmt.format(
                            jcal.getDate()
                    )
            );
        }
    }

    private void fechaPopup(
            JTextField campo
    ) {

        Date inicial = new Date();

        try {

            inicial =
                    new SimpleDateFormat(
                            "dd/MM/yyyy"
                    ).parse(
                            campo.getText()
                    );

        } catch (Exception ignored) {
        }

        java.util.Calendar calVista = java.util.Calendar.getInstance();
        calVista.setTime(inicial);
        calVista.set(java.util.Calendar.DAY_OF_MONTH, 1);

        java.util.Calendar calSel = java.util.Calendar.getInstance();
        calSel.setTime(inicial);

        JDialog d = new JDialog(this, "Seleccionar fecha", true);

        d.setSize(300, 330);
        d.setLocationRelativeTo(this);
        d.setResizable(false);

        JPanel raiz = new JPanel(new BorderLayout());

        raiz.setBackground(PANEL);

        raiz.setBorder(new LineBorder(BORDER));

        // ---- Cabecera: mes/año + flechas ----

        JPanel cab = new JPanel(new BorderLayout());

        cab.setBackground(PANEL2);

        cab.setBorder(new EmptyBorder(10, 12, 10, 12));

        JButton izq = new JButton("‹");

        izq.setForeground(TEXT);
        izq.setFont(f("SansSerif", Font.BOLD, 16));
        izq.setBorder(null);
        izq.setContentAreaFilled(false);
        izq.setFocusPainted(false);
        izq.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton der = new JButton("›");

        der.setForeground(TEXT);
        der.setFont(f("SansSerif", Font.BOLD, 16));
        der.setBorder(null);
        der.setContentAreaFilled(false);
        der.setFocusPainted(false);
        der.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel mesLabel = new JLabel("", SwingConstants.CENTER);

        mesLabel.setForeground(GOLD);

        mesLabel.setFont(f("SansSerif", Font.BOLD, 14));

        cab.add(izq, BorderLayout.WEST);
        cab.add(mesLabel, BorderLayout.CENTER);
        cab.add(der, BorderLayout.EAST);

        raiz.add(cab, BorderLayout.NORTH);

        // ---- Grilla de días ----

        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));

        grid.setBackground(PANEL);

        grid.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] dow = {"D", "L", "M", "M", "J", "V", "S"};

        for (String s : dow) {

            JLabel l = new JLabel(s, SwingConstants.CENTER);

            l.setForeground(MUTED);

            l.setFont(f("SansSerif", Font.BOLD, 10));

            grid.add(l);
        }

        JPanel diasWrap = new JPanel(new GridLayout(6, 7, 2, 2));

        diasWrap.setBackground(PANEL);

        SimpleDateFormat fmtSalida = new SimpleDateFormat("dd/MM/yyyy");

        Runnable[] pintarRef = new Runnable[1];

        pintarRef[0] = () -> {

            diasWrap.removeAll();

            java.util.Calendar tmp = (java.util.Calendar) calVista.clone();

            mesLabel.setText(
                    new SimpleDateFormat("MMMM yyyy").format(tmp.getTime())
                            .toUpperCase()
            );

            int primerDia = tmp.get(java.util.Calendar.DAY_OF_WEEK) - 1;
            int diasEnMes = tmp.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

            for (int i = 0; i < primerDia; i++) {

                diasWrap.add(new JLabel(""));
            }

            for (int dia = 1; dia <= diasEnMes; dia++) {

                final int diaF = dia;

                boolean esSel =
                        calSel.get(java.util.Calendar.YEAR) == tmp.get(java.util.Calendar.YEAR)
                        && calSel.get(java.util.Calendar.MONTH) == tmp.get(java.util.Calendar.MONTH)
                        && calSel.get(java.util.Calendar.DAY_OF_MONTH) == dia;

                JButton b = new JButton(String.valueOf(dia));

                b.setFocusPainted(false);
                b.setFont(f("SansSerif", Font.PLAIN, 11));
                b.setCursor(new Cursor(Cursor.HAND_CURSOR));

                if (esSel) {

                    b.setBackground(GOLD);
                    b.setForeground(Color.BLACK);
                    b.setBorder(null);

                } else {

                    b.setBackground(PANEL2);
                    b.setForeground(TEXT);
                    b.setBorder(new LineBorder(BORDER));
                }

                b.addActionListener(ev -> {

                    calSel.setTime(calVista.getTime());
                    calSel.set(java.util.Calendar.DAY_OF_MONTH, diaF);

                    campo.setText(fmtSalida.format(calSel.getTime()));

                    d.dispose();
                });

                diasWrap.add(b);
            }

            diasWrap.revalidate();
            diasWrap.repaint();
        };

        izq.addActionListener(e -> {

            calVista.add(java.util.Calendar.MONTH, -1);
            pintarRef[0].run();
        });

        der.addActionListener(e -> {

            calVista.add(java.util.Calendar.MONTH, 1);
            pintarRef[0].run();
        });

        pintarRef[0].run();

        grid.add(diasWrap);

        raiz.add(grid, BorderLayout.CENTER);

        // ---- Pie: hoy / cerrar ----

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        pie.setBackground(PANEL);

        JButton hoy = dark("Hoy", Icono.CALENDAR);

        hoy.addActionListener(e -> {

            campo.setText(fmtSalida.format(new Date()));

            d.dispose();
        });

        pie.add(hoy);

        raiz.add(pie, BorderLayout.SOUTH);

        d.setContentPane(raiz);

        d.setVisible(true);
    }

    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    private void cerrarSesion() {

        int r =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas cerrar la sesión?",
                        "Cerrar sesión",
                        JOptionPane.YES_NO_OPTION
                );

        if (
                r == JOptionPane.YES_OPTION
        ) {

            dispose();

            new Login().setVisible(true);
        }
    }

    // =========================================================
    // RENDERER ESTADO
    // =========================================================

    // =========================================================
    // RENDERER NOMBRE CON AVATAR
    // =========================================================

    private class NombreAvatarRenderer
            extends DefaultTableCellRenderer {

        private final java.util.function.Function<Integer, String> fotoLookup;

        NombreAvatarRenderer() {

            this.fotoLookup = null;
        }

        NombreAvatarRenderer(
                java.util.function.Function<Integer, String> fotoLookup
        ) {

            this.fotoLookup = fotoLookup;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JLabel l =
                    (JLabel)
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            selected,
                            focus,
                            row,
                            column
                    );

            Icon icono = new Icono(Icono.USER_CIRCLE, MUTED, 26);

            if (fotoLookup != null) {

                try {

                    int id = Integer.parseInt(
                            String.valueOf(table.getValueAt(row, 0))
                    );

                    String ruta = fotoLookup.apply(id);

                    if (ruta != null && !ruta.isEmpty()) {

                        icono = fotoCircular(ruta, 26);
                    }

                } catch (Exception ignored) {
                }
            }

            l.setIcon(icono);

            l.setIconTextGap(10);

            l.setForeground(TEXT);

            l.setFont(
                    f(
                            "SansSerif",
                            Font.PLAIN,
                            12
                    )
            );

            l.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            l.setBorder(
                    new EmptyBorder(0, 12, 0, 0)
            );

            return l;
        }
    }

    // =========================================================
    // INSIGNIA DE COLOR (PILL) PARA MEMBRESÍA / MÉTODO
    // =========================================================

    private class PillRenderer
            extends DefaultTableCellRenderer {

        private final java.util.Map<String, Color> colores;

        PillRenderer(java.util.Map<String, Color> colores) {

            this.colores = colores;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JLabel l = new JLabel(String.valueOf(value));

            l.setOpaque(false);

            l.setHorizontalAlignment(SwingConstants.CENTER);

            l.setFont(
                    f("SansSerif", Font.BOLD, 10)
            );

            Color bg = colores.get(String.valueOf(value));

            if (bg == null) {

                bg = MUTED;
            }

            final Color fondo = bg;

            final Color texto =
                    (fondo.equals(GOLD) || fondo.equals(GREEN))
                            ? Color.BLACK
                            : Color.WHITE;

            l.setForeground(texto);

            l.setBorder(
                    new EmptyBorder(4, 4, 4, 4)
            );

            JPanel wrap = new JPanel(new BorderLayout()) {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D x = (Graphics2D) g.create();

                    x.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    x.setColor(fondo);

                    int pw = Math.min(getWidth() - 10, 100);

                    x.fillRoundRect(
                            (getWidth() - pw) / 2,
                            (getHeight() - 20) / 2,
                            pw, 20,
                            10, 10
                    );

                    x.dispose();
                }
            };

            wrap.setOpaque(false);

            wrap.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            wrap.add(l, BorderLayout.CENTER);

            return wrap;
        }
    }

    private class StatusRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JLabel l =
                    (JLabel)
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            selected,
                            focus,
                            row,
                            column
                    );

            l.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            l.setFont(
                    f(
                            "SansSerif",
                            Font.BOLD,
                            10
                    )
            );

            l.setForeground(
                    "Activo".equals(value)
                            ? GREEN
                            : RED
            );

            l.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            return l;
        }
    }

    // =========================================================
    // CUADRO DE ACCIÓN (ÍCONO COLOREADO)
    // =========================================================

    /**
     * Calcula sobre qué botón de la columna ACCIONES se hizo clic.
     * Los renderers usan FlowLayout.CENTER con botones de 24 px y
     * separación de 4 px, así que la posición es exacta.
     */
    /** true si la membresía del socio ya venció o vence en 7 días o menos. */
    private boolean socioNecesitaRenovar(Socio s) {

        try {

            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("dd/MM/yyyy");

            java.util.Date fv = fmt.parse(s.getFechaVencimiento());

            java.util.Calendar limite = java.util.Calendar.getInstance();
            limite.add(java.util.Calendar.DAY_OF_MONTH, 7);

            return fv.before(limite.getTime());

        } catch (Exception ex) {

            return false;
        }
    }

    private int zonaAccion(
            int xRelativo,
            int anchoCelda,
            int cantidadBotones
    ) {

        int hgap = 4;
        int lado = 24;

        int anchoContenido =
                cantidadBotones * lado + (cantidadBotones + 1) * hgap;

        int inicio =
                Math.max(0, (anchoCelda - anchoContenido) / 2) + hgap;

        int indice = (xRelativo - inicio) / (lado + hgap);

        if (indice < 0) {
            return 0;
        }

        if (indice >= cantidadBotones) {
            return cantidadBotones - 1;
        }

        return indice;
    }

    private JPanel actionSquare(
            int icono,
            Color bg
    ) {

        JPanel p = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D x = (Graphics2D) g.create();

                x.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                x.setColor(bg);

                x.fillRoundRect(
                        0, 0,
                        getWidth(), getHeight(),
                        8, 8
                );

                x.dispose();
            }
        };

        p.setOpaque(false);

        p.setPreferredSize(new Dimension(24, 24));

        JLabel ic = new JLabel(
                new Icono(icono, Color.WHITE, 14)
        );

        ic.setHorizontalAlignment(SwingConstants.CENTER);

        p.add(ic, BorderLayout.CENTER);

        return p;
    }

    // =========================================================
    // RENDERER ACCIONES SOCIOS
    // =========================================================

    private class ActionRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JPanel p =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.CENTER,
                                    4,
                                    4
                            )
                    );

            p.setOpaque(true);

            p.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            int idFila =
                    Integer.parseInt(
                            table.getValueAt(row, 0).toString()
                    );

            Socio socioFila = control.buscarSocio(idFila);

            boolean necesitaRenovar =
                    socioFila != null
                    && socioNecesitaRenovar(socioFila);

            if (necesitaRenovar) {

                JPanel btnRenovar = actionSquare(Icono.REFRESH, GOLD);
                btnRenovar.setToolTipText("Renovar membresía");
                p.add(btnRenovar);
            }

            JPanel btnEditar = actionSquare(Icono.PENCIL, BLUE);
            btnEditar.setToolTipText("Editar");
            p.add(btnEditar);

            JPanel btnVer = actionSquare(Icono.EYE, BLUE);
            btnVer.setToolTipText("Ver perfil");
            p.add(btnVer);

            boolean activo =
                    "Activo".equals(
                            table.getValueAt(row, 4)
                    );

            JPanel btnEstado =
                    actionSquare(
                            activo ? Icono.BLOCK : Icono.REFRESH,
                            activo ? new Color(230, 126, 34) : GREEN
                    );

            btnEstado.setToolTipText(
                    activo ? "Suspender" : "Reactivar"
            );

            p.add(btnEstado);

            JPanel btnEliminar = actionSquare(Icono.TRASH, RED);
            btnEliminar.setToolTipText("Eliminar");
            p.add(btnEliminar);

            return p;
        }
    }

    // =========================================================
    // RENDERER ACCIONES ENTRENADORES
    // =========================================================

    // =========================================================
    // RENDERER ACCIONES PAGOS (VER / EDITAR / ELIMINAR)
    // =========================================================

    private class PagoActionRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JPanel p =
                    new JPanel(
                            new FlowLayout(FlowLayout.CENTER, 4, 4)
                    );

            p.setOpaque(true);

            p.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            JPanel btnVer = actionSquare(Icono.EYE, BLUE);
            btnVer.setToolTipText("Ver detalle");
            p.add(btnVer);

            JPanel btnEditar = actionSquare(Icono.PENCIL, GOLD);
            btnEditar.setToolTipText("Editar");
            p.add(btnEditar);

            JPanel btnEliminar = actionSquare(Icono.TRASH, RED);
            btnEliminar.setToolTipText("Eliminar");
            p.add(btnEliminar);

            return p;
        }
    }

    private class SimpleActionRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focus,
                int row,
                int column
        ) {

            JPanel p =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.CENTER,
                                    4,
                                    4
                            )
                    );

            p.setOpaque(true);

            p.setBackground(
                    selected
                            ? table.getSelectionBackground()
                            : PANEL
            );

            boolean activo =
                    "Activo".equals(
                            table.getValueAt(row, 6)
                    );

            JPanel btnEditar = actionSquare(Icono.PENCIL, BLUE);
            btnEditar.setToolTipText("Editar");
            p.add(btnEditar);

            JPanel btnEstado =
                    actionSquare(
                            activo ? Icono.BLOCK : Icono.REFRESH,
                            activo ? new Color(230, 126, 34) : GREEN
                    );

            btnEstado.setToolTipText(
                    activo ? "Suspender" : "Reactivar"
            );

            p.add(btnEstado);

            JPanel btnEliminar = actionSquare(Icono.TRASH, RED);
            btnEliminar.setToolTipText("Eliminar");
            p.add(btnEliminar);

            return p;
        }
    }

    private JLabel actionLabel(
            String texto,
            Color color
    ) {

        JLabel l =
                new JLabel(
                        texto
                );

        l.setForeground(color);

        l.setFont(
                f(
                        "SansSerif",
                        Font.BOLD,
                        9
                )
        );

        return l;
    }

    // =========================================================
    // TOGGLE (INTERRUPTOR)
    // =========================================================

    private class Toggle
            extends JPanel {

        private boolean on;

        Toggle(boolean inicial) {

            this.on = inicial;

            setOpaque(false);

            setPreferredSize(new Dimension(42, 22));
            setMaximumSize(new Dimension(42, 22));
            setMinimumSize(new Dimension(42, 22));

            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(
                    new MouseAdapter() {

                        @Override
                        public void mouseClicked(MouseEvent e) {

                            on = !on;

                            repaint();
                        }
                    }
            );
        }

        boolean isOn() {

            return on;
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D x = (Graphics2D) g.create();

            x.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = 40;
            int h = 20;

            x.setColor(on ? GOLD : PANEL2);

            x.fillRoundRect(0, 0, w, h, h, h);

            if (!on) {

                x.setColor(BORDER);
                x.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            }

            x.setColor(on ? Color.BLACK : MUTED);

            int kx = on ? (w - h + 2) : 2;

            x.fillOval(kx, 2, h - 4, h - 4);

            x.dispose();
        }
    }

    // =========================================================
    // COMBO ESTILIZADO
    // =========================================================

    private JComboBox<String> combo(String[] opciones) {

        JComboBox<String> c = new JComboBox<>(opciones);

        c.setBackground(INPUT);
        c.setForeground(TEXT);

        c.setFont(
                f(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        c.setBorder(
                new LineBorder(BORDER)
        );

        c.setFocusable(false);

        c.setCursor(new Cursor(Cursor.HAND_CURSOR));

        c.setPreferredSize(new Dimension(200, 32));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        return c;
    }

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    private JPanel configCardWrap(String titulo, int icono) {

        JPanel p = card();

        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.setBorder(
                new EmptyBorder(18, 20, 20, 20)
        );

        p.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));

        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        head.add(new JLabel(new Icono(icono, GOLD, 19)));

        JLabel t = new JLabel(titulo);

        t.setForeground(TEXT);

        t.setFont(
                f("SansSerif", Font.BOLD, 15)
        );

        head.add(t);

        p.add(head);

        p.add(Box.createVerticalStrut(16));

        return p;
    }

    private JPanel configRow(String etiqueta, JComponent campo) {

        JPanel row = new JPanel(new BorderLayout());

        row.setOpaque(false);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel l = new JLabel(etiqueta);

        l.setForeground(MUTED);

        l.setFont(
                f("SansSerif", Font.PLAIN, 12)
        );

        row.add(l, BorderLayout.WEST);
        row.add(campo, BorderLayout.EAST);

        return row;
    }

    private JPanel configToggleRow(String titulo, String desc, boolean inicial) {

        return configToggleRow(titulo, desc, new Toggle(inicial));
    }

    private JPanel configToggleRow(String titulo, String desc, Toggle toggle) {

        JPanel row = new JPanel(new BorderLayout(10, 0));

        row.setOpaque(false);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel textos = new JPanel();

        textos.setOpaque(false);

        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(titulo);

        t.setForeground(TEXT);

        t.setFont(
                f("SansSerif", Font.PLAIN, 12)
        );

        JLabel d = new JLabel(desc);

        d.setForeground(MUTED);

        d.setFont(
                f("SansSerif", Font.PLAIN, 10)
        );

        textos.add(t);
        textos.add(d);

        row.add(textos, BorderLayout.CENTER);

        JPanel togWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        togWrap.setOpaque(false);

        togWrap.add(toggle);

        row.add(togWrap, BorderLayout.EAST);

        return row;
    }

    private void mostrarConfiguracion() {

        seccionVisible = "configuracion";

        activarNav(6);

        pageTitle.setText(
                "Configuración"
        );

        JPanel page = base();

        // ---- Encabezado ----

        JPanel head = new JPanel(new BorderLayout());

        head.setOpaque(false);

        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel headIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        headIzq.setOpaque(false);

        headIzq.add(new JLabel(new Icono(Icono.GEAR, GOLD, 34)));

        JPanel headTextos = new JPanel();

        headTextos.setOpaque(false);

        headTextos.setLayout(new BoxLayout(headTextos, BoxLayout.Y_AXIS));

        JLabel ht = new JLabel("Configuración");

        ht.setForeground(TEXT);

        ht.setFont(f("SansSerif", Font.BOLD, 29));

        JLabel hs = new JLabel("Ajusta la configuración del sistema de gimnasio.");

        hs.setForeground(MUTED);

        hs.setFont(f("SansSerif", Font.PLAIN, 13));

        headTextos.add(ht);
        headTextos.add(Box.createVerticalStrut(4));
        headTextos.add(hs);

        headIzq.add(headTextos);

        head.add(headIzq, BorderLayout.WEST);

        page.add(head);

        page.add(Box.createVerticalStrut(22));

        // ---- Pestañas ----

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        tabs.setOpaque(false);

        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);

        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        String[] tabNombres = {
            "General", "Apariencia", "Seguridad", "Respaldo"
        };

        int[] tabIconos = {
            Icono.GEAR, Icono.PALETTE, Icono.SHIELD, Icono.CLOUD
        };

        JButton[] tabBotones = new JButton[tabNombres.length];

        for (int i = 0; i < tabNombres.length; i++) {

            JButton tb;

            if (i == 0) {

                tb = gold(tabNombres[i], tabIconos[i]);

            } else {

                tb = new JButton(
                        tabNombres[i],
                        new Icono(tabIconos[i], MUTED, 15)
                );

                tb.setBackground(PANEL);
                tb.setForeground(MUTED);

                tb.setFont(f("SansSerif", Font.BOLD, 11));

                tb.setBorder(
                        new javax.swing.border.CompoundBorder(
                                new LineBorder(BORDER, 1),
                                new EmptyBorder(9, 14, 9, 14)
                        )
                );

                tb.setFocusPainted(false);

                tb.setIconTextGap(7);

                tb.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            tabBotones[i] = tb;

            tabs.add(tb);
        }

        page.add(tabs);

        page.add(Box.createVerticalStrut(22));

        // ---- Contenedor de pestañas (CardLayout) ----

        CardLayout cardLayout = new CardLayout();

        JPanel contenidoTabs = new JPanel(cardLayout);

        contenidoTabs.setOpaque(false);

        contenidoTabs.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Antes esto estaba fijo en 340 px y por eso "Versión del
        // sistema" / "Última actualización" quedaban tapados: la
        // tarjeta "Información del sistema" necesita más alto que
        // eso. La página ya tiene scroll propio, así que ampliar
        // esto no rompe nada, solo deja de recortar contenido.
        contenidoTabs.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 460)
        );

        contenidoTabs.setPreferredSize(
                new Dimension(Integer.MAX_VALUE, 460)
        );

        for (int i = 0; i < tabNombres.length; i++) {

            final int idx = i;

            tabBotones[i].addActionListener(e -> {

                cardLayout.show(contenidoTabs, tabNombres[idx]);

                for (int j = 0; j < tabBotones.length; j++) {

                    if (j == idx) {

                        tabBotones[j].setBackground(GOLD);
                        tabBotones[j].setForeground(Color.BLACK);
                        tabBotones[j].setIcon(
                                new Icono(tabIconos[j], Color.BLACK, 15)
                        );

                    } else {

                        tabBotones[j].setBackground(PANEL);
                        tabBotones[j].setForeground(MUTED);
                        tabBotones[j].setIcon(
                                new Icono(tabIconos[j], MUTED, 15)
                        );
                    }
                }
            });
        }

        // ---- Fila 1: Información + Interfaz ----

        JPanel panelGeneral = new JPanel(new GridLayout(1, 2, 20, 0));

        panelGeneral.setOpaque(false);

        panelGeneral.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelGeneral.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));

        // Información del sistema

        JPanel info = configCardWrap("Información del sistema", Icono.INFO);

        JTextField fNombre = field();
        JTextField fDireccion = field();
        JTextField fTelefono = field();
        JTextField fCorreo = field();
        JTextField fHorario = field();

        String[] etiquetasInfo = {
            "Nombre del gimnasio", "Dirección", "Teléfono", "Correo", "Horario de atención"
        };

        JTextField[] camposInfo = {
            fNombre, fDireccion, fTelefono, fCorreo, fHorario
        };

        String[] valoresInfo = {
            cfgNombreGym, cfgDireccion, cfgTelefono, cfgCorreo, cfgHorario
        };

        for (int i = 0; i < etiquetasInfo.length; i++) {

            JLabel lab = new JLabel(etiquetasInfo[i]);

            lab.setForeground(MUTED);
            lab.setFont(f("SansSerif", Font.PLAIN, 11));
            lab.setAlignmentX(Component.LEFT_ALIGNMENT);

            info.add(lab);
            info.add(Box.createVerticalStrut(4));

            JTextField f = camposInfo[i];

            f.setText(valoresInfo[i]);
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            info.add(f);
            info.add(Box.createVerticalStrut(10));
        }

        info.add(Box.createVerticalGlue());

        info.add(new LineSep());

        info.add(Box.createVerticalStrut(8));

        info.add(configRow("Versión del sistema", mutedLabel("1.0.0")));
        info.add(Box.createVerticalStrut(6));
        info.add(configRow("Última actualización", mutedLabel("01/09/2025 10:24")));

        panelGeneral.add(info);

        // Configuración de la interfaz

        JPanel interfaz = configCardWrap("Configuración de la interfaz", Icono.PALETTE);

        JComboBox<String> comboTema =
                combo(new String[]{"Oscuro", "Claro"});

        comboTema.setSelectedIndex(cfgTemaClaro ? 1 : 0);

        interfaz.add(configRow("Tema", comboTema));
        interfaz.add(Box.createVerticalStrut(14));

        JComboBox<String> comboFuente =
                combo(new String[]{"Aa Normal", "Aa Grande", "Aa Pequeño"});

        comboFuente.setSelectedIndex(
                cfgFontScale >= 1.15 ? 1 : (cfgFontScale <= 0.9 ? 2 : 0)
        );

        interfaz.add(configRow("Tamaño de fuente", comboFuente));
        interfaz.add(Box.createVerticalStrut(14));

        Toggle toggleNotificaciones = new Toggle(cfgMostrarNotificaciones);

        interfaz.add(configToggleRow("Mostrar notificaciones", "Recibir alertas del sistema y pagos.", toggleNotificaciones));
        interfaz.add(Box.createVerticalStrut(10));

        Toggle toggleHora = new Toggle(cfgMostrarHora);

        interfaz.add(configToggleRow("Mostrar hora en el header", "Mostrar la fecha y hora actual.", toggleHora));
        interfaz.add(Box.createVerticalStrut(10));
        Toggle toggleAnimaciones = new Toggle(cfgAnimaciones);

        interfaz.add(configToggleRow("Animaciones", "Habilitar transiciones y efectos visuales.", toggleAnimaciones));

        interfaz.add(Box.createVerticalGlue());

        JPanel panelApariencia = new JPanel(new GridLayout(1, 1));

        panelApariencia.setOpaque(false);

        panelApariencia.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelApariencia.add(interfaz);

        // ---- Seguridad + Respaldo (una tarjeta por pestaña) ----

        JPanel panelSeguridad = new JPanel(new GridLayout(1, 1));

        panelSeguridad.setOpaque(false);

        panelSeguridad.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelRespaldo = new JPanel(new GridLayout(1, 2, 20, 0));

        panelRespaldo.setOpaque(false);

        panelRespaldo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Seguridad y acceso

        JPanel seg = configCardWrap("Seguridad y acceso", Icono.SHIELD);

        JLabel segLab = new JLabel("Cambiar contraseña de administrador");
        segLab.setForeground(MUTED);
        segLab.setFont(f("SansSerif", Font.PLAIN, 11));
        segLab.setAlignmentX(Component.LEFT_ALIGNMENT);

        seg.add(segLab);
        seg.add(Box.createVerticalStrut(8));

        JButton cambiarPass = gold("Cambiar contraseña", Icono.KEY);

        cambiarPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        cambiarPass.addActionListener(e -> dialogoCambiarPassword());

        seg.add(cambiarPass);
        seg.add(Box.createVerticalStrut(16));

        JLabel inactLab = new JLabel("Tiempo de inactividad (minutos)");
        inactLab.setForeground(MUTED);
        inactLab.setFont(f("SansSerif", Font.PLAIN, 11));
        inactLab.setAlignmentX(Component.LEFT_ALIGNMENT);

        seg.add(inactLab);
        seg.add(Box.createVerticalStrut(4));

        JSpinner spinner = new JSpinner(
                new SpinnerNumberModel(cfgMinutosInactividad, 1, 120, 1)
        );
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Antes ocupaba todo el ancho de la tarjeta (era un
        // spinner numérico, no necesita tanto espacio).
        spinner.setMaximumSize(new Dimension(90, 32));
        spinner.setPreferredSize(new Dimension(90, 32));

        JPanel spinnerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerRow.setOpaque(false);
        spinnerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        spinnerRow.add(spinner);

        seg.add(spinnerRow);
        seg.add(Box.createVerticalStrut(14));

        Toggle toggleCierreAuto = new Toggle(cfgCierreAutomatico);

        seg.add(configToggleRow("Activar cierre automático de sesión", "Cerrar sesión por inactividad.", toggleCierreAuto));
        seg.add(Box.createVerticalStrut(14));

        JComboBox<String> comboNivelSeg =
                combo(new String[]{"Alto", "Medio", "Bajo"});

        comboNivelSeg.setSelectedItem(cfgNivelSeguridad);

        seg.add(configRow("Nivel de seguridad", comboNivelSeg));

        seg.add(Box.createVerticalGlue());

        panelSeguridad.add(seg);

        // Respaldo automático

        JPanel resp = configCardWrap("Respaldo automático", Icono.CLOUD);

        Toggle toggleRespaldoAuto = new Toggle(cfgRespaldoAuto);

        resp.add(configToggleRow(
                "Habilitar respaldos automáticos",
                "Envía un PDF con los datos al correo indicado.",
                toggleRespaldoAuto
        ));
        resp.add(Box.createVerticalStrut(14));

        JComboBox<String> comboFrecuencia =
                combo(new String[]{"Diario", "Semanal", "Mensual"});

        comboFrecuencia.setSelectedItem(cfgRespaldoFrecuencia);

        resp.add(configRow("Frecuencia", comboFrecuencia));
        resp.add(Box.createVerticalStrut(14));

        // Hora con AM/PM elegible (internamente se guarda en 24 h).
        int hora12 = cfgRespaldoHora % 12 == 0 ? 12 : cfgRespaldoHora % 12;

        JSpinner spHoraResp =
                new JSpinner(new SpinnerNumberModel(hora12, 1, 12, 1));

        JSpinner spMinResp =
                new JSpinner(new SpinnerNumberModel(cfgRespaldoMinuto, 0, 59, 1));

        spMinResp.setEditor(new JSpinner.NumberEditor(spMinResp, "00"));

        spHoraResp.setPreferredSize(new Dimension(56, 32));
        spMinResp.setPreferredSize(new Dimension(56, 32));

        // Mismo estilo oscuro que el resto de los campos.
        for (JSpinner sp : new JSpinner[]{spHoraResp, spMinResp}) {

            JTextField tf =
                    ((JSpinner.DefaultEditor) sp.getEditor()).getTextField();

            tf.setBackground(INPUT);
            tf.setForeground(TEXT);
            tf.setCaretColor(TEXT);
            tf.setHorizontalAlignment(JTextField.CENTER);

            sp.setBorder(BorderFactory.createLineBorder(BORDER));
        }

        JComboBox<String> comboAmPm = combo(new String[]{"AM", "PM"});

        comboAmPm.setSelectedItem(cfgRespaldoHora >= 12 ? "PM" : "AM");

        comboAmPm.setPreferredSize(new Dimension(70, 32));
        comboAmPm.setMaximumSize(new Dimension(70, 32));

        JLabel dosPuntos = new JLabel(":");
        dosPuntos.setForeground(TEXT);

        JPanel horaPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        horaPanel.setOpaque(false);

        horaPanel.add(spHoraResp);
        horaPanel.add(dosPuntos);
        horaPanel.add(spMinResp);
        horaPanel.add(comboAmPm);

        resp.add(configRow("Hora del respaldo", horaPanel));
        resp.add(Box.createVerticalStrut(6));

        JLabel avisoFrecuencia = new JLabel(
                "<html><body style='width:250px'>Diario: todos los días. "
                + "Semanal: los lunes. Mensual: el día 1. Se ejecuta "
                + "solo mientras el programa está abierto.</body></html>"
        );

        avisoFrecuencia.setForeground(MUTED);
        avisoFrecuencia.setFont(f("SansSerif", Font.ITALIC, 10));
        avisoFrecuencia.setAlignmentX(Component.LEFT_ALIGNMENT);

        resp.add(avisoFrecuencia);
        resp.add(Box.createVerticalStrut(14));

        JLabel rutaLab = new JLabel("Correo donde recibir los respaldos");
        rutaLab.setForeground(MUTED);
        rutaLab.setFont(f("SansSerif", Font.PLAIN, 11));
        rutaLab.setAlignmentX(Component.LEFT_ALIGNMENT);

        resp.add(rutaLab);
        resp.add(Box.createVerticalStrut(4));

        JTextField rutaField = field();
        rutaField.setText(cfgCorreoRespaldo);
        rutaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        rutaField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        resp.add(rutaField);
        resp.add(Box.createVerticalStrut(6));

        JLabel avisoRuta = new JLabel(
                "<html>El respaldo se envía como PDF adjunto a este "
                + "correo; no queda guardado en este dispositivo.</html>"
        );

        avisoRuta.setForeground(MUTED);
        avisoRuta.setFont(f("SansSerif", Font.ITALIC, 10));
        avisoRuta.setAlignmentX(Component.LEFT_ALIGNMENT);

        resp.add(avisoRuta);
        resp.add(Box.createVerticalStrut(16));

        JButton respaldar = gold("Realizar respaldo ahora", Icono.CLOUD);
        respaldar.setAlignmentX(Component.LEFT_ALIGNMENT);

        respaldar.addActionListener(e -> realizarRespaldo(rutaField.getText().trim()));

        resp.add(respaldar);

        resp.add(Box.createVerticalGlue());

        panelRespaldo.add(resp);

        // ---- Correo para comprobantes ----

        JPanel correoCard = configCardWrap("Correo para comprobantes (SMTP)", Icono.INFO);

        JLabel infoComprobante = new JLabel(
                "<html><b>¿Qué es esto?</b> Es el correo que usa el "
                + "sistema para MANDAR los comprobantes — no es el "
                + "correo del socio, es el correo del gimnasio (el "
                + "remitente). Si no piensas enviar comprobantes por "
                + "correo, puedes dejar esto vacío sin problema; el "
                + "resto de la app funciona igual.</html>"
        );

        infoComprobante.setForeground(MUTED);
        infoComprobante.setFont(f("SansSerif", Font.ITALIC, 10));
        infoComprobante.setAlignmentX(Component.LEFT_ALIGNMENT);

        correoCard.add(infoComprobante);

        correoCard.add(Box.createVerticalStrut(14));

        JLabel corSmtpLab = new JLabel("Correo remitente (Gmail recomendado)");
        corSmtpLab.setForeground(MUTED);
        corSmtpLab.setFont(f("SansSerif", Font.PLAIN, 11));
        corSmtpLab.setAlignmentX(Component.LEFT_ALIGNMENT);

        correoCard.add(corSmtpLab);
        correoCard.add(Box.createVerticalStrut(4));

        JTextField correoSmtpField = field();
        correoSmtpField.setText(cfgSmtpCorreo);
        correoSmtpField.setAlignmentX(Component.LEFT_ALIGNMENT);
        correoSmtpField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        correoCard.add(correoSmtpField);
        correoCard.add(Box.createVerticalStrut(12));

        JLabel passSmtpLab = new JLabel("Contraseña de aplicación (no la de tu cuenta)");
        passSmtpLab.setForeground(MUTED);
        passSmtpLab.setFont(f("SansSerif", Font.PLAIN, 11));
        passSmtpLab.setAlignmentX(Component.LEFT_ALIGNMENT);

        correoCard.add(passSmtpLab);
        correoCard.add(Box.createVerticalStrut(4));

        JPasswordField passSmtpField = new JPasswordField();
        passSmtpField.setText(cfgSmtpPassword);
        passSmtpField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passSmtpField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        passSmtpField.setBackground(INPUT);
        passSmtpField.setForeground(TEXT);
        passSmtpField.setCaretColor(TEXT);
        passSmtpField.setBorder(new LineBorder(BORDER));

        correoCard.add(passSmtpField);
        correoCard.add(Box.createVerticalStrut(14));

        JLabel avisoSmtp = new JLabel(
                "<html><b>¿Dónde consigo esa contraseña?</b> No es la "
                + "contraseña con la que entras a tu Gmail. Necesitas "
                + "activar la \"verificación en 2 pasos\" en tu cuenta "
                + "de Google y luego entrar a "
                + "myaccount.google.com/apppasswords para generar un "
                + "código de 16 letras — ese código es el que va aquí.</html>"
        );

        avisoSmtp.setForeground(MUTED);
        avisoSmtp.setFont(f("SansSerif", Font.ITALIC, 10));
        avisoSmtp.setAlignmentX(Component.LEFT_ALIGNMENT);

        correoCard.add(avisoSmtp);

        correoCard.add(Box.createVerticalGlue());

        panelRespaldo.add(correoCard);

        // Otras opciones

        JPanel otras = configCardWrap("Otras opciones", Icono.SLIDERS);

        Toggle toggleSinMembresia = new Toggle(cfgPermitirSinMembresia);

        otras.add(configToggleRow("Permitir registro de socios sin membresía", "Solo para visitas o planes personalizados.", toggleSinMembresia));
        otras.add(Box.createVerticalStrut(12));

        Toggle toggleHistorialPagos = new Toggle(cfgMostrarHistorialPagos);

        otras.add(configToggleRow("Mostrar historial de pagos", "En el perfil del socio.", toggleHistorialPagos));
        otras.add(Box.createVerticalStrut(12));

        Toggle toggleComprobantes = new Toggle(cfgImprimirComprobantes);

        otras.add(configToggleRow("Imprimir comprobantes", "Generar comprobantes de pagos.", toggleComprobantes));
        otras.add(Box.createVerticalStrut(12));

        Toggle toggleMantenimiento = new Toggle(cfgModoMantenimiento);

        otras.add(configToggleRow("Modo de mantenimiento", "Deshabilita el acceso para usuarios normales.", toggleMantenimiento));

        otras.add(Box.createVerticalGlue());

        panelGeneral.add(otras);

        // ---- Ensamblar CardLayout ----

        contenidoTabs.add(panelGeneral, "General");
        contenidoTabs.add(panelApariencia, "Apariencia");
        contenidoTabs.add(panelSeguridad, "Seguridad");
        contenidoTabs.add(panelRespaldo, "Respaldo");

        page.add(contenidoTabs);

        page.add(Box.createVerticalStrut(20));

        // ---- Botón guardar ----

        JPanel guardarWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        guardarWrap.setOpaque(false);

        guardarWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        guardarWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton guardar = gold("Guardar cambios", Icono.SAVE);

        guardar.setPreferredSize(new Dimension(170, 40));

        guardar.addActionListener(e -> {

            cfgNombreGym = fNombre.getText().trim();
            cfgDireccion = fDireccion.getText().trim();
            cfgTelefono = fTelefono.getText().trim();
            cfgCorreo = fCorreo.getText().trim();
            cfgHorario = fHorario.getText().trim();

            cfgMostrarHora = toggleHora.isOn();
            cfgCierreAutomatico = toggleCierreAuto.isOn();
            cfgMinutosInactividad = (Integer) spinner.getValue();
            cfgCorreoRespaldo = rutaField.getText().trim();

            cfgRespaldoAuto = toggleRespaldoAuto.isOn();
            cfgRespaldoFrecuencia = (String) comboFrecuencia.getSelectedItem();
            cfgRespaldoHora =
                    ((Integer) spHoraResp.getValue()) % 12
                    + ("PM".equals(comboAmPm.getSelectedItem()) ? 12 : 0);
            cfgRespaldoMinuto = (Integer) spMinResp.getValue();

            // Reinicia el programador con la nueva hora/frecuencia.
            iniciarRespaldoAutomatico();

            cfgSmtpCorreo = correoSmtpField.getText().trim();
            cfgSmtpPassword = new String(passSmtpField.getPassword());

            cfgImprimirComprobantes = toggleComprobantes.isOn();

            cfgMostrarNotificaciones = toggleNotificaciones.isOn();
            cfgAnimaciones = toggleAnimaciones.isOn();
            cfgNivelSeguridad = (String) comboNivelSeg.getSelectedItem();
            cfgPermitirSinMembresia = toggleSinMembresia.isOn();
            cfgMostrarHistorialPagos = toggleHistorialPagos.isOn();
            cfgModoMantenimiento = toggleMantenimiento.isOn();

            Login.MODO_MANTENIMIENTO = cfgModoMantenimiento;

            switch (comboFuente.getSelectedIndex()) {
                case 1:
                    cfgFontScale = 1.18;
                    break;
                case 2:
                    cfgFontScale = 0.88;
                    break;
                default:
                    cfgFontScale = 1.0;
            }

            actualizarNombreGimnasio();

            // aplicarTema reconstruye toda la interfaz (sidebar,
            // header y esta misma pantalla) con el tema y el
            // tamaño de letra ya guardados, así el cambio se ve
            // de inmediato sin reiniciar el programa.
            aplicarTema(comboTema.getSelectedIndex() == 1);

            guardarConfiguracionEnDisco();

            mensaje(
                    "Cambios guardados correctamente."
            );
        });

        guardarWrap.add(guardar);

        page.add(guardarWrap);

        mostrarContenido(page);
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    private String mensajeRequisitosPassword() {

        if ("Bajo".equals(cfgNivelSeguridad)) {

            return "La contraseña debe tener mínimo 4 caracteres.";

        } else if ("Medio".equals(cfgNivelSeguridad)) {

            return "La contraseña debe tener mínimo 6 caracteres, "
                    + "con al menos una letra y un número.";
        }

        return "La contraseña debe tener mínimo 8 caracteres, "
                + "una mayúscula, una minúscula, un número y un "
                + "carácter especial.";
    }

    private void dialogoCambiarPassword() {

        JPasswordField actual = new JPasswordField();
        JPasswordField nueva = new JPasswordField();
        JPasswordField confirmar = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 8));

        panel.add(new JLabel("Contraseña actual:"));
        panel.add(actual);
        panel.add(new JLabel("Nueva contraseña:"));
        panel.add(nueva);
        panel.add(new JLabel("Confirmar nueva contraseña:"));
        panel.add(confirmar);

        int r = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Cambiar contraseña de administrador",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (r != JOptionPane.OK_OPTION) {
            return;
        }

        String actualTxt = new String(actual.getPassword());
        String nuevaTxt = new String(nueva.getPassword());
        String confirmarTxt = new String(confirmar.getPassword());

        if (!actualTxt.equals(Login.CUENTAS.get(Login.USUARIO_ACTUAL))) {

            mensaje("La contraseña actual no es correcta.");
            return;
        }

        if (!RegexControl.password(nuevaTxt, cfgNivelSeguridad)) {

            mensaje(mensajeRequisitosPassword());
            return;
        }

        if (!nuevaTxt.equals(confirmarTxt)) {

            mensaje("La confirmación no coincide con la nueva contraseña.");
            return;
        }

        Login.CUENTAS.put(Login.USUARIO_ACTUAL, nuevaTxt);

        mensaje("Contraseña actualizada correctamente.");
    }

    // =========================================================
    // CREAR NUEVA CUENTA DE ADMINISTRADOR
    // =========================================================

    private void dialogoCrearCuenta() {

        JTextField usuarioField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JPasswordField confirmarField = new JPasswordField();
        JTextField respuestaField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 6));

        panel.add(new JLabel("Nuevo usuario:"));
        panel.add(usuarioField);
        panel.add(new JLabel("Contraseña:"));
        panel.add(passField);
        panel.add(new JLabel("Confirmar contraseña:"));
        panel.add(confirmarField);
        panel.add(new JLabel("Respuesta secreta (para recuperar la contraseña):"));
        panel.add(respuestaField);

        int r = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Crear nueva cuenta",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (r != JOptionPane.OK_OPTION) {
            return;
        }

        String usuario = usuarioField.getText().trim();
        String pass = new String(passField.getPassword());
        String confirmar = new String(confirmarField.getPassword());
        String respuesta = respuestaField.getText().trim();

        if (!RegexControl.usuario(usuario)) {

            mensaje(
                    "El usuario debe tener entre 4 y 20 caracteres, "
                    + "solo letras, números y guion bajo."
            );
            return;
        }

        if (Login.CUENTAS.containsKey(usuario)) {

            mensaje("Ya existe una cuenta con ese usuario.");
            return;
        }

        if (!RegexControl.password(pass, cfgNivelSeguridad)) {

            mensaje(mensajeRequisitosPassword());
            return;
        }

        if (!pass.equals(confirmar)) {

            mensaje("La confirmación no coincide con la contraseña.");
            return;
        }

        if (respuesta.isEmpty()) {

            mensaje("Ingresa una respuesta secreta para poder recuperar la cuenta.");
            return;
        }

        Login.CUENTAS.put(usuario, pass);
        Login.PREGUNTAS.put(usuario, respuesta);

        mensaje("Cuenta creada correctamente. Ya puede iniciar sesión con ella.");
    }

    // =========================================================
    // RESPALDO DE DATOS
    // =========================================================

    /** Botón "Realizar respaldo ahora". */
    private void realizarRespaldo(String correoDestino) {

        enviarRespaldo(correoDestino, false);
    }

    /** Rellena o recorta un texto a un ancho fijo (columnas del PDF). */
    private String colPdf(String texto, int ancho) {

        if (texto == null) {
            texto = "";
        }

        if (texto.length() > ancho) {

            texto = texto.substring(0, ancho - 1) + ".";
        }

        StringBuilder sb = new StringBuilder(texto);

        while (sb.length() < ancho) {
            sb.append(' ');
        }

        return sb.toString();
    }

    /** Arma el respaldo completo como PDF en un archivo temporal. */
    private java.io.File generarPdfRespaldo() throws java.io.IOException {

        java.io.File temporal =
                java.io.File.createTempFile("respaldo_gym_", ".pdf");

        temporal.deleteOnExit();

        control.PdfSimple pdf = new control.PdfSimple();

        java.util.Locale us = java.util.Locale.US;

        pdf.linea("RESPALDO - " + cfgNombreGym, 15, true);

        pdf.linea(
                "Generado: "
                + new SimpleDateFormat("dd/MM/yyyy hh:mm a").format(new Date()),
                9,
                false
        );

        pdf.espacio();

        // ---- Socios ----

        pdf.linea("SOCIOS (" + control.getSocios().size() + ")", 11, true);

        pdf.linea(
                colPdf("ID", 4) + " " + colPdf("NOMBRE", 24) + " "
                + colPdf("DNI", 9) + " " + colPdf("MEMBRESIA", 12) + " "
                + colPdf("ESTADO", 10) + " " + colPdf("ALTA", 10) + " "
                + colPdf("VENCE", 10),
                9,
                true
        );

        for (Socio s : control.getSocios()) {

            pdf.linea(
                    colPdf(String.valueOf(s.getId()), 4) + " "
                    + colPdf(s.getNombre(), 24) + " "
                    + colPdf(s.getDni(), 9) + " "
                    + colPdf(s.getMembresia(), 12) + " "
                    + colPdf(s.isActivo() ? "Activo" : "Suspendido", 10) + " "
                    + colPdf(s.getFechaAlta(), 10) + " "
                    + colPdf(s.getFechaVencimiento(), 10),
                    9,
                    false
            );
        }

        pdf.espacio();

        // ---- Entrenadores ----

        pdf.linea(
                "ENTRENADORES (" + control.getEntrenadores().size() + ")",
                11,
                true
        );

        pdf.linea(
                colPdf("ID", 4) + " " + colPdf("NOMBRE", 24) + " "
                + colPdf("ESPECIALIDAD", 20) + " "
                + colPdf("SALARIO", 12) + " " + colPdf("ESTADO", 10),
                9,
                true
        );

        for (Entrenador en : control.getEntrenadores()) {

            pdf.linea(
                    colPdf(String.valueOf(en.getId()), 4) + " "
                    + colPdf(en.getNombre(), 24) + " "
                    + colPdf(en.getEspecialidad(), 20) + " "
                    + colPdf(String.format(us, "$%.2f", en.getSalario()), 12) + " "
                    + colPdf(en.isActivo() ? "Activo" : "Inactivo", 10),
                    9,
                    false
            );
        }

        pdf.espacio();

        // ---- Membresías ----

        pdf.linea(
                "MEMBRESIAS (" + control.getMembresias().size() + ")",
                11,
                true
        );

        pdf.linea(
                colPdf("NOMBRE", 20) + " " + colPdf("DURACION", 12) + " "
                + colPdf("PRECIO", 12),
                9,
                true
        );

        for (Membresia m : control.getMembresias()) {

            pdf.linea(
                    colPdf(m.getNombre(), 20) + " "
                    + colPdf(m.getDuracionDias() + " dias", 12) + " "
                    + colPdf(String.format(us, "$%.2f", m.getPrecio()), 12),
                    9,
                    false
            );
        }

        pdf.espacio();

        // ---- Pagos ----

        pdf.linea("PAGOS (" + control.getPagos().size() + ")", 11, true);

        pdf.linea(
                colPdf("FECHA", 10) + " " + colPdf("SOCIO", 24) + " "
                + colPdf("MEMBRESIA", 12) + " " + colPdf("MONTO", 10) + " "
                + colPdf("METODO", 13) + " " + colPdf("CONCEPTO", 20),
                9,
                true
        );

        for (Pago pg : control.getPagos()) {

            pdf.linea(
                    colPdf(pg.getFecha(), 10) + " "
                    + colPdf(pg.getSocioNombre(), 24) + " "
                    + colPdf(pg.getMembresia(), 12) + " "
                    + colPdf(String.format(us, "$%.2f", pg.getMonto()), 10) + " "
                    + colPdf(pg.getMetodoPago(), 13) + " "
                    + colPdf(pg.getConcepto(), 20),
                    9,
                    false
            );
        }

        pdf.espacio();

        pdf.linea(
                "Ingresos totales: "
                + String.format(us, "$%.2f", control.totalIngresos()),
                11,
                true
        );

        pdf.guardar(temporal);

        return temporal;
    }

    /**
     * Genera el PDF de respaldo y lo envía por correo.
     * "automatico" = lo disparó el programador (no el botón).
     */
    private void enviarRespaldo(String correoDestino, boolean automatico) {

        String prefijo = automatico
                ? "No se pudo hacer el respaldo automático: "
                : "";

        if (correoDestino == null || correoDestino.trim().isEmpty()) {

            mensaje(
                    prefijo
                    + "Escribe el correo donde quieres recibir el respaldo."
            );

            return;
        }

        if (cfgSmtpCorreo.trim().isEmpty()
                || cfgSmtpPassword.trim().isEmpty()) {

            mensaje(
                    prefijo
                    + "Antes configura el correo remitente y su contraseña "
                    + "de aplicación (a la derecha, en \"Correo para "
                    + "comprobantes (SMTP)\")."
            );

            return;
        }

        try {

            String nombreArchivo =
                    "respaldo_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                    + ".pdf";

            java.io.File temporal = generarPdfRespaldo();

            cfgCorreoRespaldo = correoDestino;

            final String destinoFinal = correoDestino;

            new Thread(() -> {

                try {

                    CorreoSimple.enviar(
                            cfgSmtpHost,
                            cfgSmtpPuerto,
                            cfgSmtpCorreo.trim(),
                            cfgSmtpPassword,
                            cfgNombreGym,
                            destinoFinal,
                            "Respaldo - " + cfgNombreGym + " - "
                            + new SimpleDateFormat("dd/MM/yyyy hh:mm a")
                                    .format(new Date()),
                            (automatico ? "Respaldo automático" : "Respaldo")
                            + " adjunto (PDF) de " + cfgNombreGym
                            + ".\n\nEste archivo no queda guardado en el "
                            + "dispositivo, solo en este correo.",
                            temporal,
                            nombreArchivo
                    );

                    temporal.delete();

                    if (!automatico || cfgMostrarNotificaciones) {

                        SwingUtilities.invokeLater(() -> mensaje(
                                (automatico ? "Respaldo automático" : "Respaldo")
                                + " enviado correctamente a "
                                + destinoFinal
                                + "."
                        ));
                    }

                } catch (Exception ex) {

                    temporal.delete();

                    SwingUtilities.invokeLater(() -> mensaje(
                            "No se pudo enviar el respaldo por correo: "
                            + ex.getMessage()
                    ));
                }

            }).start();

        } catch (Exception ex) {

            mensaje("No se pudo generar el respaldo: " + ex.getMessage());
        }
    }

    // =========================================================
    // PROGRAMADOR DE RESPALDO AUTOMÁTICO
    // =========================================================

    /**
     * Inicio del periodo actual a la hora configurada: hoy (Diario),
     * el lunes de esta semana (Semanal) o el día 1 del mes (Mensual).
     */
    private java.util.Calendar inicioPeriodoRespaldo() {

        java.util.Calendar c = java.util.Calendar.getInstance();

        c.setFirstDayOfWeek(java.util.Calendar.MONDAY);

        c.set(java.util.Calendar.HOUR_OF_DAY, cfgRespaldoHora);
        c.set(java.util.Calendar.MINUTE, cfgRespaldoMinuto);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);

        if ("Semanal".equals(cfgRespaldoFrecuencia)) {

            c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);

        } else if ("Mensual".equals(cfgRespaldoFrecuencia)) {

            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        return c;
    }

    private String clavePeriodoRespaldo() {

        return new SimpleDateFormat("yyyyMMdd")
                .format(inicioPeriodoRespaldo().getTime());
    }

    /**
     * Arranca (o reinicia) la revisión que dispara el respaldo a la
     * hora configurada. Si la hora de este periodo ya pasó al momento
     * de activarlo, se espera al siguiente periodo (no se manda un
     * respaldo "atrasado" de golpe).
     */
    private void iniciarRespaldoAutomatico() {

        if (timerRespaldo != null) {
            timerRespaldo.stop();
        }

        ultimoRespaldoClave =
                System.currentTimeMillis()
                        >= inicioPeriodoRespaldo().getTimeInMillis()
                        ? clavePeriodoRespaldo()
                        : "";

        timerRespaldo =
                new javax.swing.Timer(30_000, e -> revisarRespaldoAutomatico());

        timerRespaldo.setRepeats(true);

        timerRespaldo.start();
    }

    private void revisarRespaldoAutomatico() {

        if (!cfgRespaldoAuto) {
            return;
        }

        if (System.currentTimeMillis()
                < inicioPeriodoRespaldo().getTimeInMillis()) {
            return;
        }

        String clave = clavePeriodoRespaldo();

        if (clave.equals(ultimoRespaldoClave)) {
            return;
        }

        ultimoRespaldoClave = clave;

        enviarRespaldo(cfgCorreoRespaldo, true);
    }

    // =========================================================
    // ENVIAR TICKET DE PAGO POR CORREO (CorreoSimple, sin librerías)
    // =========================================================

    /**
     * Genera el comprobante de un pago como archivo de texto en la
     * carpeta "comprobantes" (junto al programa) y lo abre con el
     * lector de PDF/texto predeterminado de Windows para imprimirlo.
     * Se activa con el interruptor "Imprimir comprobantes" de
     * Configuración > Otras opciones.
     */
    /**
     * Envía el comprobante de pago SOLO por correo al socio (no abre
     * ni "imprime" ningún archivo en la computadora del gimnasio).
     * Se usa desde Pagos > Editar pago con el checkbox de enviarlo.
     */
    private void generarYEnviarComprobante(
            Socio socio,
            Pago pago,
            boolean enviarPorCorreo
    ) {

        if (socio == null || pago == null) {
            return;
        }

        if (!enviarPorCorreo) {
            return;
        }

        String destino = socio.getCorreo();

        if (destino == null || destino.trim().isEmpty()) {

            mensaje(
                    "Este socio no tiene correo registrado, así que "
                    + "no se pudo enviar el comprobante."
            );

            return;
        }

        if (cfgSmtpCorreo.trim().isEmpty()
                || cfgSmtpPassword.trim().isEmpty()) {

            mensaje(
                    "No se pudo enviar el comprobante por correo: "
                    + "configura el correo remitente en Configuración > "
                    + "Respaldo."
            );

            return;
        }

        // Se envía en un hilo aparte para no congelar la interfaz
        // mientras se conecta al servidor de correo.

        new Thread(() -> {

            try {

                String cuerpo =
                        "Hola " + socio.getNombre() + ",\n\n"
                        + "Gracias por tu pago en " + cfgNombreGym + ".\n\n"
                        + "-------------------------------------\n"
                        + "COMPROBANTE DE PAGO #" + pago.getId() + "\n"
                        + "-------------------------------------\n"
                        + "Membresía: " + pago.getMembresia() + "\n"
                        + "Monto: $" + String.format("%.2f", pago.getMonto()) + "\n"
                        + "Método de pago: " + pago.getMetodoPago() + "\n"
                        + "Fecha: " + pago.getFecha() + "\n"
                        + "Concepto: " + pago.getConcepto() + "\n"
                        + "-------------------------------------\n\n"
                        + "Este es un comprobante generado automáticamente, "
                        + "no es necesario responder este correo.\n\n"
                        + cfgNombreGym + "\n"
                        + cfgDireccion + "\n"
                        + cfgTelefono;

                CorreoSimple.enviar(
                        cfgSmtpHost,
                        cfgSmtpPuerto,
                        cfgSmtpCorreo.trim(),
                        cfgSmtpPassword,
                        cfgNombreGym,
                        destino,
                        "Comprobante de pago - " + cfgNombreGym,
                        cuerpo,
                        null,
                        null
                );

            } catch (Exception ex) {

                SwingUtilities.invokeLater(() -> mensaje(
                        "No se pudo enviar el comprobante por correo: "
                        + ex.getMessage()
                ));
            }

        }).start();
    }



    private JLabel mutedLabel(String texto) {

        JLabel l = new JLabel(texto);

        l.setForeground(TEXT);

        l.setFont(f("SansSerif", Font.BOLD, 12));

        return l;
    }

    private JLabel timeLabel(String texto) {

        JLabel l = new JLabel(texto, new Icono(Icono.CLOCK, MUTED, 15), SwingConstants.LEFT);

        l.setForeground(TEXT);

        l.setFont(f("SansSerif", Font.PLAIN, 12));

        l.setIconTextGap(6);

        l.setBackground(INPUT);
        l.setOpaque(true);

        l.setBorder(new javax.swing.border.CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        return l;
    }

    private class LineSep extends JPanel {

        LineSep() {

            setOpaque(false);

            setAlignmentX(Component.LEFT_ALIGNMENT);

            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            setPreferredSize(new Dimension(10, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            g.setColor(BORDER);
            g.drawLine(0, 0, getWidth(), 0);
        }
    }

    // =========================================================
    // GRÁFICA
    // =========================================================

    // =========================================================
    // GRÁFICA CIRCULAR (DONA / PASTEL)
    // =========================================================

    private class RingChart
            extends JPanel {

        private final java.util.LinkedHashMap<String, Double> datos;
        private final java.util.LinkedHashMap<String, Color> colores;
        private final boolean dona;
        private final String centroTexto;

        RingChart(
                java.util.LinkedHashMap<String, Double> datos,
                java.util.LinkedHashMap<String, Color> colores,
                boolean dona,
                String centroTexto
        ) {

            this.datos = datos;
            this.colores = colores;
            this.dona = dona;
            this.centroTexto = centroTexto;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D x = (Graphics2D) g.create();

            x.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            double total = 0;

            for (double v : datos.values()) {
                total += v;
            }

            int diametro = Math.min(getWidth(), getHeight()) - 10;

            if (diametro < 20) {
                diametro = 20;
            }

            int cx = (getWidth() - diametro) / 2;
            int cy = (getHeight() - diametro) / 2;

            if (total <= 0) {

                x.setColor(BORDER);

                x.fillOval(cx, cy, diametro, diametro);

            } else {

                double inicio = 90;

                for (java.util.Map.Entry<String, Double> e : datos.entrySet()) {

                    double porcion = e.getValue() / total * 360.0;

                    if (porcion <= 0) {
                        continue;
                    }

                    Color c = colores.get(e.getKey());

                    x.setColor(c == null ? MUTED : c);

                    x.fillArc(
                            cx, cy,
                            diametro, diametro,
                            (int) Math.round(inicio),
                            -(int) Math.round(porcion)
                    );

                    inicio -= porcion;
                }
            }

            if (dona) {

                int anillo = (int) (diametro * 0.55);

                int ox = cx + (diametro - anillo) / 2;
                int oy = cy + (diametro - anillo) / 2;

                x.setColor(PANEL);

                x.fillOval(ox, oy, anillo, anillo);

                if (centroTexto != null) {

                    x.setColor(TEXT);

                    x.setFont(f("SansSerif", Font.BOLD, 20));

                    FontMetrics fm = x.getFontMetrics();

                    String[] lineas = centroTexto.split("\n");

                    int ly = getHeight() / 2 - (lineas.length * fm.getHeight()) / 2 + fm.getAscent();

                    for (int i = 0; i < lineas.length; i++) {

                        if (i == 1) {

                            x.setFont(f("SansSerif", Font.PLAIN, 11));
                            x.setColor(MUTED);

                            fm = x.getFontMetrics();
                        }

                        int lw = fm.stringWidth(lineas[i]);

                        x.drawString(
                                lineas[i],
                                getWidth() / 2 - lw / 2,
                                ly + i * (fm.getHeight())
                        );
                    }
                }
            }

            x.dispose();
        }
    }

    private JPanel leyenda(
            java.util.LinkedHashMap<String, Double> datos,
            java.util.LinkedHashMap<String, Color> colores,
            boolean conteo
    ) {

        JPanel p = new JPanel();

        p.setOpaque(false);

        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        double total = 0;

        for (double v : datos.values()) {
            total += v;
        }

        for (java.util.Map.Entry<String, Double> e : datos.entrySet()) {

            JPanel fila = new JPanel(new BorderLayout(8, 0));

            fila.setOpaque(false);

            fila.setAlignmentX(Component.LEFT_ALIGNMENT);

            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

            Color c = colores.get(e.getKey());

            JLabel dot = new JLabel("●");

            dot.setForeground(c == null ? MUTED : c);

            JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

            izq.setOpaque(false);

            izq.add(dot);

            JLabel nombre = new JLabel(e.getKey());

            nombre.setForeground(TEXT);

            nombre.setFont(f("SansSerif", Font.PLAIN, 12));

            izq.add(nombre);

            fila.add(izq, BorderLayout.WEST);

            double pct = total > 0 ? (e.getValue() / total * 100) : 0;

            String der =
                    conteo
                    ? String.format("%.0f%% (%.0f)", pct, e.getValue())
                    : String.format("%.0f%%", pct);

            JLabel derLab = new JLabel(der);

            derLab.setForeground(MUTED);

            derLab.setFont(f("SansSerif", Font.BOLD, 11));

            fila.add(derLab, BorderLayout.EAST);

            p.add(fila);

            p.add(Box.createVerticalStrut(6));
        }

        return p;
    }

    // =========================================================
    // INGRESOS REALES POR MES (últimos 6 meses)
    // =========================================================

    private double[] ingresosPorMesReal() {

        double[] valores = new double[6];

        java.util.Calendar ahora = java.util.Calendar.getInstance();

        int[] anioMes = new int[6];

        java.util.Calendar cal = (java.util.Calendar) ahora.clone();

        for (int i = 5; i >= 0; i--) {

            anioMes[i] =
                    cal.get(java.util.Calendar.YEAR) * 100
                    + cal.get(java.util.Calendar.MONTH);

            cal.add(java.util.Calendar.MONTH, -1);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");

        for (Pago p : control.getPagos()) {

            try {

                java.util.Calendar c = java.util.Calendar.getInstance();

                c.setTime(fmt.parse(p.getFecha()));

                int clave =
                        c.get(java.util.Calendar.YEAR) * 100
                        + c.get(java.util.Calendar.MONTH);

                for (int i = 0; i < 6; i++) {

                    if (anioMes[i] == clave) {

                        valores[i] += p.getMonto();
                        break;
                    }
                }

            } catch (Exception ignored) {
            }
        }

        return valores;
    }

    private String[] etiquetasMesesReal() {

        String[] etiquetas = new String[6];

        String[] nombres = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        };

        java.util.Calendar cal = java.util.Calendar.getInstance();

        cal.add(java.util.Calendar.MONTH, -5);

        for (int i = 0; i < 6; i++) {

            etiquetas[i] = nombres[cal.get(java.util.Calendar.MONTH)];

            cal.add(java.util.Calendar.MONTH, 1);
        }

        return etiquetas;
    }

    private class Chart
            extends JPanel {

        Chart() {

            setOpaque(false);

            setToolTipText("Ingresos reales de los últimos 6 meses");
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D x = (Graphics2D) g.create();

            x.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = getWidth();
            int h = getHeight();

            double[] valores = ingresosPorMesReal();

            String[] meses = etiquetasMesesReal();

            // ---- Escala: se redondea hacia arriba a un número "bonito" ----

            double maxReal = 0;

            for (double v : valores) {
                maxReal = Math.max(maxReal, v);
            }

            double max = escalaBonita(maxReal);

            // ---- Zona de dibujo ----

            int margenIzq = 58;
            int margenArriba = 14;
            int margenAbajo = 30;

            int areaAlto = Math.max(20, h - margenArriba - margenAbajo);
            int areaAncho = Math.max(40, w - margenIzq - 14);

            int lineas = 4;

            x.setFont(f("SansSerif", Font.PLAIN, 10));

            for (int i = 0; i <= lineas; i++) {

                int y = margenArriba + areaAlto * i / lineas;

                x.setColor(new Color(47, 59, 73));

                x.drawLine(margenIzq, y, margenIzq + areaAncho, y);

                double valorLinea = max * (lineas - i) / lineas;

                x.setColor(MUTED);

                String etiqueta = formatoCorto(valorLinea);

                int anchoTexto = x.getFontMetrics().stringWidth(etiqueta);

                x.drawString(
                        etiqueta,
                        margenIzq - 8 - anchoTexto,
                        y + 4
                );
            }

            // ---- Barras ----

            int columnas = valores.length;

            int paso = areaAncho / columnas;

            int anchoBarra = Math.max(12, Math.min(46, paso - 18));

            for (int i = 0; i < columnas; i++) {

                int alto = (int) (valores[i] / max * areaAlto);

                alto = Math.max(valores[i] > 0 ? 3 : 0, alto);

                int bx = margenIzq + i * paso + (paso - anchoBarra) / 2;

                int by = margenArriba + areaAlto - alto;

                x.setColor(GOLD);

                x.fillRoundRect(bx, by, anchoBarra, Math.max(2, alto), 6, 6);

                // Etiqueta del mes

                x.setColor(MUTED);

                x.setFont(f("SansSerif", Font.PLAIN, 10));

                int anchoMes = x.getFontMetrics().stringWidth(meses[i]);

                x.drawString(
                        meses[i],
                        bx + (anchoBarra - anchoMes) / 2,
                        margenArriba + areaAlto + 16
                );

                // Monto encima de la barra

                if (valores[i] > 0) {

                    x.setColor(TEXT);

                    x.setFont(f("SansSerif", Font.BOLD, 10));

                    String monto = "$" + formatoCorto(valores[i]);

                    int anchoMonto = x.getFontMetrics().stringWidth(monto);

                    x.drawString(
                            monto,
                            bx + (anchoBarra - anchoMonto) / 2,
                            Math.max(10, by - 5)
                    );
                }
            }

            x.dispose();
        }

        /** Redondea el máximo a 1000, 2000, 5000, 10000... */
        private double escalaBonita(double valor) {

            if (valor <= 0) {
                return 1000;
            }

            double magnitud = Math.pow(
                    10,
                    Math.floor(Math.log10(valor))
            );

            double normalizado = valor / magnitud;

            double techo;

            if (normalizado <= 1) {
                techo = 1;
            } else if (normalizado <= 2) {
                techo = 2;
            } else if (normalizado <= 5) {
                techo = 5;
            } else {
                techo = 10;
            }

            return techo * magnitud;
        }

        private String formatoCorto(double valor) {

            if (valor >= 1000) {

                return String.format("%,.0f", valor);
            }

            return String.format("%.0f", valor);
        }
    }

    // =========================================================
    // ICONOS DIBUJADOS EN JAVA
    // =========================================================

    private static class Icono
            implements Icon {

        static final int HOME = 1;
        static final int USER = 2;
        static final int CARD = 3;
        static final int MONEY = 4;
        static final int TRAINER = 5;
        static final int CHART = 6;
        static final int LOGOUT = 7;
        static final int DUMBBELL = 8;
        static final int MENU = 9;
        static final int USER_CIRCLE = 10;
        static final int DOWN = 11;
        static final int USER_GROUP = 12;
        static final int CHECK_USER = 13;
        static final int CALENDAR = 14;
        static final int PLUS = 15;
        static final int CHECK = 16;
        static final int BLOCK = 17;
        static final int REFRESH = 18;
        static final int GEAR = 19;
        static final int PALETTE = 20;
        static final int SHIELD = 21;
        static final int CLOUD = 22;
        static final int DATABASE = 23;
        static final int INFO = 24;
        static final int KEY = 25;
        static final int CLOCK = 26;
        static final int FOLDER = 27;
        static final int SLIDERS = 28;
        static final int SAVE = 29;
        static final int EYE = 30;
        static final int PENCIL = 31;
        static final int TRASH = 32;

        private final int type;
        private final int size;

        private Color color;

        Icono(
                int type,
                Color color,
                int size
        ) {

            this.type = type;
            this.color = color;
            this.size = size;
        }

        void setColor(
                Color color
        ) {

            this.color = color;
        }

        @Override
        public int getIconWidth() {

            return size;
        }

        @Override
        public int getIconHeight() {

            return size;
        }

        @Override
        public void paintIcon(
                Component c,
                Graphics g,
                int xx,
                int yy
        ) {

            Graphics2D x =
                    (Graphics2D)
                    g.create();

            x.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            x.setColor(color);

            x.setStroke(
                    new BasicStroke(
                            Math.max(
                                    1.5f,
                                    size / 9f
                            ),
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            double s =
                    size / 24.0;

            int X = xx;
            int Y = yy;

            switch (type) {

                case HOME:

                    Polygon h =
                            new Polygon();

                    h.addPoint(
                            X + (int) (12 * s),
                            Y + (int) (3 * s)
                    );

                    h.addPoint(
                            X + (int) (3 * s),
                            Y + (int) (11 * s)
                    );

                    h.addPoint(
                            X + (int) (5 * s),
                            Y + (int) (11 * s)
                    );

                    h.addPoint(
                            X + (int) (5 * s),
                            Y + (int) (21 * s)
                    );

                    h.addPoint(
                            X + (int) (19 * s),
                            Y + (int) (21 * s)
                    );

                    h.addPoint(
                            X + (int) (19 * s),
                            Y + (int) (11 * s)
                    );

                    h.addPoint(
                            X + (int) (21 * s),
                            Y + (int) (11 * s)
                    );

                    x.fill(h);

                    break;

                case USER:

                    x.fillOval(
                            X + (int) (8 * s),
                            Y + (int) (3 * s),
                            (int) (8 * s),
                            (int) (8 * s)
                    );

                    x.fillRoundRect(
                            X + (int) (4 * s),
                            Y + (int) (12 * s),
                            (int) (16 * s),
                            (int) (9 * s),
                            (int) (5 * s),
                            (int) (5 * s)
                    );

                    break;

                case USER_CIRCLE:

                    x.drawOval(
                            X + 1,
                            Y + 1,
                            size - 2,
                            size - 2
                    );

                    x.fillOval(
                            X + size / 3,
                            Y + size / 5,
                            size / 3,
                            size / 3
                    );

                    x.fillRoundRect(
                            X + size / 5,
                            Y + size / 2,
                            size * 3 / 5,
                            size * 2 / 5,
                            size / 3,
                            size / 3
                    );

                    break;

                case USER_GROUP:

                    x.fillOval(
                            X + (int) (2 * s),
                            Y + (int) (5 * s),
                            (int) (6 * s),
                            (int) (6 * s)
                    );

                    x.fillOval(
                            X + (int) (9 * s),
                            Y + (int) (3 * s),
                            (int) (7 * s),
                            (int) (7 * s)
                    );

                    x.fillOval(
                            X + (int) (17 * s),
                            Y + (int) (5 * s),
                            (int) (5 * s),
                            (int) (5 * s)
                    );

                    x.fillRoundRect(
                            X + (int) (1 * s),
                            Y + (int) (12 * s),
                            (int) (9 * s),
                            (int) (8 * s),
                            4,
                            4
                    );

                    x.fillRoundRect(
                            X + (int) (8 * s),
                            Y + (int) (11 * s),
                            (int) (10 * s),
                            (int) (10 * s),
                            5,
                            5
                    );

                    x.fillRoundRect(
                            X + (int) (16 * s),
                            Y + (int) (12 * s),
                            (int) (7 * s),
                            (int) (8 * s),
                            4,
                            4
                    );

                    break;

                case CHECK_USER:

                    x.drawOval(
                            X + (int) (3 * s),
                            Y + (int) (2 * s),
                            (int) (9 * s),
                            (int) (9 * s)
                    );

                    x.drawRoundRect(
                            X + (int) (1 * s),
                            Y + (int) (12 * s),
                            (int) (12 * s),
                            (int) (9 * s),
                            4,
                            4
                    );

                    x.drawLine(
                            X + (int) (14 * s),
                            Y + (int) (15 * s),
                            X + (int) (17 * s),
                            Y + (int) (18 * s)
                    );

                    x.drawLine(
                            X + (int) (17 * s),
                            Y + (int) (18 * s),
                            X + (int) (23 * s),
                            Y + (int) (11 * s)
                    );

                    break;

                case CARD:

                    x.drawRoundRect(
                            X + (int) (2 * s),
                            Y + (int) (5 * s),
                            (int) (20 * s),
                            (int) (14 * s),
                            2,
                            2
                    );

                    x.fillRect(
                            X + (int) (3 * s),
                            Y + (int) (8 * s),
                            (int) (18 * s),
                            (int) (3 * s)
                    );

                    break;

                case MONEY:

                    x.fillOval(
                            X + (int) (3 * s),
                            Y + (int) (3 * s),
                            (int) (18 * s),
                            (int) (18 * s)
                    );

                    break;

                case TRAINER:

                    x.drawOval(
                            X + (int) (8 * s),
                            Y + (int) (2 * s),
                            (int) (8 * s),
                            (int) (8 * s)
                    );

                    x.drawRoundRect(
                            X + (int) (5 * s),
                            Y + (int) (11 * s),
                            (int) (14 * s),
                            (int) (10 * s),
                            5,
                            5
                    );

                    break;

                case CHART:

                    x.fillRoundRect(
                            X + (int) (3 * s),
                            Y + (int) (13 * s),
                            (int) (4 * s),
                            (int) (8 * s),
                            2,
                            2
                    );

                    x.fillRoundRect(
                            X + (int) (10 * s),
                            Y + (int) (9 * s),
                            (int) (4 * s),
                            (int) (12 * s),
                            2,
                            2
                    );

                    x.fillRoundRect(
                            X + (int) (17 * s),
                            Y + (int) (4 * s),
                            (int) (4 * s),
                            (int) (17 * s),
                            2,
                            2
                    );

                    break;

                case LOGOUT:

                    x.drawRoundRect(
                            X + (int) (3 * s),
                            Y + (int) (3 * s),
                            (int) (10 * s),
                            (int) (18 * s),
                            2,
                            2
                    );

                    x.drawLine(
                            X + (int) (11 * s),
                            Y + (int) (12 * s),
                            X + (int) (22 * s),
                            Y + (int) (12 * s)
                    );

                    x.drawLine(
                            X + (int) (17 * s),
                            Y + (int) (7 * s),
                            X + (int) (22 * s),
                            Y + (int) (12 * s)
                    );

                    x.drawLine(
                            X + (int) (17 * s),
                            Y + (int) (17 * s),
                            X + (int) (22 * s),
                            Y + (int) (12 * s)
                    );

                    break;

                case DUMBBELL:

                    x.fillRoundRect(
                            X + (int) (1 * s),
                            Y + (int) (8 * s),
                            (int) (5 * s),
                            (int) (8 * s),
                            2,
                            2
                    );

                    x.fillRoundRect(
                            X + (int) (18 * s),
                            Y + (int) (8 * s),
                            (int) (5 * s),
                            (int) (8 * s),
                            2,
                            2
                    );

                    x.fillRect(
                            X + (int) (5 * s),
                            Y + (int) (10 * s),
                            (int) (14 * s),
                            (int) (4 * s)
                    );

                    x.fillRoundRect(
                            X + (int) (6 * s),
                            Y + (int) (5 * s),
                            (int) (4 * s),
                            (int) (14 * s),
                            2,
                            2
                    );

                    x.fillRoundRect(
                            X + (int) (14 * s),
                            Y + (int) (5 * s),
                            (int) (4 * s),
                            (int) (14 * s),
                            2,
                            2
                    );

                    break;

                case MENU:

                    for (
                            int i = 0;
                            i < 3;
                            i++
                    ) {

                        x.drawLine(
                                X + (int) (4 * s),
                                Y + (int) ((7 + i * 5) * s),
                                X + (int) (20 * s),
                                Y + (int) ((7 + i * 5) * s)
                        );
                    }

                    break;

                case DOWN:

                    x.drawLine(
                            X + (int) (5 * s),
                            Y + (int) (8 * s),
                            X + (int) (12 * s),
                            Y + (int) (15 * s)
                    );

                    x.drawLine(
                            X + (int) (12 * s),
                            Y + (int) (15 * s),
                            X + (int) (19 * s),
                            Y + (int) (8 * s)
                    );

                    break;

                case CALENDAR:

                    x.drawRoundRect(
                            X + (int) (3 * s),
                            Y + (int) (5 * s),
                            (int) (18 * s),
                            (int) (16 * s),
                            2,
                            2
                    );

                    x.drawLine(
                            X + (int) (3 * s),
                            Y + (int) (10 * s),
                            X + (int) (21 * s),
                            Y + (int) (10 * s)
                    );

                    x.drawLine(
                            X + (int) (8 * s),
                            Y + (int) (2 * s),
                            X + (int) (8 * s),
                            Y + (int) (7 * s)
                    );

                    x.drawLine(
                            X + (int) (16 * s),
                            Y + (int) (2 * s),
                            X + (int) (16 * s),
                            Y + (int) (7 * s)
                    );

                    break;

                case PLUS:

                    x.drawLine(
                            X + (int) (12 * s),
                            Y + (int) (5 * s),
                            X + (int) (12 * s),
                            Y + (int) (19 * s)
                    );

                    x.drawLine(
                            X + (int) (5 * s),
                            Y + (int) (12 * s),
                            X + (int) (19 * s),
                            Y + (int) (12 * s)
                    );

                    break;

                case CHECK:

                    x.drawLine(
                            X + (int) (4 * s),
                            Y + (int) (12 * s),
                            X + (int) (10 * s),
                            Y + (int) (18 * s)
                    );

                    x.drawLine(
                            X + (int) (10 * s),
                            Y + (int) (18 * s),
                            X + (int) (20 * s),
                            Y + (int) (6 * s)
                    );

                    break;

                case BLOCK:

                    x.drawOval(
                            X + (int) (3 * s),
                            Y + (int) (3 * s),
                            (int) (18 * s),
                            (int) (18 * s)
                    );

                    x.drawLine(
                            X + (int) (7 * s),
                            Y + (int) (7 * s),
                            X + (int) (17 * s),
                            Y + (int) (17 * s)
                    );

                    break;

                case REFRESH:

                    x.drawArc(
                            X + (int) (4 * s),
                            Y + (int) (4 * s),
                            (int) (16 * s),
                            (int) (16 * s),
                            45,
                            270
                    );

                    x.drawLine(
                            X + (int) (18 * s),
                            Y + (int) (5 * s),
                            X + (int) (20 * s),
                            Y + (int) (10 * s)
                    );

                    x.drawLine(
                            X + (int) (18 * s),
                            Y + (int) (5 * s),
                            X + (int) (13 * s),
                            Y + (int) (6 * s)
                    );

                    break;

                case GEAR:

                    x.drawOval(
                            X + (int) (8 * s),
                            Y + (int) (8 * s),
                            (int) (8 * s),
                            (int) (8 * s)
                    );

                    for (int i = 0; i < 8; i++) {

                        double ang = Math.toRadians(i * 45);

                        int cx = X + (int) (12 * s);
                        int cy = Y + (int) (12 * s);

                        int x1 = cx + (int) (7 * s * Math.cos(ang));
                        int y1 = cy + (int) (7 * s * Math.sin(ang));
                        int x2 = cx + (int) (10 * s * Math.cos(ang));
                        int y2 = cy + (int) (10 * s * Math.sin(ang));

                        x.drawLine(x1, y1, x2, y2);
                    }

                    break;

                case PALETTE:

                    x.drawArc(
                            X + (int) (2 * s),
                            Y + (int) (3 * s),
                            (int) (20 * s),
                            (int) (18 * s),
                            0,
                            360
                    );

                    x.fillOval(
                            X + (int) (7 * s),
                            Y + (int) (8 * s),
                            (int) (3 * s),
                            (int) (3 * s)
                    );

                    x.fillOval(
                            X + (int) (13 * s),
                            Y + (int) (7 * s),
                            (int) (3 * s),
                            (int) (3 * s)
                    );

                    x.fillOval(
                            X + (int) (16 * s),
                            Y + (int) (13 * s),
                            (int) (3 * s),
                            (int) (3 * s)
                    );

                    x.fillOval(
                            X + (int) (8 * s),
                            Y + (int) (15 * s),
                            (int) (3 * s),
                            (int) (3 * s)
                    );

                    break;

                case SHIELD:

                    Polygon sh = new Polygon();

                    sh.addPoint(X + (int) (12 * s), Y + (int) (2 * s));
                    sh.addPoint(X + (int) (20 * s), Y + (int) (5 * s));
                    sh.addPoint(X + (int) (20 * s), Y + (int) (12 * s));
                    sh.addPoint(X + (int) (12 * s), Y + (int) (22 * s));
                    sh.addPoint(X + (int) (4 * s), Y + (int) (12 * s));
                    sh.addPoint(X + (int) (4 * s), Y + (int) (5 * s));

                    x.drawPolygon(sh);

                    x.drawLine(
                            X + (int) (8 * s),
                            Y + (int) (12 * s),
                            X + (int) (11 * s),
                            Y + (int) (15 * s)
                    );

                    x.drawLine(
                            X + (int) (11 * s),
                            Y + (int) (15 * s),
                            X + (int) (17 * s),
                            Y + (int) (8 * s)
                    );

                    break;

                case CLOUD:

                    x.fillOval(
                            X + (int) (2 * s),
                            Y + (int) (10 * s),
                            (int) (8 * s),
                            (int) (7 * s)
                    );

                    x.fillOval(
                            X + (int) (8 * s),
                            Y + (int) (6 * s),
                            (int) (10 * s),
                            (int) (10 * s)
                    );

                    x.fillOval(
                            X + (int) (14 * s),
                            Y + (int) (10 * s),
                            (int) (8 * s),
                            (int) (7 * s)
                    );

                    x.fillRect(
                            X + (int) (6 * s),
                            Y + (int) (13 * s),
                            (int) (14 * s),
                            (int) (6 * s)
                    );

                    break;

                case DATABASE:

                    x.drawOval(
                            X + (int) (4 * s),
                            Y + (int) (2 * s),
                            (int) (16 * s),
                            (int) (6 * s)
                    );

                    x.drawLine(
                            X + (int) (4 * s), Y + (int) (5 * s),
                            X + (int) (4 * s), Y + (int) (19 * s)
                    );

                    x.drawLine(
                            X + (int) (20 * s), Y + (int) (5 * s),
                            X + (int) (20 * s), Y + (int) (19 * s)
                    );

                    x.drawArc(
                            X + (int) (4 * s), Y + (int) (10 * s),
                            (int) (16 * s), (int) (6 * s),
                            180, 180
                    );

                    x.drawArc(
                            X + (int) (4 * s), Y + (int) (16 * s),
                            (int) (16 * s), (int) (6 * s),
                            180, 180
                    );

                    break;

                case INFO:

                    x.drawOval(
                            X + (int) (2 * s),
                            Y + (int) (2 * s),
                            (int) (20 * s),
                            (int) (20 * s)
                    );

                    x.fillOval(
                            X + (int) (11 * s),
                            Y + (int) (6 * s),
                            (int) (2 * s),
                            (int) (2 * s)
                    );

                    x.drawLine(
                            X + (int) (12 * s), Y + (int) (11 * s),
                            X + (int) (12 * s), Y + (int) (17 * s)
                    );

                    break;

                case KEY:

                    x.drawOval(
                            X + (int) (2 * s),
                            Y + (int) (9 * s),
                            (int) (8 * s),
                            (int) (8 * s)
                    );

                    x.drawLine(
                            X + (int) (9 * s), Y + (int) (13 * s),
                            X + (int) (21 * s), Y + (int) (13 * s)
                    );

                    x.drawLine(
                            X + (int) (16 * s), Y + (int) (13 * s),
                            X + (int) (16 * s), Y + (int) (18 * s)
                    );

                    x.drawLine(
                            X + (int) (20 * s), Y + (int) (13 * s),
                            X + (int) (20 * s), Y + (int) (17 * s)
                    );

                    break;

                case CLOCK:

                    x.drawOval(
                            X + (int) (2 * s),
                            Y + (int) (2 * s),
                            (int) (20 * s),
                            (int) (20 * s)
                    );

                    x.drawLine(
                            X + (int) (12 * s), Y + (int) (12 * s),
                            X + (int) (12 * s), Y + (int) (6 * s)
                    );

                    x.drawLine(
                            X + (int) (12 * s), Y + (int) (12 * s),
                            X + (int) (17 * s), Y + (int) (14 * s)
                    );

                    break;

                case FOLDER:

                    x.drawRoundRect(
                            X + (int) (2 * s),
                            Y + (int) (6 * s),
                            (int) (20 * s),
                            (int) (14 * s),
                            (int) (2 * s),
                            (int) (2 * s)
                    );

                    x.drawLine(
                            X + (int) (2 * s), Y + (int) (7 * s),
                            X + (int) (9 * s), Y + (int) (7 * s)
                    );

                    x.drawLine(
                            X + (int) (9 * s), Y + (int) (7 * s),
                            X + (int) (11 * s), Y + (int) (4 * s)
                    );

                    x.drawLine(
                            X + (int) (11 * s), Y + (int) (4 * s),
                            X + (int) (16 * s), Y + (int) (4 * s)
                    );

                    x.drawLine(
                            X + (int) (16 * s), Y + (int) (4 * s),
                            X + (int) (18 * s), Y + (int) (6 * s)
                    );

                    break;

                case SLIDERS:

                    for (int i = 0; i < 3; i++) {

                        int yy2 = Y + (int) ((5 + i * 7) * s);

                        x.drawLine(
                                X + (int) (2 * s), yy2,
                                X + (int) (22 * s), yy2
                        );

                        int kx = X + (int) ((6 + i * 6) * s);

                        x.fillOval(
                                kx - (int) (2 * s),
                                yy2 - (int) (2 * s),
                                (int) (4 * s),
                                (int) (4 * s)
                        );
                    }

                    break;

                case SAVE:

                    x.drawRoundRect(
                            X + (int) (3 * s),
                            Y + (int) (2 * s),
                            (int) (18 * s),
                            (int) (20 * s),
                            (int) (2 * s),
                            (int) (2 * s)
                    );

                    x.drawRect(
                            X + (int) (7 * s),
                            Y + (int) (2 * s),
                            (int) (10 * s),
                            (int) (6 * s)
                    );

                    x.drawRect(
                            X + (int) (6 * s),
                            Y + (int) (13 * s),
                            (int) (12 * s),
                            (int) (7 * s)
                    );

                    break;

                case EYE:

                    x.drawArc(
                            X + (int) (1 * s), Y + (int) (7 * s),
                            (int) (10 * s), (int) (10 * s),
                            30, 120
                    );

                    x.drawArc(
                            X + (int) (13 * s), Y + (int) (7 * s),
                            (int) (10 * s), (int) (10 * s),
                            30, 120
                    );

                    x.drawArc(
                            X + (int) (1 * s), Y + (int) (7 * s),
                            (int) (22 * s), (int) (10 * s),
                            210, 120
                    );

                    x.fillOval(
                            X + (int) (10 * s), Y + (int) (10 * s),
                            (int) (4 * s), (int) (4 * s)
                    );

                    break;

                case PENCIL:

                    x.drawLine(
                            X + (int) (4 * s), Y + (int) (20 * s),
                            X + (int) (16 * s), Y + (int) (8 * s)
                    );

                    x.drawLine(
                            X + (int) (16 * s), Y + (int) (8 * s),
                            X + (int) (19 * s), Y + (int) (11 * s)
                    );

                    x.drawLine(
                            X + (int) (19 * s), Y + (int) (11 * s),
                            X + (int) (7 * s), Y + (int) (23 * s)
                    );

                    x.drawLine(
                            X + (int) (7 * s), Y + (int) (23 * s),
                            X + (int) (4 * s), Y + (int) (20 * s)
                    );

                    x.drawLine(
                            X + (int) (17 * s), Y + (int) (4 * s),
                            X + (int) (21 * s), Y + (int) (8 * s)
                    );

                    x.drawLine(
                            X + (int) (21 * s), Y + (int) (8 * s),
                            X + (int) (19 * s), Y + (int) (11 * s)
                    );

                    x.drawLine(
                            X + (int) (17 * s), Y + (int) (4 * s),
                            X + (int) (16 * s), Y + (int) (8 * s)
                    );

                    break;

                case TRASH:

                    x.drawLine(
                            X + (int) (5 * s), Y + (int) (7 * s),
                            X + (int) (19 * s), Y + (int) (7 * s)
                    );

                    x.drawLine(
                            X + (int) (9 * s), Y + (int) (7 * s),
                            X + (int) (9 * s), Y + (int) (4 * s)
                    );

                    x.drawLine(
                            X + (int) (9 * s), Y + (int) (4 * s),
                            X + (int) (15 * s), Y + (int) (4 * s)
                    );

                    x.drawLine(
                            X + (int) (15 * s), Y + (int) (4 * s),
                            X + (int) (15 * s), Y + (int) (7 * s)
                    );

                    x.drawRoundRect(
                            X + (int) (6 * s), Y + (int) (7 * s),
                            (int) (12 * s), (int) (14 * s),
                            (int) (2 * s), (int) (2 * s)
                    );

                    x.drawLine(
                            X + (int) (10 * s), Y + (int) (10 * s),
                            X + (int) (10 * s), Y + (int) (18 * s)
                    );

                    x.drawLine(
                            X + (int) (14 * s), Y + (int) (10 * s),
                            X + (int) (14 * s), Y + (int) (18 * s)
                    );

                    break;

                default:
                    break;
            }

            x.dispose();
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(
                () -> new MainFrame()
                        .setVisible(true)
        );
    }
}