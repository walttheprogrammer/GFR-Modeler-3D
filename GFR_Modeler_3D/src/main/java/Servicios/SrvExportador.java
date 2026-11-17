package Servicios;
import Modelo.CotaGraduacion;
import Modelo.ResultadoCalculo;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.imageio.ImageIO;

public class SrvExportador {
    public static void exportarCSV(File archivo, ResultadoCalculo res) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            pw.println("Nivel_ml;Altura_cm;Radio_cm;Area_cm2;Material_cm3;Costo_Soles");
            if (res.cotas != null) {
                for (CotaGraduacion c : res.cotas) {
                    pw.printf(Locale.US, "%.2f;%.4f;%.4f;%.4f;%.4f;%.2f%n",
                        c.getVolumenMl(), c.getAlturaCm(), c.getRadioCm(),
                        c.getAreaAcumuladaCm2(), c.getResinaAcumuladaCm3(), c.getCostoAcumulado());
                }
            }
        }
    }

    public static void exportarPNG(File archivo, BufferedImage img2D) throws IOException {
        if (img2D == null) throw new IOException("No hay gráfica generada para exportar.");
        ImageIO.write(img2D, "PNG", archivo);
    }

    public static void exportarSTL(File archivo, SrvMotorCalculo motor, double alturaH, double espesorE) throws IOException {
        int nTheta = 48;
        int nZ = 30;
        double zMin = motor.evaluarZ(0, 0);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            pw.println("solid GFR_Modeler_3D_Envase");
            for (int iz = 0; iz < nZ; iz++) {
                double z0 = zMin + (alturaH - zMin) * iz / nZ;
                double z1 = zMin + (alturaH - zMin) * (iz + 1) / nZ;

                for (int it = 0; it < nTheta; it++) {
                    double t0 = 2.0 * Math.PI * it / nTheta;
                    double t1 = 2.0 * Math.PI * (it + 1) / nTheta;

                    double r00 = motor.hallarRadioEnNivel(z0, t0) + espesorE;
                    double r01 = motor.hallarRadioEnNivel(z0, t1) + espesorE;
                    double r10 = motor.hallarRadioEnNivel(z1, t0) + espesorE;
                    double r11 = motor.hallarRadioEnNivel(z1, t1) + espesorE;

                    double[] p00 = {r00 * Math.cos(t0), r00 * Math.sin(t0), z0};
                    double[] p01 = {r01 * Math.cos(t1), r01 * Math.sin(t1), z0};
                    double[] p10 = {r10 * Math.cos(t0), r10 * Math.sin(t0), z1};
                    double[] p11 = {r11 * Math.cos(t1), r11 * Math.sin(t1), z1};

                    escribirTrianguloSTL(pw, p00, p01, p11);
                    escribirTrianguloSTL(pw, p00, p11, p10);
                }
            }
            pw.println("endsolid GFR_Modeler_3D_Envase");
        }
    }

    private static void escribirTrianguloSTL(PrintWriter pw, double[] a, double[] b, double[] c) {
        pw.println("  facet normal 0.0 0.0 1.0");
        pw.println("    outer loop");
        pw.printf(Locale.US, "      vertex %.5f %.5f %.5f%n", a[0], a[1], a[2]);
        pw.printf(Locale.US, "      vertex %.5f %.5f %.5f%n", b[0], b[1], b[2]);
        pw.printf(Locale.US, "      vertex %.5f %.5f %.5f%n", c[0], c[1], c[2]);
        pw.println("    endloop");
        pw.println("  endfacet");
    }

    public static void exportarDXF(File archivo, SrvMotorCalculo motor, ResultadoCalculo res) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            pw.println("0\nSECTION\n2\nENTITIES");

            // Capa 1: Curvas de nivel en XY
            if (res.cotas != null) {
                for (CotaGraduacion c : res.cotas) {
                    pw.printf(Locale.US, "0\nCIRCLE\n8\nCURVAS_NIVEL\n10\n0.0\n20\n0.0\n30\n%.4f\n40\n%.4f\n",
                            c.getAlturaCm(), c.getRadioCm());
                }
            }

            // Capa 2: Perfil generatriz en XZ
            double zMin = motor.evaluarZ(0, 0);
            for (int i = 0; i < 30; i++) {
                double z0 = zMin + (res.alturaIngresada - zMin) * i / 30.0;
                double z1 = zMin + (res.alturaIngresada - zMin) * (i + 1) / 30.0;
                double r0 = motor.hallarRadioEnNivel(z0, 0);
                double r1 = motor.hallarRadioEnNivel(z1, 0);

                pw.printf(Locale.US, "0\nLINE\n8\nPERFIL_ENVASE\n10\n%.4f\n20\n%.4f\n30\n0.0\n11\n%.4f\n21\n%.4f\n31\n0.0\n",
                        r0, z0, r1, z1);
            }
            pw.println("0\nENDSEC\n0\nEOF");
        }
    }

    public static void exportarPDF(File archivo, String nombreProyecto, ResultadoCalculo res,
                                   BufferedImage img2D, BufferedImage img3D) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();

        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(163, 53, 53));
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        Paragraph titulo = new Paragraph("GFR MODELER 3D — REPORTE TÉCNICO DE INGENIERÍA", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);
        doc.add(new Paragraph("Proyecto: " + nombreProyecto + "   |   Ecuación: z = " + res.funcion, subFont));
        doc.add(Chunk.NEWLINE);

        // Tabla resumen ejecutivo
        PdfPTable tablaResumen = new PdfPTable(2);
        tablaResumen.setWidthPercentage(100);
        agregarFila(tablaResumen, "Altura (h)", String.format("%.2f cm", res.alturaIngresada), normalFont);
        agregarFila(tablaResumen, "Espesor (e)", String.format("%.2f cm", res.espesor), normalFont);
        agregarFila(tablaResumen, "Capacidad volumétrica V(h)", String.format("%.2f ml (Meta: %.2f ml)", res.volumenCalculado, res.volumenObjetivo), normalFont);
        agregarFila(tablaResumen, "Área superficial de pared", String.format("%.2f cm²", res.areaPared), normalFont);
        agregarFila(tablaResumen, "Consumo estimado del material", String.format("%.2f cm³ (%.2f g)", res.volumenMaterial, res.masaGramos), normalFont);
        agregarFila(tablaResumen, "Costo Unitario estimado", String.format("S/ %.2f", res.costoTotal), normalFont);
        agregarFila(tablaResumen, "Ángulo de Desmolde (∇f)", String.format("%.2f° (%s)", res.anguloDesmoldeGrados, res.desmoldable ? "OK" : "Revisar"), normalFont);
        doc.add(tablaResumen);
        doc.add(Chunk.NEWLINE);

        // Tabla de cotas de graduación
        doc.add(new Paragraph("Tabla de Marcas de Graduación", subFont));
        doc.add(new Paragraph(" "));
        PdfPTable tablaCotas = new PdfPTable(6);
        tablaCotas.setWidthPercentage(100);
        String[] headers = {"Nivel (ml)", "Altura (cm)", "Radio (cm)", "Área (cm²)", "Insumo (cm³)", "Costo (S/)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(new Color(54, 54, 54));
            tablaCotas.addCell(cell);
        }
        if (res.cotas != null) {
            for (CotaGraduacion c : res.cotas) {
                tablaCotas.addCell(new Phrase(String.format("%.0f", c.getVolumenMl()), normalFont));
                tablaCotas.addCell(new Phrase(String.format("%.2f", c.getAlturaCm()), normalFont));
                tablaCotas.addCell(new Phrase(String.format("%.2f", c.getRadioCm()), normalFont));
                tablaCotas.addCell(new Phrase(String.format("%.2f", c.getAreaAcumuladaCm2()), normalFont));
                tablaCotas.addCell(new Phrase(String.format("%.2f", c.getResinaAcumuladaCm3()), normalFont));
                tablaCotas.addCell(new Phrase(String.format("%.2f", c.getCostoAcumulado()), normalFont));
            }
        }
        doc.add(tablaCotas);
        doc.add(Chunk.NEWLINE);

        // Gráficas incrustadas
        if (img2D != null) {
            Image pdfImg2D = Image.getInstance(img2D, null);
            pdfImg2D.scaleToFit(480, 240);
            pdfImg2D.setAlignment(Element.ALIGN_CENTER);
            doc.add(pdfImg2D);
        }
        if (img3D != null) {
            Image pdfImg3D = Image.getInstance(img3D, null);
            pdfImg3D.scaleToFit(480, 240);
            pdfImg3D.setAlignment(Element.ALIGN_CENTER);
            doc.add(pdfImg3D);
        }

        doc.close();
    }

    private static void agregarFila(PdfPTable tabla, String k, String v, Font font) {
        tabla.addCell(new Phrase(k, font));
        tabla.addCell(new Phrase(v, font));
    }
}
