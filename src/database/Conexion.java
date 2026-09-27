package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto único de conexión a la base de datos MySQL/MariaDB (XAMPP).
 *
 * Ajusta estas 4 constantes si tu XAMPP usa otro puerto, usuario o
 * contraseña, o si la base de datos se llama distinto:
 */
public class Conexion {

    private static final String HOST = "localhost";
    private static final String PUERTO = "3306";
    private static final String BASE_DATOS = "golden_gym";

    private static final String USUARIO = "root";
    private static final String PASSWORD = "missa113";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS
            + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    /**
     * Abre una conexión nueva. Quien la llame es responsable de
     * cerrarla (usar try-with-resources).
     *
     * Lanza SQLException si XAMPP/MySQL no está encendido, si la
     * base de datos "golden_gym" no existe (falta correr el script
     * golden_gym.sql), o si el driver mysql-connector-j no está
     * agregado como librería del proyecto.
     */
    public static Connection obtener() throws SQLException {

        try {

            // Desde Java 6+ normalmente no hace falta esta línea,
            // pero se deja para versiones más viejas de NetBeans.
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException ex) {

            throw new SQLException(
                    "No se encontró el driver de MySQL "
                    + "(mysql-connector-j). Agrégalo como librería "
                    + "del proyecto en NetBeans.",
                    ex
            );
        }

        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    /**
     * Prueba rápida de conexión: true si se pudo conectar,
     * false si no (sin lanzar excepción).
     */
    public static boolean probar() {

        try (Connection c = obtener()) {

            return c != null && !c.isClosed();

        } catch (SQLException ex) {

            return false;
        }
    }
}
