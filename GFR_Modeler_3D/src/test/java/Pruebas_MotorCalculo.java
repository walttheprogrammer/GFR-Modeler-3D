import Controladores.CtrlMotorCalculo;
import Modelo.Material;
import Modelo.ResultadoCalculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class Pruebas_MotorCalculo {

    private CtrlMotorCalculo motor;
    private Material vidrio;

    @BeforeEach
    void setUp() {
        motor = new CtrlMotorCalculo();
        vidrio = new Material(1, "Vidrio Templado", 2.50, 0.18);
    }

    @Test
    @DisplayName("Caso analítico vaso 350 ml con z = 0.5 * (x^2 + y^2)")
    void testCasoAnaliticoVaso350ml() {
        String funcion = "0.5 * (x^2 + y^2)";
        double volumenObjetivo = 350.0;
        double espesor = 0.20;

        ResultadoCalculo res = motor.ejecutarAnalisisCompleto(funcion, 0.0, espesor, volumenObjetivo, vidrio);

        assertNotNull(res);

        // Altura despejada teórica h = sqrt(350 / pi) ~ 10.555 cm (~ 10.56 cm)
        assertEquals(10.56, res.alturaIngresada, 0.05, "La altura calculada debe ser aproximadamente 10.56 cm");

        // Radio en el borde r = sqrt(2 * h) ~ 4.595 cm (~ 4.60 cm)
        assertEquals(4.60, res.radioBorde, 0.05, "El radio del borde debe ser aproximadamente 4.60 cm");

        // Capacidad calculada cercana a 350 ml con error numérico < 0.1%
        assertEquals(350.0, res.volumenCalculado, 1.5, "El volumen calculado debe aproximar los 350 ml");
        assertTrue(res.errorNumericoPorcentaje < 0.1, "El error numérico de integración debe ser < 0.1%");

        // Área superficial de pared paraboloidal A = (2*pi/3)*((1+2h)^1.5 - 1) ~ 215.6 cm²
        assertTrue(res.areaPared > 200.0 && res.areaPared < 230.0, "El área superficial debe rondar los 215.6 cm²");

        // Optimización de Lagrange r* = h* = (350/pi)^(1/3) ~ 4.81 cm
        assertEquals(4.81, res.radioOptimoLagrange, 0.05, "El radio óptimo de Lagrange debe ser ~ 4.81 cm");
        assertEquals(4.81, res.alturaOptimaLagrange, 0.05, "La altura óptima de Lagrange debe ser ~ 4.81 cm");
        assertTrue(res.materialOptimoLagrange > 0, "El insumo óptimo de Lagrange debe ser positivo");

        // Prueba de sensibilidad: factor k = 1.1 -> V' = 350 / 1.1 ~ 318.18 ml
        double volSimulado = motor.simularDesviacionCurvatura(funcion, 1.1, res.alturaIngresada);
        assertEquals(318.18, volSimulado, 2.0, "El volumen simulado con k = 1.1 debe aproximar 318.18 ml");
    }
}
