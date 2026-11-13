package Controladores;
import Modelo.CotaGraduacion;
import Modelo.Material;
import Modelo.ResultadoCalculo;
import java.util.ArrayList;
import java.util.List;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BisectionSolver;

public class CtrlMotorCalculo {

    private Expression expresion;
    private String funcionActual = "";
    private static final int N_THETA = 36;
    private static final int N_R = 60;

    public boolean compilarFuncion(String textoCrudo) {
        try {
            String limpia = textoCrudo
                    .replace("²", "^2")
                    .replace("³", "^3")
                    .replace(",", ".")
                    .trim();
            if (limpia.isEmpty()) return false;

            this.expresion = new ExpressionBuilder(limpia)
                    .variables("x", "y")
                    .build();
            // Verificación en origen
            this.expresion.setVariable("x", 0.0).setVariable("y", 0.0).evaluate();
            this.funcionActual = limpia;
            return true;
        } catch (Exception e) {
            this.expresion = null;
            return false;
        }
    }

    public double evaluarZ(double x, double y) {
        if (expresion == null) return Double.NaN;
        return expresion.setVariable("x", x).setVariable("y", y).evaluate();
    }

    // Encuentra el radio donde f(r*cos(theta), r*sin(theta)) = zNivel usando BisectionSolver de Commons Math3
    public double hallarRadioEnNivel(double zNivel, double theta) {
        double z0 = evaluarZ(0, 0);
        if (zNivel <= z0 + 1e-6) return 0.0;

        UnivariateFunction fn = r -> evaluarZ(r * Math.cos(theta), r * Math.sin(theta)) - zNivel;
        double rMax = 0.5;
        while (rMax < 60.0 && fn.value(rMax) < 0) {
            rMax *= 1.5;
        }
        if (fn.value(rMax) < 0) return 10.0;

        try {
            BisectionSolver solver = new BisectionSolver(1e-6);
            return solver.solve(200, fn, 0.0, rMax);
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

        // Reportes formateados para los botones A, B y C
        res.reporteA = String.format(
            "=== [A] CÁLCULO VOLUMÉTRICO Y CUBICACIÓN ===\n" +
            "Función: z = %s\n" +
            "Altura (h): %.2f cm (Objetivo: %.2f cm)\n" +
            "V(h): %.2f ml / %.0f ml\n" +
            "Borde (r): %.2f cm | Insumo: %.2f cm³ (%.1f g)\n" +
            "Área (A): %.2f cm²\n" +
            "Costo: S/ %.2f (%s)",
            res.funcion,
            res.alturaIngresada, res.alturaParaObjetivo,
            res.volumenCalculado, res.volumenObjetivo,
            res.radioBorde, res.volumenMaterial, res.masaGramos,
            res.areaPared,
            res.costoTotal, mat.getNombreMaterial()
        );
        res.reporteB = String.format(
            "=== [B] ANÁLISIS DE GRADIENTE Y DESMOLDE ===\n" +
            "∇f(%.2f, 0) = (%.2f, %.2f)\n" +
            "||∇f||: %.2f | Ángulo: %.1f°\n" +
            "Desmolde: %s (≥ 3.0°)\n" +
            "Tolerancia: ±%.3f%% (%s)",
            xb, dfdx, dfdy,
            magGrad, res.anguloDesmoldeGrados,
            res.desmoldable ? "APROBADO" : "REVISAR",
            res.errorNumericoPorcentaje,
            res.errorNumericoPorcentaje < 0.1 ? "OK" : "REVISAR"
        );

        res.reporteC = String.format(
            "=== C_OPTIMIZACIÓN Y COMPARATIVA CAD ===\n" +
            "Optimización Lagrange: r* = %.2f cm | h* = %.2f cm | Insumo* = %.1f cm³\n" +
            "V(h) Integral: %.2f ml | Aprox. Cilindro: %.2f ml (%+.1f ml) | Aprox. Tronco de Cono: %.2f ml (%+.1f ml)",
            res.radioOptimoLagrange, res.alturaOptimaLagrange, res.materialOptimoLagrange,
            res.volumenCalculado,
            res.volumenCilindroAprox, res.volumenCilindroAprox - res.volumenCalculado,
            res.volumenConoAprox, res.volumenConoAprox - res.volumenCalculado
        );
        return res;
    }
    public double simularDesviacionCurvatura(String funcionBase, double factorK, double alturaH) {
        String funcDesviada = "(" + factorK + ") * (" + funcionBase + ")";
        if (!compilarFuncion(funcDesviada)) return 0.0;
        double vol = calcularVolumenHastaAltura(alturaH);
        compilarFuncion(funcionBase); // Restaura original
        return vol;
    }
}