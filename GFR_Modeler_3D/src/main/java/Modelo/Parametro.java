package Modelo;
public class Parametro {
    private int idParametro;
    private int idProyecto;
    private String funcionPared;
    private double alturaCm;
    private double espesorCm;
    private double volumenObjetivoMl;
    private double volumenCalculadoMl;
    private double areaParedCm2;
    private double volumenMaterialCm3;
    private double costoEstimado;

    public int getIdParametro() { return idParametro; }
    public void setIdParametro(int idParametro) { this.idParametro = idParametro; }
    public int getIdProyecto() { return idProyecto; }
    public void setIdProyecto(int idProyecto) { this.idProyecto = idProyecto; }
    public String getFuncionPared() { return funcionPared; }
    public void setFuncionPared(String funcionPared) { this.funcionPared = funcionPared; }
    public double getAlturaCm() { return alturaCm; }
    public void setAlturaCm(double alturaCm) { this.alturaCm = alturaCm; }
    public double getEspesorCm() { return espesorCm; }
    public void setEspesorCm(double espesorCm) { this.espesorCm = espesorCm; }
    public double getVolumenObjetivoMl() { return volumenObjetivoMl; }
    public void setVolumenObjetivoMl(double volumenObjetivoMl) { this.volumenObjetivoMl = volumenObjetivoMl; }
    public double getVolumenCalculadoMl() { return volumenCalculadoMl; }
    public void setVolumenCalculadoMl(double volumenCalculadoMl) { this.volumenCalculadoMl = volumenCalculadoMl; }
    public double getAreaParedCm2() { return areaParedCm2; }
    public void setAreaParedCm2(double areaParedCm2) { this.areaParedCm2 = areaParedCm2; }
    public double getVolumenMaterialCm3() { return volumenMaterialCm3; }
    public void setVolumenMaterialCm3(double volumenMaterialCm3) { this.volumenMaterialCm3 = volumenMaterialCm3; }
    public double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(double costoEstimado) { this.costoEstimado = costoEstimado; }
}