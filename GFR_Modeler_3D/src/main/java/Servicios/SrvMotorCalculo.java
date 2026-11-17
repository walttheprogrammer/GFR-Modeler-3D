package Servicios;
import Modelo.CotaGraduacion;
import Modelo.Material;
import Modelo.ResultadoCalculo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BisectionSolver;

public class SrvMotorCalculo {
    private Expression expresion;
    private String funcionActual = "";
    private static final int N_THETA = 36;
    private static final int N_R = 60;

    public boolean compilarFuncion(String textoCrudo) {
        if (textoCrudo == null || textoCrudo.trim().isEmpty()) {
            this.expresion = null;
            this.funcionActual = "";
            throw new IllegalArgumentException("La función no puede estar vacía.");
        }
        try {
            String limpia = textoCrudo
                    .toLowerCase(Locale.ROOT)
                    .replaceFirst("^z\\s*=\\s*", "")
                    .replace("²", "^2")
                    .replace("³", "^3")
                    .replace(",", ".")
                    .trim();
            if (limpia.isEmpty()) {
                this.expresion = null;
                this.funcionActual = "";
                throw new IllegalArgumentException("La función no puede estar vacía.");
            }

            this.expresion = new ExpressionBuilder(limpia)
                    .variables("x", "y")
                    .build();

            double z0 = this.expresion.setVariable("x", 0.0).setVariable("y", 0.0).evaluate();
            if (Double.isNaN(z0) || Double.isInfinite(z0)) {
                throw new IllegalArgumentException("La función no está definida en el origen (0, 0).");
            }

            for (int i = 0; i < 16; i++) {
                double theta = (2.0 * Math.PI * i) / 16.0;
                double cos = Math.cos(theta);
                double sin = Math.sin(theta);

                double z1 = this.expresion.setVariable("x", 1.0 * cos).setVariable("y", 1.0 * sin).evaluate();
                double z5 = this.expresion.setVariable("x", 5.0 * cos).setVariable("y", 5.0 * sin).evaluate();

                if (Double.isNaN(z1) || Double.isInfinite(z1) || Double.isNaN(z5) || Double.isInfinite(z5)) {
                    throw new IllegalArgumentException("La función produce valores indefinidos (NaN/Infinito) en el dominio del envase.");
                }
                if (z5 <= z0 + 1e-4) {
                    throw new IllegalArgumentException("La superficie f(x,y) no forma un recipiente cerrado válido (debe crecer en todas direcciones desde el origen).");
                }
            }

            this.funcionActual = limpia;
            return true;
        } catch (IllegalArgumentException iae) {
            this.expresion = null;
            throw iae;
        } catch (Exception e) {
            this.expresion = null;
            throw new IllegalArgumentException("Sintaxis matemática inválida en f(x,y): " + e.getMessage(), e);
        }
    }

    public double evaluarZ(double x, double y) {
        if (expresion == null) return Double.NaN;
        return expresion.setVariable("x", x).setVariable("y", y).evaluate();
    }

    // Encuentra el radio donde f(r*cos(theta), r*sin(theta)) = zNivel usando BisectionSolver de Commons Math3
    public double hallarRadioEnNivel(double zNivel, double theta) {
        if (expresion == null) {
            throw new IllegalArgumentException("No hay una función compilada válida.");
        }
        double z0 = evaluarZ(0, 0);
        if (Double.isNaN(z0) || Double.isInfinite(z0)) {
            throw new IllegalArgumentException("La función produce valores indefinidos en el origen.");
        }
        if (zNivel <= z0 + 1e-6) return 0.0;

        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        UnivariateFunction fn = r -> {
            double val = evaluarZ(r * cos, r * sin);
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                throw new IllegalArgumentException("La función produce valores indefinidos en el dominio del envase.");
            }
            return val - zNivel;
        };

