package Main;
import Controladores.CtrlLogin;
import Vistas.frmLogin;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;

public class main {
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(() -> {
            frmLogin login = new frmLogin();
            new CtrlLogin(login);
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}