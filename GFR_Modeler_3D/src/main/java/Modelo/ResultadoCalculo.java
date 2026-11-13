package Modelo;
import java.util.List;
public class ResultadoCalculo {
    public String funcion;
    public double alturaIngresada;
    public double espesor;
    public double volumenObjetivo;
    public double volumenCalculado;
    public double alturaParaObjetivo;
    public double radioBorde;
    public double areaPared;
    public double volumenMaterial;
    public double masaGramos;
    public double costoTotal;
    public double anguloDesmoldeGrados;
    public boolean desmoldable;
    public double errorNumericoPorcentaje;

    // Lagrange
    public double radioOptimoLagrange;
    public double alturaOptimaLagrange;
    public double materialOptimoLagrange;

    // Aproximaciones
    public double volumenCilindroAprox;
    public double volumenConoAprox;

    // Tabla y reportes para los
    public List<CotaGraduacion> cotas;
    public String reporteA;
    public String reporteB;
    public String reporteC;
}
