import Controladores.CtrlMotorCalculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class Pruebas_ValidacionFunciones {

    private CtrlMotorCalculo motor;

    @BeforeEach
    void setUp() {
        motor = new CtrlMotorCalculo();
    }

    @Test
    @DisplayName("Aceptación de superficies cerradas válidas")
    void testSuperficiesCerradasValidas() {
        // Paraboloide circular estándar
        assertTrue(motor.compilarFuncion("0.5 * (x^2 + y^2)"), "Debe compilar un paraboloide circular válido");
        double r1 = motor.hallarRadioEnNivel(5.0, 0.0);
        assertTrue(r1 > 0.0, "El radio en nivel h = 5 debe ser positivo");
        assertEquals(Math.sqrt(10.0), r1, 0.01, "Para z = 0.5 * r^2, en z = 5 el radio r = sqrt(10) ~ 3.16 cm");

        // Paraboloide elíptico suave
        assertTrue(motor.compilarFuncion("0.25 * x^2 + 0.30 * y^2"), "Debe compilar un paraboloide elíptico");
        double r2 = motor.hallarRadioEnNivel(4.0, 0.0);
        assertTrue(r2 > 0.0, "El radio en nivel h = 4 debe ser positivo");
    }

    @Test
    @DisplayName("Rechazo de superficie abierta cúbica x^3 + y^3")
    void testSuperficieAbiertaCubica() {
        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion("x^3 + y^3");
        }, "Debe lanzar IllegalArgumentException ante una superficie abierta x^3 + y^3");
    }

    @Test
    @DisplayName("Rechazo de ensilladura x^2 - y^2 (paraboloide hiperbólico)")
    void testSuperficieEnsilladura() {
        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion("x^2 - y^2");
        }, "Debe lanzar IllegalArgumentException ante una superficie en silla de montar x^2 - y^2");
    }

    @Test
    @DisplayName("Rechazo de función inválida o con dominio restringido sqrt(x)")
    void testFuncionInvalidaRaiz() {
        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion("sqrt(x)");
        }, "Debe lanzar IllegalArgumentException ante sqrt(x) por indeterminación para x < 0");
    }

    @Test
    @DisplayName("Rechazo de entrada de texto vacío o en blanco")
    void testTextoVacio() {
        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion("");
        }, "Debe lanzar IllegalArgumentException ante texto vacío");

        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion("   ");
        }, "Debe lanzar IllegalArgumentException ante espacios en blanco");

        assertThrows(IllegalArgumentException.class, () -> {
            motor.compilarFuncion(null);
        }, "Debe lanzar IllegalArgumentException ante valor null");
    }

    @Test
    @DisplayName("Rechazo en hallarRadioEnNivel si no hay función o es inalcanzable")
    void testHallarRadioExcepciones() {
        CtrlMotorCalculo motorSinCompilar = new CtrlMotorCalculo();
        assertThrows(IllegalArgumentException.class, () -> {
            motorSinCompilar.hallarRadioEnNivel(5.0, 0.0);
        }, "Debe lanzar excepción si no hay función compilada");

        motor.compilarFuncion("0.5 * (x^2 + y^2)");
        assertThrows(IllegalArgumentException.class, () -> {
            motor.hallarRadioEnNivel(50000.0, 0.0);
        }, "Debe lanzar excepción si la altura excede el radio máximo admisible de 60 cm");
    }
}
