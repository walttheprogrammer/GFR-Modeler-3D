package Modelo;
public class CotaGraduacion {
    private double volumenMl;
    private double alturaCm;
    private double radioCm;
    private double areaAcumuladaCm2;
    private double resinaAcumuladaCm3;
    private double costoAcumulado;

    public CotaGraduacion(double volumenMl, double alturaCm, double radioCm,
                          double areaAcumuladaCm2, double resinaAcumuladaCm3, double costoAcumulado) {
        this.volumenMl = volumenMl;
        this.alturaCm = alturaCm;
        this.radioCm = radioCm;
        this.areaAcumuladaCm2 = areaAcumuladaCm2;
        this.resinaAcumuladaCm3 = resinaAcumuladaCm3;
        this.costoAcumulado = costoAcumulado;
    }

    public double getVolumenMl() { return volumenMl; }
    public double getAlturaCm() { return alturaCm; }
    public double getRadioCm() { return radioCm; }
    public double getAreaAcumuladaCm2() { return areaAcumuladaCm2; }
    public double getResinaAcumuladaCm3() { return resinaAcumuladaCm3; }
    public double getCostoAcumulado() { return costoAcumulado; }
}