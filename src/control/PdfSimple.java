package control;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador de PDF mínimo, sin librerías externas.
 *
 * Solo escribe texto en fuente Courier (monoespaciada, así las
 * columnas quedan alineadas) y reparte las líneas en páginas
 * tamaño carta, con numeración "Página X de Y" al pie.
 *
 * Uso:
 *   PdfSimple pdf = new PdfSimple();
 *   pdf.linea("Título", 16, true);
 *   pdf.linea("texto normal", 9, false);
 *   pdf.guardar(archivo);
 */
public class PdfSimple {

    private static final Charset WIN = Charset.forName("windows-1252");

    private static final float ANCHO = 612f;
    private static final float ALTO = 792f;
    private static final float MARGEN = 40f;

    /** Ancho de un carácter de Courier = 0.6 x tamaño de fuente. */
    private static final float FACTOR_ANCHO = 0.6f;

    private static class Linea {

        final String texto;
        final int tamano;
        final boolean negrita;

        Linea(String texto, int tamano, boolean negrita) {
            this.texto = texto;
            this.tamano = tamano;
            this.negrita = negrita;
        }
    }

    private final List<Linea> lineas = new ArrayList<>();

    /** Cuántos caracteres caben en una línea con ese tamaño de letra. */
    public static int maxCaracteres(int tamano) {

        return (int) ((ANCHO - 2 * MARGEN) / (tamano * FACTOR_ANCHO));
    }

    /** Agrega una línea de texto (sin salto automático). */
    public void linea(String texto, int tamano, boolean negrita) {

        lineas.add(new Linea(limpiar(texto), tamano, negrita));
    }

    /** Agrega una línea vacía. */
    public void espacio() {

        lineas.add(new Linea("", 9, false));
    }

    // ---------------------------------------------------------

    /** Deja solo caracteres que la fuente estándar puede mostrar. */
    private static String limpiar(String t) {

        if (t == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < t.length(); i++) {

            char c = t.charAt(i);

            if (c == '\n' || c == '\r' || c == '\t') {

                sb.append(' ');

            } else if (c < 32) {

                continue;

            } else if (WIN.newEncoder().canEncode(c)) {

                sb.append(c);

            } else {

                sb.append('?');
            }
        }

        return sb.toString();
    }

    private static String escapar(String t) {

        return t.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    // ---------------------------------------------------------

    /** Reparte las líneas en páginas y arma el contenido de cada una. */
    private List<List<Object[]>> paginar() {

        List<List<Object[]>> paginas = new ArrayList<>();

        List<Object[]> actual = new ArrayList<>();

        float y = ALTO - MARGEN;

        // Se deja espacio al pie para "Página X de Y".
        float limiteInferior = MARGEN + 14f;

        for (Linea l : lineas) {

            float alto = l.tamano * 1.35f;

            if (y - alto < limiteInferior && !actual.isEmpty()) {

                paginas.add(actual);

                actual = new ArrayList<>();

                y = ALTO - MARGEN;
            }

            y -= alto;

            actual.add(new Object[]{l, y});
        }

        paginas.add(actual);

        return paginas;
    }

    private static void escribir(ByteArrayOutputStream out, String s) {

        byte[] b = s.getBytes(WIN);

        out.write(b, 0, b.length);
    }

    public void guardar(File destino) throws IOException {

        List<List<Object[]>> paginas = paginar();

        int total = paginas.size();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<Integer> offsets = new ArrayList<>();

        escribir(out, "%PDF-1.4\n");

        // Objetos fijos: 1 = catálogo, 2 = páginas, 3 = Courier,
        // 4 = Courier-Bold. Luego, por cada página: un objeto de
        // página y otro con su contenido.

        offsets.add(out.size());
        escribir(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();

        for (int i = 0; i < total; i++) {

            kids.append(5 + 2 * i).append(" 0 R ");
        }

        offsets.add(out.size());
        escribir(out, "2 0 obj\n<< /Type /Pages /Kids [ " + kids
                + "] /Count " + total + " >>\nendobj\n");

        offsets.add(out.size());
        escribir(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 "
                + "/BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n");

        offsets.add(out.size());
        escribir(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 "
                + "/BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int i = 0; i < total; i++) {

            StringBuilder c = new StringBuilder();

            for (Object[] item : paginas.get(i)) {

                Linea l = (Linea) item[0];

                float y = (Float) item[1];

                if (l.texto.isEmpty()) {
                    continue;
                }

                c.append("BT /")
                        .append(l.negrita ? "F2" : "F1")
                        .append(' ').append(l.tamano)
                        .append(" Tf ")
                        .append(MARGEN).append(' ').append(y)
                        .append(" Td (")
                        .append(escapar(l.texto))
                        .append(") Tj ET\n");
            }

            // Pie de página
            c.append("BT /F1 8 Tf ")
                    .append(MARGEN).append(' ').append(MARGEN - 8)
                    .append(" Td (Página ").append(i + 1)
                    .append(" de ").append(total)
                    .append(") Tj ET\n");

            byte[] contenido = c.toString().getBytes(WIN);

            int idPagina = 5 + 2 * i;
            int idContenido = 6 + 2 * i;

            offsets.add(out.size());
            escribir(out, idPagina + " 0 obj\n<< /Type /Page /Parent 2 0 R "
                    + "/MediaBox [0 0 " + ANCHO + " " + ALTO + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> "
                    + "/Contents " + idContenido + " 0 R >>\nendobj\n");

            offsets.add(out.size());
            escribir(out, idContenido + " 0 obj\n<< /Length "
                    + contenido.length + " >>\nstream\n");
            out.write(contenido, 0, contenido.length);
            escribir(out, "endstream\nendobj\n");
        }

        int inicioXref = out.size();

        int cantidad = offsets.size() + 1;

        escribir(out, "xref\n0 " + cantidad + "\n");
        escribir(out, "0000000000 65535 f \n");

        for (int off : offsets) {

            escribir(out, String.format(java.util.Locale.ROOT, "%010d 00000 n \n", off));
        }

        escribir(out, "trailer\n<< /Size " + cantidad
                + " /Root 1 0 R >>\nstartxref\n" + inicioXref + "\n%%EOF\n");

        try (FileOutputStream fos = new FileOutputStream(destino)) {

            out.writeTo(fos);
        }
    }
}
