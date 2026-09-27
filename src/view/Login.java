package view;

import control.RegexControl;

import javax.swing.*;
import java.awt.*;

public class Login extends JFrame {

    // ==========================================
    // CUENTAS EN MEMORIA (persisten mientras la app esté abierta)
    // ==========================================

    public static java.util.Map<String, String> CUENTAS =
            new java.util.LinkedHashMap<>();

    public static java.util.Map<String, String> PREGUNTAS =
            new java.util.LinkedHashMap<>();

    public static String USUARIO_ACTUAL = "admin";

    // "Modo de mantenimiento" (Configuración > Otras opciones):
    // si está activo, solo la cuenta "admin" puede entrar.
    public static boolean MODO_MANTENIMIENTO = false;

    static {
        CUENTAS.put("admin", "Admin123!");
        PREGUNTAS.put("admin", "gimnasio");
    }

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;

    public Login() {

        setTitle("Sistema de Gimnasio - Acceso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Antes la ventana no se podía redimensionar ni maximizar.
        // Con esto sí se puede maximizar/agrandar y el panel de
        // acceso se mantiene centrado y a su tamaño original.
        setResizable(true);

        crearInterfaz();

        // Antes se usaba setSize(450, 380) para TODA la ventana
        // (incluyendo el borde y la barra de título), así que el
        // área interior real quedaba más chica que 450x380 y el
        // formulario (con posiciones fijas) se recortaba: no se
        // veían los campos de usuario/contraseña hasta maximizar.
        // pack() calcula el tamaño de la ventana a partir del
        // tamaño preferido del contenido, así que el área interior
        // sí mide 450x380 completos desde que abre, sin recortes.
        pack();

        setMinimumSize(getSize());

        setLocationRelativeTo(null);
    }

    // ==========================================
    // PALETA (la misma azul marino del MainFrame)
    // ==========================================

    private static final Color BG = new Color(10, 18, 28);
    private static final Color PANEL = new Color(24, 35, 48);
    private static final Color INPUT = new Color(13, 24, 36);
    private static final Color BORDER = new Color(49, 65, 82);
    private static final Color GOLD = new Color(245, 183, 35);
    private static final Color GOLD_HOVER = new Color(255, 200, 70);
    private static final Color TEXT = new Color(241, 244, 248);
    private static final Color MUTED = new Color(164, 177, 191);

    // Ancho común de campos y botón: por eso quedan alineados y
    // centrados entre sí dentro de la tarjeta.
    private static final int ANCHO_FORM = 300;

    private javax.swing.border.Border bordeCampo(boolean enfocado) {

        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(enfocado ? GOLD : BORDER, 1),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        );
    }

