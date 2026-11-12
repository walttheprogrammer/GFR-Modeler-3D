package Modelo;

public class Usuario {
    private int idUsuario;
    private String nombreCompleto;
    private String nombres;
    private String apellidos;
    private String usuario;
    private String correo;
    private String contraseña;

    public Usuario() {}

    public Usuario(String nombreCompleto, String usuario, String correo, String clave) {
        setNombreCompleto(nombreCompleto);
        this.usuario = usuario;
        this.correo = correo;
        this.contraseña = clave;
    }

    public Usuario(String nombres, String apellidos, String usuario, String correo, String clave) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        actualizarNombreCompleto();
        this.usuario = usuario;
        this.correo = correo;
        this.contraseña = clave;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreCompleto() {
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
            return nombreCompleto.trim();
        }
        String n = nombres != null ? nombres.trim() : "";
        String a = apellidos != null ? apellidos.trim() : "";
        return (n + " " + a).trim();
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
            String[] partes = nombreCompleto.trim().split("\\s+", 2);
            this.nombres = partes.length > 0 ? partes[0] : "";
            this.apellidos = partes.length > 1 ? partes[1] : "";
        }
    }

    public String getNombres() {
        if (nombres != null && !nombres.trim().isEmpty()) return nombres.trim();
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
            String[] partes = nombreCompleto.trim().split("\\s+", 2);
            return partes.length > 0 ? partes[0] : "";
        }
        return "";
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
        actualizarNombreCompleto();
    }

    public String getApellidos() {
        if (apellidos != null && !apellidos.trim().isEmpty()) return apellidos.trim();
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
            String[] partes = nombreCompleto.trim().split("\\s+", 2);
            return partes.length > 1 ? partes[1] : "";
        }
        return "";
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
        actualizarNombreCompleto();
    }

    private void actualizarNombreCompleto() {
        String n = nombres != null ? nombres.trim() : "";
        String a = apellidos != null ? apellidos.trim() : "";
        this.nombreCompleto = (n + " " + a).trim();
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContraseña() { return contraseña; }
    public void setContraseña(String clave) { this.contraseña = clave; }
}
