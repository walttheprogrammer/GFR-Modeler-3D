package DAO;
import Modelo.Material;
import Modelo.Parametro;
import Modelo.Proyecto;
import Utilidades.ConexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProyectoDAO {

    public int guardarNuevo(Proyecto p) {
        String sqlProy = "INSERT INTO proyectos (id_usuario, id_material, nombre_proyecto) VALUES (?, ?, ?)";
        String sqlParam = "INSERT INTO parametros (id_proyecto, funcion_pared, altura_cm, espesor_cm, " +
                          "volumen_objetivo_ml, volumen_calculado_ml, area_pared_cm2, volumen_material_cm3, costo_estimado) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection cn = null;
        try {
            cn = ConexionBD.conectar();
            cn.setAutoCommit(false);

            int idGenerado = -1;
            try (PreparedStatement ps1 = cn.prepareStatement(sqlProy, Statement.RETURN_GENERATED_KEYS)) {
                ps1.setInt(1, p.getIdUsuario());
                ps1.setInt(2, p.getIdMaterial());
                ps1.setString(3, p.getNombreProyecto());
                ps1.executeUpdate();
                try (ResultSet keys = ps1.getGeneratedKeys()) {
                    if (keys.next()) idGenerado = keys.getInt(1);
                }
            }

            Parametro par = p.getParametro();
            try (PreparedStatement ps2 = cn.prepareStatement(sqlParam)) {
                ps2.setInt(1, idGenerado);
                ps2.setString(2, par.getFuncionPared());
                ps2.setDouble(3, par.getAlturaCm());
                ps2.setDouble(4, par.getEspesorCm());
                ps2.setDouble(5, par.getVolumenObjetivoMl());
                ps2.setDouble(6, par.getVolumenCalculadoMl());
                ps2.setDouble(7, par.getAreaParedCm2());
                ps2.setDouble(8, par.getVolumenMaterialCm3());
                ps2.setDouble(9, par.getCostoEstimado());
                ps2.executeUpdate();
            }

            cn.commit();
            return idGenerado;
        } catch (SQLException e) {
            if (cn != null) try { cn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return -1;
        } finally {
            if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignored) {}
        }
    }

    public boolean actualizarExistente(Proyecto p) {
        String sqlProy = "UPDATE proyectos SET id_material = ?, nombre_proyecto = ? WHERE id_proyecto = ?";
        String sqlParam = "UPDATE parametros SET funcion_pared = ?, altura_cm = ?, espesor_cm = ?, " +
                          "volumen_objetivo_ml = ?, volumen_calculado_ml = ?, area_pared_cm2 = ?, " +
                          "volumen_material_cm3 = ?, costo_estimado = ? WHERE id_proyecto = ?";
        try (Connection cn = ConexionBD.conectar()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps1 = cn.prepareStatement(sqlProy);
                 PreparedStatement ps2 = cn.prepareStatement(sqlParam)) {
                ps1.setInt(1, p.getIdMaterial());
                ps1.setString(2, p.getNombreProyecto());
                ps1.setInt(3, p.getIdProyecto());
                ps1.executeUpdate();

                Parametro par = p.getParametro();
                ps2.setString(1, par.getFuncionPared());
                ps2.setDouble(2, par.getAlturaCm());
                ps2.setDouble(3, par.getEspesorCm());
                ps2.setDouble(4, par.getVolumenObjetivoMl());
                ps2.setDouble(5, par.getVolumenCalculadoMl());
                ps2.setDouble(6, par.getAreaParedCm2());
                ps2.setDouble(7, par.getVolumenMaterialCm3());
                ps2.setDouble(8, par.getCostoEstimado());
                ps2.setInt(9, p.getIdProyecto());
                ps2.executeUpdate();

                cn.commit();
                return true;
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Proyecto> listarPorUsuario(int idUsuario, String filtro) {
        List<Proyecto> lista = new ArrayList<>();
        String sql = "SELECT p.id_proyecto, p.id_usuario, p.id_material, p.nombre_proyecto, " +
                     "DATE_FORMAT(p.fecha_creacion, '%Y-%m-%d %H:%i') AS f_crea, " +
                     "DATE_FORMAT(p.fecha_modificacion, '%Y-%m-%d %H:%i') AS f_mod, " +
                     "pa.id_parametro, pa.funcion_pared, pa.altura_cm, pa.espesor_cm, pa.volumen_objetivo_ml, " +
                     "pa.volumen_calculado_ml, pa.area_pared_cm2, pa.volumen_material_cm3, pa.costo_estimado, " +
                     "m.nombre_material, m.densidad_gcm3, m.precio_cm3 " +
                     "FROM proyectos p " +
                     "INNER JOIN parametros pa ON p.id_proyecto = pa.id_proyecto " +
                     "INNER JOIN materiales m ON p.id_material = m.id_material " +
                     "WHERE p.id_usuario = ? AND (p.nombre_proyecto LIKE ? OR pa.funcion_pared LIKE ?) " +
                     "ORDER BY p.fecha_modificacion DESC";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            String patron = "%" + (filtro == null ? "" : filtro.trim()) + "%";
            ps.setInt(1, idUsuario);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Proyecto p = new Proyecto();
                    p.setIdProyecto(rs.getInt("id_proyecto"));
                    p.setIdUsuario(rs.getInt("id_usuario"));
                    p.setIdMaterial(rs.getInt("id_material"));
                    p.setNombreProyecto(rs.getString("nombre_proyecto"));
                    p.setFechaCreacion(rs.getString("f_crea"));
                    p.setFechaModificacion(rs.getString("f_mod"));

                    Parametro pa = new Parametro();
                    pa.setIdParametro(rs.getInt("id_parametro"));
                    pa.setIdProyecto(p.getIdProyecto());
                    pa.setFuncionPared(rs.getString("funcion_pared"));
                    pa.setAlturaCm(rs.getDouble("altura_cm"));
                    pa.setEspesorCm(rs.getDouble("espesor_cm"));
                    pa.setVolumenObjetivoMl(rs.getDouble("volumen_objetivo_ml"));
                    pa.setVolumenCalculadoMl(rs.getDouble("volumen_calculado_ml"));
                    pa.setAreaParedCm2(rs.getDouble("area_pared_cm2"));
                    pa.setVolumenMaterialCm3(rs.getDouble("volumen_material_cm3"));
                    pa.setCostoEstimado(rs.getDouble("costo_estimado"));
                    p.setParametro(pa);

                    p.setMaterial(new Material(
                        rs.getInt("id_material"),
                        rs.getString("nombre_material"),
                        rs.getDouble("densidad_gcm3"),
                        rs.getDouble("precio_cm3")
                    ));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean renombrar(int idProyecto, String nuevoNombre) {
        String sql = "UPDATE proyectos SET nombre_proyecto = ? WHERE id_proyecto = ?";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre.trim());
            ps.setInt(2, idProyecto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int idProyecto) {
        String sql = "DELETE FROM proyectos WHERE id_proyecto = ?";
        try (Connection cn = ConexionBD.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idProyecto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}