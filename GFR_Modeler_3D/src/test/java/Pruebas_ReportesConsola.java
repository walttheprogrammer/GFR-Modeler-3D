import Controladores.*;
import Modelo.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class Pruebas_ReportesConsola {
    private CtrlMotorCalculo motor;
    private Material vidrio;
    private ResultadoCalculo resultado;

    @BeforeEach
    void setUp() {
        motor = new CtrlMotorCalculo();
        vidrio = new Material(1, "Vidrio Templado", 2.50, 0.18);
        resultado = motor.ejecutarAnalisisCompleto("0.25 * (x^2 + y^2)", 7.5, 0.20, 350.0, vidrio);
    }

    @Test
    @DisplayName("Títulos limpios sin prefijos [A], [B], [C] ni C_")
    void testTitulosLimpios() {
        assertTrue(resultado.reporteA.startsWith("=== CÁLCULO VOLUMÉTRICO Y CUBICACIÓN ==="),
                "El reporte A debe iniciar exactamente con '=== CÁLCULO VOLUMÉTRICO Y CUBICACIÓN ==='");
        assertTrue(resultado.reporteB.startsWith("=== ANÁLISIS DE GRADIENTE Y DESMOLDE ==="),
                "El reporte B debe iniciar exactamente con '=== ANÁLISIS DE GRADIENTE Y DESMOLDE ==='");
        assertTrue(resultado.reporteC.startsWith("=== OPTIMIZACIÓN Y COMPARATIVA CAD ==="),
                "El reporte C debe iniciar exactamente con '=== OPTIMIZACIÓN Y COMPARATIVA CAD ==='");

        assertFalse(resultado.reporteA.contains("[A]"), "Reporte A no debe contener '[A]'");
        assertFalse(resultado.reporteA.contains("[B]"), "Reporte A no debe contener '[B]'");
        assertFalse(resultado.reporteA.contains("[C]"), "Reporte A no debe contener '[C]'");

        assertFalse(resultado.reporteB.contains("[A]"), "Reporte B no debe contener '[A]'");
        assertFalse(resultado.reporteB.contains("[B]"), "Reporte B no debe contener '[B]'");
        assertFalse(resultado.reporteB.contains("[C]"), "Reporte B no debe contener '[C]'");

        assertFalse(resultado.reporteC.contains("[A]"), "Reporte C no debe contener '[A]'");
        assertFalse(resultado.reporteC.contains("[B]"), "Reporte C no debe contener '[B]'");
        assertFalse(resultado.reporteC.contains("[C]"), "Reporte C no debe contener '[C]'");
        assertFalse(resultado.reporteC.contains("C_"), "Reporte C no debe contener 'C_'");
    }

    @Test
    @DisplayName("Narrativa técnica en Reporte A (Cubicación) de mínimo 4 líneas con valores")
    void testNarrativaReporteA() {
        assertTrue(resultado.reporteA.contains("Diagnóstico Técnico:"), "El reporte A debe incluir sección de diagnóstico técnico");
        
        String parteNarrativa = resultado.reporteA.substring(resultado.reporteA.indexOf("Diagnóstico Técnico:"));
        String[] lineas = parteNarrativa.split("\n");
        assertTrue(lineas.length >= 5, "La narrativa del reporte A debe contener al menos 4 líneas técnicas interpretativas");

        assertTrue(resultado.reporteA.contains("ml"), "La narrativa de cubicación debe evaluar volúmenes en ml");
        assertTrue(resultado.reporteA.contains("cm³"), "La narrativa debe evaluar el consumo de insumo en cm³");
        assertTrue(resultado.reporteA.contains("S/"), "La narrativa debe evaluar el costo unitario de producción");
    }

    @Test
    @DisplayName("Narrativa técnica en Reporte B (Gradiente/Desmolde) de mínimo 4 líneas con valores")
    void testNarrativaReporteB() {
        assertTrue(resultado.reporteB.contains("Diagnóstico Técnico:"), "El reporte B debe incluir sección de diagnóstico técnico");

        String parteNarrativa = resultado.reporteB.substring(resultado.reporteB.indexOf("Diagnóstico Técnico:"));
        String[] lineas = parteNarrativa.split("\n");
        assertTrue(lineas.length >= 5, "La narrativa del reporte B debe contener al menos 4 líneas técnicas interpretativas");

        assertTrue(resultado.reporteB.contains("3.0°"), "La narrativa de desmolde debe contrastar contra el umbral de 3.0°");
        assertTrue(resultado.reporteB.contains("molde") || resultado.reporteB.contains("herramental"), 
                "La narrativa debe interpretar el comportamiento frente a fricción y molde");
    }

    @Test
    @DisplayName("Narrativa técnica en Reporte C (Lagrange/CAD) de mínimo 4 líneas con valores")
    void testNarrativaReporteC() {
        assertTrue(resultado.reporteC.contains("Diagnóstico Técnico:"), "El reporte C debe incluir sección de diagnóstico técnico");

        String parteNarrativa = resultado.reporteC.substring(resultado.reporteC.indexOf("Diagnóstico Técnico:"));
        String[] lineas = parteNarrativa.split("\n");
        assertTrue(lineas.length >= 5, "La narrativa del reporte C debe contener al menos 4 líneas técnicas interpretativas");

        assertTrue(resultado.reporteC.contains("Lagrange"), "La narrativa debe interpretar el modelo de Lagrange");
        assertTrue(resultado.reporteC.contains("cilindro") || resultado.reporteC.contains("tronco de cono"),
                "La narrativa debe contrastar contra aproximaciones de taller");
    }

    @Test
    @DisplayName("Consolidación en 'Analizar todo' sin prefijos [A], [B], [C]")
    void testAnalizarTodoConsolidado() {
        String consolidado = resultado.reporteA + "\n\n" + resultado.reporteB + "\n\n" + resultado.reporteC;
        assertFalse(consolidado.contains("[A]"));
        assertFalse(consolidado.contains("[B]"));
        assertFalse(consolidado.contains("[C]"));
        assertFalse(consolidado.contains("C_"));
    }
}
