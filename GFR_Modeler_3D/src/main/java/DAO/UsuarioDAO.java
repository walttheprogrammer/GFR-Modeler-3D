package DAO;

import Modelo.Usuario;
import Utilidades.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public Usuario login(String usuarioOCorreo, String clave) {
        String sql = "SELECT id_usuario, nombre_completo, usuario, correo, clave FROM usuarios WHERE (usuario = ? OR correo = ?) AND clave = ?";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, usuarioOCorreo.trim());
            ps.setString(2, usuarioOCorreo.trim());
            ps.setString(3, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setCorreo(rs.getString("correo"));
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registrar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre_completo, usuario, correo, clave) VALUES (?, ?, ?, ?)";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            String nomCompleto = u.getNombreCompleto();
            if (nomCompleto == null || nomCompleto.trim().isEmpty()) {
                nomCompleto = (u.getNombres() + " " + u.getApellidos()).trim();
            }
            ps.setString(1, nomCompleto);
            ps.setString(2, u.getUsuario().trim());
            ps.setString(3, u.getCorreo().trim());
            ps.setString(4, u.getContraseña());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();    
            return false;
        }
    }
}
