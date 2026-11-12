package DAO;
import Modelo.*;
import Utilidades.*;
import java.sql.*;
import java.util.*;
public class MaterialDAO {

    public List<Material> listar() {
        List<Material> lista = new ArrayList<>();
        String sql = "SELECT * FROM materiales ORDER BY id_material ASC";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Material(
                    rs.getInt("id_material"),
                    rs.getString("nombre_material"),
                    rs.getDouble("densidad_gcm3"),
                    rs.getDouble("precio_cm3")
                ));
            }
        } catch (SQLException e) {
            return null;
        }
        return lista;
    }

    public boolean actualizar(int idMaterial, double densidad, double precio) {
        String sql = "UPDATE materiales SET densidad_gcm3 = ?, precio_cm3 = ? WHERE id_material = ?";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDouble(1, densidad);
            ps.setDouble(2, precio);
            ps.setInt(3, idMaterial);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}