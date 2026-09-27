package control;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Cliente SMTP mínimo escrito solo con las clases estándar de Java.
 *
 * Sustituye a la librería JavaMail (javax.mail), que no está incluida
 * en el JDK y provocaba los errores "package javax.mail does not exist".
 *
 * Soporta:
 *   - Puerto 587 con STARTTLS (Gmail, Outlook, etc.)
 *   - Puerto 465 con SSL directo
 *   - Autenticación AUTH LOGIN
 *   - Mensaje de texto plano, con o sin un archivo adjunto
 *
 * Nota para Gmail: hay que usar una "contraseña de aplicación",
 * no la contraseña normal de la cuenta.
 */
public class CorreoSimple {

    private Socket socket;
    private BufferedReader entrada;
    private Writer salida;

    // =====================================================
    // MÉTODO PRINCIPAL
    // =====================================================

    /**
     * Envía un correo. Lanza una excepción si algo falla.
     *
     * @param host        servidor SMTP (ej. smtp.gmail.com)
     * @param puerto      puerto ("587" o "465")
     * @param usuario     correo remitente
     * @param password    contraseña o contraseña de aplicación
     * @param nombreDe    nombre visible del remitente (puede ser null)
     * @param destino     correo destinatario
     * @param asunto      asunto del mensaje
     * @param cuerpo      texto del mensaje
     * @param adjunto     archivo a adjuntar (puede ser null)
     * @param nombreAdjunto nombre con el que se verá el adjunto
     */
    public static void enviar(
            String host,
            String puerto,
            String usuario,
            String password,
            String nombreDe,
            String destino,
            String asunto,
            String cuerpo,
            File adjunto,
            String nombreAdjunto
    ) throws IOException {

        CorreoSimple c = new CorreoSimple();

        int puertoNumero;

        try {
            puertoNumero = Integer.parseInt(puerto.trim());
        } catch (NumberFormatException ex) {
            puertoNumero = 587;
        }

        try {

            c.conectar(host.trim(), puertoNumero);

            c.autenticar(usuario.trim(), password);

            c.enviarMensaje(
                    usuario.trim(),
                    nombreDe,
                    destino.trim(),
                    asunto,
                    cuerpo,
                    adjunto,
                    nombreAdjunto
            );

        } finally {

            c.cerrar();
        }
    }

    // =====================================================
    // CONEXIÓN (con STARTTLS o SSL directo)
    // =====================================================

    private void conectar(String host, int puerto) throws IOException {

        if (puerto == 465) {

            socket = javax.net.ssl.SSLSocketFactory.getDefault()
                    .createSocket(host, puerto);

            abrirFlujos();

            leerRespuesta("220");

            escribirComando("EHLO gymsystem");
            leerRespuesta("250");

            return;
        }

        socket = new Socket(host, puerto);

        abrirFlujos();

        leerRespuesta("220");

        escribirComando("EHLO gymsystem");
        leerRespuesta("250");

        escribirComando("STARTTLS");
        leerRespuesta("220");

        socket = ((javax.net.ssl.SSLSocketFactory)
                javax.net.ssl.SSLSocketFactory.getDefault())
                .createSocket(socket, host, puerto, true);

        abrirFlujos();

        escribirComando("EHLO gymsystem");
        leerRespuesta("250");
    }

    private void abrirFlujos() throws IOException {

        socket.setSoTimeout(30000);

        entrada = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        salida = new OutputStreamWriter(
                socket.getOutputStream(),
                StandardCharsets.UTF_8
        );
    }

    // =====================================================
    // AUTENTICACIÓN
    // =====================================================

    private void autenticar(String usuario, String password)
            throws IOException {

        escribirComando("AUTH LOGIN");
        leerRespuesta("334");

        escribirComando(base64(usuario));
        leerRespuesta("334");

        escribirComando(base64(password));
        leerRespuesta("235");
    }

    // =====================================================
    // ENVÍO DEL MENSAJE
    // =====================================================