    private void estilizarCampo(JTextField campo) {

        campo.setBackground(INPUT);
        campo.setForeground(TEXT);
        campo.setCaretColor(TEXT);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setAlignmentX(Component.CENTER_ALIGNMENT);
        campo.setPreferredSize(new Dimension(ANCHO_FORM, 40));
        campo.setMaximumSize(new Dimension(ANCHO_FORM, 40));
        campo.setBorder(bordeCampo(false));

        campo.addFocusListener(new java.awt.event.FocusAdapter() {

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                campo.setBorder(bordeCampo(true));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                campo.setBorder(bordeCampo(false));
            }
        });
    }

    /** Etiqueta pequeña sobre cada campo, alineada al borde del campo. */
    private JPanel filaCampo(String etiqueta, JTextField campo) {

        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.Y_AXIS));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(MUTED);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        fila.add(lbl);
        fila.add(campo);

        fila.setPreferredSize(new Dimension(ANCHO_FORM, 66));
        fila.setMaximumSize(new Dimension(ANCHO_FORM, 66));

        return fila;
    }

    private void crearInterfaz() {

        // ---- Tarjeta central ----

        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(PANEL);
        tarjeta.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        BorderFactory.createEmptyBorder(36, 40, 32, 40)
                )
        );

        JLabel titulo = new JLabel("SISTEMA DE GIMNASIO");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 21));
        titulo.setForeground(GOLD);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Inicia sesión para continuar");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitulo.setForeground(MUTED);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUsuario = new JTextField();
        estilizarCampo(txtUsuario);

        txtPassword = new JPasswordField();
        estilizarCampo(txtPassword);

        // Botón INGRESAR: mismo ancho que los campos, centrado.
        btnIngresar = new JButton("INGRESAR") {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(getModel().isRollover() ? GOLD_HOVER : GOLD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btnIngresar.setForeground(new Color(20, 20, 20));
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnIngresar.setContentAreaFilled(false);
        btnIngresar.setOpaque(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setRolloverEnabled(true);
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnIngresar.setPreferredSize(new Dimension(ANCHO_FORM, 44));
        btnIngresar.setMaximumSize(new Dimension(ANCHO_FORM, 44));

        btnIngresar.addActionListener(e -> iniciarSesion());

        JButton btnOlvide = new JButton("¿Olvidaste tu contraseña?");
        btnOlvide.setForeground(MUTED);
        btnOlvide.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnOlvide.setBorderPainted(false);
        btnOlvide.setContentAreaFilled(false);
        btnOlvide.setFocusPainted(false);
        btnOlvide.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOlvide.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnOlvide.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnOlvide.setForeground(GOLD);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnOlvide.setForeground(MUTED);
            }
        });

        btnOlvide.addActionListener(e -> recuperarPassword());

        tarjeta.add(titulo);
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(subtitulo);
        tarjeta.add(Box.createVerticalStrut(30));
        tarjeta.add(filaCampo("Usuario", txtUsuario));
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(filaCampo("Contraseña", txtPassword));
        tarjeta.add(Box.createVerticalStrut(26));
        tarjeta.add(btnIngresar);
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(btnOlvide);

        tarjeta.setPreferredSize(
                new Dimension(400, tarjeta.getPreferredSize().height)
        );

        // ---- Fondo azul marino que centra la tarjeta ----
        // Al maximizar la ventana la tarjeta se queda al centro y el
        // fondo llena todo el marco.

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG);
        wrapper.add(tarjeta);

        wrapper.setPreferredSize(
                new Dimension(520, tarjeta.getPreferredSize().height + 120)
        );

        add(wrapper);
    }

    private void recuperarPassword() {

        JTextField usuarioField = new JTextField();
        JTextField respuestaField = new JTextField();
        JPasswordField nuevaField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 6));

        panel.add(new JLabel("Usuario:"));
        panel.add(usuarioField);
        panel.add(new JLabel("Respuesta secreta (definida al crear la cuenta):"));
        panel.add(respuestaField);
        panel.add(new JLabel("Nueva contraseña:"));
        panel.add(nuevaField);

        int r = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Recuperar contraseña",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (r != JOptionPane.OK_OPTION) {
            return;
        }

        String usuario = usuarioField.getText().trim();
        String respuesta = respuestaField.getText().trim();
        String nueva = new String(nuevaField.getPassword());

        if (!CUENTAS.containsKey(usuario)) {

            JOptionPane.showMessageDialog(
                    this,
                    "No existe una cuenta con ese usuario.",
                    "Recuperación fallida",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String respuestaGuardada = PREGUNTAS.getOrDefault(usuario, "");

        if (!respuestaGuardada.equalsIgnoreCase(respuesta)) {

            JOptionPane.showMessageDialog(
                    this,
                    "La respuesta secreta no coincide.",
                    "Recuperación fallida",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (!RegexControl.password(nueva)) {

            JOptionPane.showMessageDialog(
                    this,
                    "La nueva contraseña debe tener mínimo 8 caracteres,\n"
                    + "una mayúscula, una minúscula, un número y un\n"
                    + "carácter especial.",
                    "Contraseña inválida",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        CUENTAS.put(usuario, nueva);

        JOptionPane.showMessageDialog(
                this,
                "Contraseña actualizada. Ya puedes iniciar sesión.",
                "Listo",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void iniciarSesion() {

        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        // ==========================================
        // VALIDACION CON REGEX
        // ==========================================

        if (!RegexControl.usuario(usuario)) {

            JOptionPane.showMessageDialog(
                this,
                "El usuario debe tener entre 4 y 20 caracteres.\n"
                + "Solo se permiten letras, números y _.",
                "Usuario inválido",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (!RegexControl.password(password)) {

            JOptionPane.showMessageDialog(
                this,
                "La contraseña debe contener:\n"
                + "- Mínimo 8 caracteres\n"
                + "- Una letra mayúscula\n"
                + "- Una letra minúscula\n"
                + "- Un número\n"
                + "- Un carácter especial",
                "Contraseña inválida",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // ==========================================
        // MODO DE MANTENIMIENTO
        // ==========================================

        if (MODO_MANTENIMIENTO && !"admin".equals(usuario)) {

            JOptionPane.showMessageDialog(
                this,
                "El sistema está en modo de mantenimiento. "
                + "Solo la cuenta administradora puede acceder "
                + "por ahora.",
                "Mantenimiento",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // ==========================================
        // CREDENCIALES DEL SISTEMA
        // ==========================================

        if (CUENTAS.containsKey(usuario) && CUENTAS.get(usuario).equals(password)) {

            USUARIO_ACTUAL = usuario;

            new MainFrame().setVisible(true);

            dispose();

        } else {

            JOptionPane.showMessageDialog(
                this,
                "Usuario o contraseña incorrectos.",
                "Acceso denegado",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new Login().setVisible(true);
        });
    }
}
