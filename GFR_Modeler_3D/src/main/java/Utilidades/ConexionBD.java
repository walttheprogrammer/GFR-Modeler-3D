package Utilidades;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class ConexionBD {
    private static ConexionBD instancia;    
    private static final String URL = "jdbc:mysql://localhost:3306/bd_gfr_modeler?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = ""; //
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
    public static boolean verificarConexion() {
        try (Connection cn = conectar()) {
            return cn != null && !cn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    public static ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }
}