package Modelo;
public class Proyecto {
    private int idProyecto;
    private int idUsuario;
    private int idMaterial;
    private String nombreProyecto;
    private String fechaCreacion;
    private String fechaModificacion;
    private Parametro parametro;
    private Material material;

    public int getIdProyecto() { return idProyecto; }
    public void setIdProyecto(int idProyecto) { this.idProyecto = idProyecto; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public int getIdMaterial() { return idMaterial; }
    public void setIdMaterial(int idMaterial) { this.idMaterial = idMaterial; }
    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(String fechaModificacion) { this.fechaModificacion = fechaModificacion; }
    public Parametro getParametro() { return parametro; }
    public void setParametro(Parametro parametro) { this.parametro = parametro; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
}