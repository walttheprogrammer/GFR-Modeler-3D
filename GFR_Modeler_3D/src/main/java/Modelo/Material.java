package Modelo;
public class Material {
    private int idMaterial;
    private String nombreMaterial;
    private double densidadGcm3;
    private double precioCm3;
    
    public Material() {}
    
    public Material(int idMaterial, String nombreMaterial, double densidadGcm3, double precioCm3) {
        this.idMaterial = idMaterial;
        this.nombreMaterial = nombreMaterial;
        this.densidadGcm3 = densidadGcm3;
        this.precioCm3 = precioCm3;
    }

    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial = idMaterial; }
    public String getNombreMaterial() { return nombreMaterial; }
    public void setNombreMaterial(String nombreMaterial) { this.nombreMaterial = nombreMaterial; }
    public double getDensidadGcm3() { return densidadGcm3; }
    public void setDensidadGcm3(double densidadGcm3) { this.densidadGcm3 = densidadGcm3; }
    public double getPrecioCm3() { return precioCm3; }
    public void setPrecioCm3(double precioCm3) { this.precioCm3 = precioCm3; }

    @Override
    public String toString() {
        return nombreMaterial + " (S/ " + String.format("%.2f", precioCm3) + "/cm³)";
    }
}