    private void enviarMensaje(
            String de,
            String nombreDe,
            String destino,
            String asunto,
            String cuerpo,
            File adjunto,
            String nombreAdjunto
    ) throws IOException {

        escribirComando("MAIL FROM:<" + de + ">");
        leerRespuesta("250");

        escribirComando("RCPT TO:<" + destino + ">");
        leerRespuesta("250");

        escribirComando("DATA");
        leerRespuesta("354");

        StringBuilder m = new StringBuilder();

        String remitente =
                (nombreDe == null || nombreDe.trim().isEmpty())
                        ? "<" + de + ">"
                        : codificarCabecera(nombreDe) + " <" + de + ">";

        m.append("From: ").append(remitente).append("\r\n");
        m.append("To: <").append(destino).append(">\r\n");
        m.append("Subject: ").append(codificarCabecera(asunto)).append("\r\n");
        m.append("MIME-Version: 1.0\r\n");

        if (adjunto == null || !adjunto.exists()) {

            m.append("Content-Type: text/plain; charset=UTF-8\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n");
            m.append("\r\n");

            m.append(
                    partirLineas(
                            Base64.getEncoder().encodeToString(
                                    cuerpo.getBytes(StandardCharsets.UTF_8)
                            )
                    )
            );

        } else {

            String limite = "----GymSystem" + System.currentTimeMillis();

            m.append("Content-Type: multipart/mixed; boundary=\"")
                    .append(limite).append("\"\r\n");

            m.append("\r\n");

            // --- parte de texto ---

            m.append("--").append(limite).append("\r\n");
            m.append("Content-Type: text/plain; charset=UTF-8\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n");
            m.append("\r\n");

            m.append(
                    partirLineas(
                            Base64.getEncoder().encodeToString(
                                    cuerpo.getBytes(StandardCharsets.UTF_8)
                            )
                    )
            );

            m.append("\r\n");

            // --- parte del archivo adjunto ---

            String nombre =
                    (nombreAdjunto == null || nombreAdjunto.trim().isEmpty())
                            ? adjunto.getName()
                            : nombreAdjunto;

            byte[] datos = Files.readAllBytes(adjunto.toPath());

            m.append("--").append(limite).append("\r\n");
            m.append("Content-Type: application/octet-stream; name=\"")
                    .append(nombre).append("\"\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n");
            m.append("Content-Disposition: attachment; filename=\"")
                    .append(nombre).append("\"\r\n");
            m.append("\r\n");

            m.append(
                    partirLineas(
                            Base64.getEncoder().encodeToString(datos)
                    )
            );

            m.append("\r\n");

            m.append("--").append(limite).append("--\r\n");
        }

        // Protección: una línea que empiece con "." termina el DATA.

        String texto = m.toString().replace("\r\n.", "\r\n..");

        salida.write(texto);
        salida.write("\r\n.\r\n");
        salida.flush();

        leerRespuesta("250");

        escribirComando("QUIT");
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private void escribirComando(String comando) throws IOException {

        salida.write(comando + "\r\n");
        salida.flush();
    }

    /**
     * Lee la respuesta del servidor (incluidas las multilínea del EHLO)
     * y verifica que empiece con el código esperado.
     */
    private String leerRespuesta(String codigoEsperado) throws IOException {

        StringBuilder todo = new StringBuilder();

        String linea;

        while ((linea = entrada.readLine()) != null) {

            todo.append(linea).append("\n");

            // Las líneas intermedias llevan '-' en la 4ª posición.
            if (linea.length() < 4 || linea.charAt(3) != '-') {
                break;
            }
        }

        String respuesta = todo.toString().trim();

        if (linea == null) {
            throw new IOException("El servidor cerró la conexión.");
        }

        if (!linea.startsWith(codigoEsperado)) {

            throw new IOException(
                    "Respuesta inesperada del servidor (se esperaba "
                    + codigoEsperado + "): " + respuesta
            );
        }

        return respuesta;
    }

    private void cerrar() {

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignorado) {
        }
    }

    private static String base64(String texto) {

        return Base64.getEncoder().encodeToString(
                texto.getBytes(StandardCharsets.UTF_8)
        );
    }

    /** Codifica acentos y ñ en asunto/nombre según RFC 2047. */
    private static String codificarCabecera(String texto) {

        if (texto == null) {
            return "";
        }

        boolean soloAscii = true;

        for (int i = 0; i < texto.length(); i++) {

            if (texto.charAt(i) > 127) {
                soloAscii = false;
                break;
            }
        }

        if (soloAscii) {
            return texto;
        }

        return "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(
                        texto.getBytes(StandardCharsets.UTF_8)
                )
                + "?=";
    }

    /** Parte el base64 en líneas de 76 caracteres, como pide el MIME. */
    private static String partirLineas(String base64) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < base64.length(); i += 76) {

            sb.append(
                    base64, i, Math.min(i + 76, base64.length())
            ).append("\r\n");
        }

        return sb.toString();
    }
}
