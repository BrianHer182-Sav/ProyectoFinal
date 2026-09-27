package control;

import java.util.regex.Pattern;

public class RegexControl {

    // ==============================
    // NOMBRES
    // ==============================
    public static boolean nombre(String texto) {
        return Pattern.matches(
            "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{3,50}$",
            texto
        );
    }

    // ==============================
    // DNI
    // ==============================
    public static boolean dni(String texto) {
        return Pattern.matches(
            "^\\d{8}$",
            texto
        );
    }

    // ==============================
    // CORREO
    // ==============================
    public static boolean correo(String texto) {
        return Pattern.matches(
            "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$",
            texto
        );
    }

    // ==============================
    // TELEFONO
    // ==============================
    public static boolean telefono(String texto) {
        return Pattern.matches(
            "^\\d{10}$",
            texto
        );
    }

    // ==============================
    // FECHA
    // Formato: dd/MM/yyyy
    // ==============================
    public static boolean fecha(String texto) {
        return Pattern.matches(
            "^(0[1-9]|[12]\\d|3[01])/(0[1-9]|1[0-2])/\\d{4}$",
            texto
        );
    }

    // ==============================
    // PESO
    // Ejemplos: 70, 70.5, 85.25
    // ==============================
    public static boolean peso(String texto) {
        return Pattern.matches(
            "^\\d{2,3}(\\.\\d{1,2})?$",
            texto
        );
    }

    // ==============================
    // ALTURA
    // Ejemplos: 1.70, 1.75, 1.80
    // ==============================
    public static boolean altura(String texto) {
        return Pattern.matches(
            "^\\d\\.\\d{1,2}$",
            texto
        );
    }

    // ==============================
    // USUARIO
    // ==============================
    public static boolean usuario(String texto) {
        return Pattern.matches(
            "^[A-Za-z0-9_]{4,20}$",
            texto
        );
    }

    // ==============================
    // CONTRASEÑA
    // ==============================
    public static boolean password(String texto) {
        return password(texto, "Alto");
    }

    /**
     * Mismo chequeo de siempre, pero con 3 niveles de exigencia
     * según "Nivel de seguridad" en Configuración > Seguridad.
     */
    public static boolean password(String texto, String nivel) {

        if (texto == null) {
            return false;
        }

        if ("Bajo".equals(nivel)) {

            // Solo largo mínimo.
            return texto.length() >= 4;

        } else if ("Medio".equals(nivel)) {

            // Mínimo 6, al menos una letra y un número.
            return Pattern.matches(
                "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                texto
            );
        }

        // Alto (por defecto): mínimo 8, mayúscula, minúscula,
        // número y carácter especial.
        return Pattern.matches(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$",
            texto
        );
    }

    // ==============================
    // NUMERO ENTERO
    // ==============================
    public static boolean numero(String texto) {
        return Pattern.matches(
            "^\\d+$",
            texto
        );
    }

    // ==============================
    // SALARIO / PRECIO
    // Ejemplos: 500, 500.50
    // ==============================
    public static boolean dinero(String texto) {
        return Pattern.matches(
            "^\\d+(\\.\\d{1,2})?$",
            texto
        );
    }
}