        double rMax = 0.5;
        while (rMax < 60.0 && fn.value(rMax) < 0) {
            rMax *= 1.5;
        }
        if (fn.value(rMax) < 0) {
            throw new IllegalArgumentException("La superficie no alcanza la altura solicitada dentro de un radio válido (60 cm).");
        }

        try {
            BisectionSolver solver = new BisectionSolver(1e-6);
            return solver.solve(200, fn, 0.0, rMax);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            return rMax;
        }
    }

    // Integral Doble V(h) = ∬ [h - f(x,y)] dA en coordenadas polares
    public double calcularVolumenHastaAltura(double h) {
        return calcularVolumenHastaAltura(h, N_THETA, N_R);
    }
    
    public double calcularVolumenHastaAltura(double h, int nTheta, int nR) {
        double zMin = evaluarZ(0, 0);
        if (h <= zMin) return 0.0;

        double dTheta = (2.0 * Math.PI) / nTheta;
        double volumenTotal = 0.0;

        for (int i = 0; i < nTheta; i++) {
            double theta = (i + 0.5) * dTheta;
            double rMax = hallarRadioEnNivel(h, theta);
            if (rMax <= 0) continue;

            double dr = rMax / nR;
            double sumaRadial = 0.0;
            for (int j = 0; j < nR; j++) {
                double r = (j + 0.5) * dr;
                double z = evaluarZ(r * Math.cos(theta), r * Math.sin(theta));
                sumaRadial += Math.max(0.0, h - z) * r * dr;
            }
            volumenTotal += sumaRadial * dTheta;
        }
        return volumenTotal;
    }

    // Integral Doble de Superficie A = ∬ sqrt(1 + (∂f/∂x)² + (∂f/∂y)²) dA
    public double calcularAreaPared(double h) {
        double zMin = evaluarZ(0, 0);
        if (h <= zMin) return 0.0;

        int nTheta = 36;
        int nR = 60;
        double dTheta = (2.0 * Math.PI) / nTheta;
        double delta = 1e-4;
        double areaTotal = 0.0;

        for (int i = 0; i < nTheta; i++) {
            double theta = (i + 0.5) * dTheta;
            double rMax = hallarRadioEnNivel(h, theta);
            if (rMax <= 0) continue;

            double dr = rMax / nR;
            double sumaRadial = 0.0;
            for (int j = 0; j < nR; j++) {
                double r = (j + 0.5) * dr;
                double x = r * Math.cos(theta);
                double y = r * Math.sin(theta);

                double dfdx = (evaluarZ(x + delta, y) - evaluarZ(x - delta, y)) / (2.0 * delta);
                double dfdy = (evaluarZ(x, y + delta) - evaluarZ(x, y - delta)) / (2.0 * delta);
                double ds = Math.sqrt(1.0 + dfdx * dfdx + dfdy * dfdy);

                sumaRadial += ds * r * dr;
            }
            areaTotal += sumaRadial * dTheta;
        }
        return areaTotal;
    }

    // Cálculo inverso: altura h necesaria para lograr volumenObjetivo (ml)
    public double calcularAlturaParaVolumen(double volumenObjetivo) {
        if (volumenObjetivo <= 0) return 0.0;
        double zMin = evaluarZ(0, 0);
        UnivariateFunction fn = h -> calcularVolumenHastaAltura(h) - volumenObjetivo;

        double hMax = zMin + 5.0;
        while (hMax < 100.0 && fn.value(hMax) < 0) {
            hMax *= 1.5;
        }
        try {
            BisectionSolver solver = new BisectionSolver(1e-4);
            return solver.solve(100, fn, zMin, hMax);
        } catch (Exception e) {
            return hMax;
        }
    }

    public ResultadoCalculo ejecutarAnalisisCompleto(String funcionTexto, double alturaH, double espesorE,
                                                      double volumenObj, Material mat) {
        if (!compilarFuncion(funcionTexto)) {
            throw new IllegalArgumentException("Sintaxis matemática inválida en f(x,y)");
        }

        ResultadoCalculo res = new ResultadoCalculo();
        res.funcion = this.funcionActual;
        res.alturaIngresada = alturaH;
        res.espesor = espesorE;
        res.volumenObjetivo = volumenObj;

        // Si el usuario dejó altura en 0 pero puso volumen objetivo > 0, calculamos la altura necesaria
        if (alturaH <= 0 && volumenObj > 0) {
            alturaH = calcularAlturaParaVolumen(volumenObj);
            res.alturaIngresada = alturaH;
        }

        res.volumenCalculado = calcularVolumenHastaAltura(alturaH);
        res.alturaParaObjetivo = (volumenObj > 0) ? calcularAlturaParaVolumen(volumenObj) : alturaH;
        res.radioBorde = hallarRadioEnNivel(alturaH, 0.0);
        res.areaPared = calcularAreaPared(alturaH);
        res.volumenMaterial = res.areaPared * espesorE;
        res.masaGramos = res.volumenMaterial * mat.getDensidadGcm3();
        res.costoTotal = res.volumenMaterial * mat.getPrecioCm3();

        // Gradiente ∇f = (∂f/∂x, ∂f/∂y) en el borde (r = radioBorde, y = 0)
        double delta = 1e-4;
        double xb = res.radioBorde;
        double dfdx = (evaluarZ(xb + delta, 0) - evaluarZ(xb - delta, 0)) / (2.0 * delta);
        double dfdy = (evaluarZ(xb, delta) - evaluarZ(xb, -delta)) / (2.0 * delta);
        double magGrad = Math.hypot(dfdx, dfdy);
        res.anguloDesmoldeGrados = (magGrad > 1e-6) ? Math.toDegrees(Math.atan(1.0 / magGrad)) : 90.0;
        res.desmoldable = res.anguloDesmoldeGrados >= 3.0;

        // Tolerancia respecto a refino fino¡
        double vFino = calcularVolumenHastaAltura(alturaH, 2 * N_THETA, 2 * N_R);
        res.errorNumericoPorcentaje = (vFino > 0) ? Math.abs(vFino - res.volumenCalculado) / vFino * 100.0 : 0.0;

        // Optimización por Multiplicadores de Lagrange (∇V = λ∇A -> cilindro/paraboloide óptimo H = R)
        double vRef = (volumenObj > 0) ? volumenObj : res.volumenCalculado;
        res.radioOptimoLagrange = Math.cbrt(vRef / Math.PI);
        res.alturaOptimaLagrange = res.radioOptimoLagrange;
        double areaOptima = 2.0 * Math.PI * res.radioOptimoLagrange * res.alturaOptimaLagrange
                          + Math.PI * Math.pow(res.radioOptimoLagrange, 2);
        res.materialOptimoLagrange = areaOptima * espesorE;

        // Aproximaciones simples (Anexo K)
        double rBase = hallarRadioEnNivel(evaluarZ(0, 0) + 0.05, 0.0);
        res.volumenCilindroAprox = Math.PI * Math.pow(res.radioBorde, 2) * alturaH;
        res.volumenConoAprox = (Math.PI * alturaH / 3.0) *
                (Math.pow(res.radioBorde, 2) + res.radioBorde * rBase + Math.pow(rBase, 2));

        // Tabla de cotas de graduación (cada 50 ml hasta max(volumenCalculado, volumenObj))
        List<CotaGraduacion> listaCotas = new ArrayList<>();
        double volLimite = Math.max(res.volumenCalculado, volumenObj);
        if (volLimite >= 50) {
            for (double v = 50.0; v <= volLimite + 0.1; v += 50.0) {
                double hk = calcularAlturaParaVolumen(v);
                double rk = hallarRadioEnNivel(hk, 0.0);
                double ak = calcularAreaPared(hk);
                double mk = ak * espesorE;
                double ck = mk * mat.getPrecioCm3();
                listaCotas.add(new CotaGraduacion(v, hk, rk, ak, mk, ck));
            }
        }
        res.cotas = listaCotas;

        // Reportes formateados para los botones A, B y C (sin prefijos [A], [B], [C])
        double diffVol = res.volumenCalculado - res.volumenObjetivo;
        String evaluacionVolumen = (res.volumenObjetivo > 0)
            ? (Math.abs(diffVol) <= 5.0
                ? String.format(Locale.US, "La capacidad efectiva satisface el volumen nominal requerido con una desviación de solo %+.2f ml.", diffVol)
                : String.format(Locale.US, "La geometría actual presenta una variación volumétrica de %+.2f ml respecto a la cuota establecida.", diffVol))
            : "La cavidad evaluada no presenta una restricción de llenado objetivo predefinida.";

        String evaluacionMaterial = String.format(Locale.US,
            "El espesor uniforme de %.2f cm demanda %.2f cm³ de %s, equivalente a %.1f g de masa neta.",
            res.espesor, res.volumenMaterial, mat.getNombreMaterial(), res.masaGramos);

        String evaluacionCosto = String.format(Locale.US,
            "La tasa de material proyecta un costo directo de fabricación de S/ %.2f por envase.",
            res.costoTotal);

        String conclusionA = (res.volumenObjetivo > 0 && Math.abs(diffVol) <= 5.0)
            ? "El dimensionamiento geométrico es apto para producción sin requerir ajuste de cota de llenado."
            : String.format(Locale.US, "Se sugiere calibrar la altura a %.2f cm para coincidir exactamente con el tiraje de envasado.", res.alturaParaObjetivo);

        String narrativaA = String.format(
            "\n\nDiagnóstico Técnico:\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s",
            evaluacionVolumen, evaluacionMaterial, evaluacionCosto, conclusionA
        );

        res.reporteA = String.format(Locale.US,
            "=== CÁLCULO VOLUMÉTRICO Y CUBICACIÓN ===\n" +
            "Función: z = %s\n" +
            "Altura (h): %.2f cm (Objetivo: %.2f cm)\n" +
            "V(h): %.2f ml / %.0f ml\n" +
            "Borde (r): %.2f cm | Insumo: %.2f cm³ (%.1f g)\n" +
            "Área (A): %.2f cm²\n" +
            "Costo: S/ %.2f (%s)%s",
            res.funcion,
            res.alturaIngresada, res.alturaParaObjetivo,
            res.volumenCalculado, res.volumenObjetivo,
            res.radioBorde, res.volumenMaterial, res.masaGramos,
            res.areaPared,
            res.costoTotal, mat.getNombreMaterial(),
            narrativaA
        );

        String dictamenAngulo = res.desmoldable
            ? String.format(Locale.US, "El ángulo de salida obtenido de %.2f° supera con holgura el umbral crítico de 3.0°.", res.anguloDesmoldeGrados)
            : String.format(Locale.US, "El ángulo de salida de %.2f° no alcanza el umbral mínimo de 3.0° requerido para desmolde.", res.anguloDesmoldeGrados);

        String evaluacionFriccion = res.desmoldable
            ? "La conicidad garantiza la expulsión de pieza en matriz rígida minimizando fricción y desgaste prematuro."
            : "Existe riesgo severo de bloqueo mecánico, socavado o rayaduras durante la apertura del herramental.";

        String evaluacionGradiente = String.format(Locale.US,
            "El vector gradiente ∇f con magnitud %.2f en periferia define una pendiente continua y favorable en las generatrices.",
            magGrad);

        String evaluacionTolerancia = String.format(Locale.US,
            "La convergencia numérica con tolerancia de ±%.3f%% valida la integridad dimensional para maquinado CNC.",
            res.errorNumericoPorcentaje);

        String narrativaB = String.format(
            "\n\nDiagnóstico Técnico:\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s",
            dictamenAngulo, evaluacionFriccion, evaluacionGradiente, evaluacionTolerancia
        );

        res.reporteB = String.format(Locale.US,
            "=== ANÁLISIS DE GRADIENTE Y DESMOLDE ===\n" +
            "∇f(%.2f, 0) = (%.2f, %.2f)\n" +
            "||∇f||: %.2f | Ángulo: %.1f°\n" +
            "Desmolde: %s (≥ 3.0°)\n" +
            "Tolerancia: ±%.3f%% (%s)%s",
            xb, dfdx, dfdy,
            magGrad, res.anguloDesmoldeGrados,
            res.desmoldable ? "APROBADO" : "REVISAR",
            res.errorNumericoPorcentaje,
            res.errorNumericoPorcentaje < 0.1 ? "OK" : "REVISAR",
            narrativaB
        );

        double ahorroMaterial = res.volumenMaterial - res.materialOptimoLagrange;
        double difCilindro = res.volumenCilindroAprox - res.volumenCalculado;
        double difCono = res.volumenConoAprox - res.volumenCalculado;

        String evaluacionLagrange = (ahorroMaterial > 0.05)
            ? String.format(Locale.US, "La optimización por multiplicadores de Lagrange proyecta un ahorro potencial de %.2f cm³ de insumo.", ahorroMaterial)
            : "La configuración geométrica actual se aproxima estrechamente al óptimo isoperimétrico teórico.";

        String evaluacionDimensiones = String.format(Locale.US,
            "La proporción óptima r*=h*=%.2f cm balancea el área perimetral y reduce el peso estructural para la misma capacidad.",
            res.radioOptimoLagrange);

        String evaluacionAproximaciones = String.format(Locale.US,
            "Las aproximaciones de taller acumulan desviaciones notables: cilindro (%+.1f ml) y tronco de cono (%+.1f ml).",
            difCilindro, difCono);

        String conclusionC = "El cálculo riguroso por integrales dobles previene mermas por sobrellenado en líneas automáticas.";

        String narrativaC = String.format(
            "\n\nDiagnóstico Técnico:\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s\n" +
            "• %s",
            evaluacionLagrange, evaluacionDimensiones, evaluacionAproximaciones, conclusionC
        );

        res.reporteC = String.format(Locale.US,
            "=== OPTIMIZACIÓN Y COMPARATIVA CAD ===\n" +
            "Optimización Lagrange: r* = %.2f cm | h* = %.2f cm | Insumo* = %.1f cm³\n" +
            "V(h) Integral: %.2f ml | Aprox. Cilindro: %.2f ml (%+.1f ml) | Aprox. Tronco de Cono: %.2f ml (%+.1f ml)%s",
            res.radioOptimoLagrange, res.alturaOptimaLagrange, res.materialOptimoLagrange,
            res.volumenCalculado,
            res.volumenCilindroAprox, difCilindro,
            res.volumenConoAprox, difCono,
            narrativaC
        );
        return res;
    }
    public double simularDesviacionCurvatura(String funcionBase, double nuevoCoeficiente, double alturaH) {
        if (nuevoCoeficiente <= 0) return 0.0;
        String limpia = funcionBase.trim();
        String funcDesviada;

        // Reemplaza el coeficiente inicial si tiene forma "numero * (expresion)" sin otro término sumando afuera
        String sinParentesis = limpia.replaceAll("\\([^()]*\\)", "");
        boolean tieneTerminosMultiples = sinParentesis.matches(".*[+-].*");

        if (!tieneTerminosMultiples && limpia.matches("^[0-9]+([.,][0-9]+)?\\s*\\*.*")) {
            funcDesviada = limpia.replaceFirst("^[0-9]+([.,][0-9]+)?", String.valueOf(nuevoCoeficiente));
        } else {
            funcDesviada = "(" + nuevoCoeficiente + ") * (" + limpia + ")";
        }

        try {
            compilarFuncion(funcDesviada);
        } catch (Exception e) {
            return 0.0;
        }
        double vol = calcularVolumenHastaAltura(alturaH);
        try {
            compilarFuncion(funcionBase);
        } catch (Exception ignored) {}
        return vol;
    }
}