package Controladores;

import DAO.UsuarioDAO;
import Modelo.Usuario;
import Vistas.frmLogin;
import Vistas.frmPrincipal;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;

public class CtrlLogin implements ActionListener {

    private final frmLogin vista;
    private final UsuarioDAO usuarioDAO;

    public CtrlLogin(frmLogin vista) {
        this.vista = vista;
        this.usuarioDAO = new UsuarioDAO();
        inicializar();
    }

    private void inicializar() {
        // Limpiar campos por defecto
        vista.txtContraseña.setText("");
        vista.lbl_registrarse.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Registrar ActionListeners centralizados
        vista.btnIniciarSesion.addActionListener(this);
        vista.txtContraseña.addActionListener(this); // Iniciar sesión con tecla Enter
        vista.txtUsuario.addActionListener(this);
        vista.btnSeleccionarPlantilla1.addActionListener(this); // Botón de registro en dlg_registrarusuario

        // Evento de clic sobre el enlace de registro
        vista.lbl_registrarse.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirDialogoRegistro();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object fuente = e.getSource();

        if (fuente == vista.btnIniciarSesion || fuente == vista.txtContraseña || fuente == vista.txtUsuario) {
            procesarLogin();
        } else if (fuente == vista.btnSeleccionarPlantilla1) {
            procesarRegistro();
        }
    }

    private void abrirDialogoRegistro() {
        limpiarCamposRegistro();
        vista.dlg_registrarusuario.pack();
        vista.dlg_registrarusuario.setLocationRelativeTo(vista);
        vista.dlg_registrarusuario.setVisible(true);
    }

    private void procesarLogin() {
        String user = vista.txtUsuario.getText().trim();
        String pass = new String(vista.txtContraseña.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "Ingrese su usuario/correo y contraseña.",
                    "Campos requeridos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario u = usuarioDAO.login(user, pass);
        if (u != null) {
            frmPrincipal principal = new frmPrincipal();
            new CtrlPrincipal(principal, u);
            principal.setLocationRelativeTo(null);
            principal.setVisible(true);
            vista.dispose();
        } else {
            JOptionPane.showMessageDialog(vista,
                    "Credenciales incorrectas o sin conexión a la base de datos.",
                    "Error de autenticación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesarRegistro() {
        String nombres = vista.txtNombresUsuario.getText().trim();
        String apellidos = vista.txtApellidosUsuario.getText().trim();
        String user = vista.txtUsuarioReg.getText().trim();
        String correo = vista.txtCorreoUsuario.getText().trim();
        String pass1 = new String(vista.txtContraseñaReg.getPassword());
        String pass2 = new String(vista.txtContraseñaRegConf.getPassword());

        if (nombres.isEmpty() || apellidos.isEmpty() || user.isEmpty() || correo.isEmpty() || pass1.isEmpty()) {
            JOptionPane.showMessageDialog(vista.dlg_registrarusuario,
                    "Complete todos los campos del formulario.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass1.equals(pass2)) {
            JOptionPane.showMessageDialog(vista.dlg_registrarusuario,
                    "Las contraseñas ingresadas no coinciden.",
                    "Error de validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombreCompleto((nombres + " " + apellidos).trim());
        nuevo.setNombres(nombres);
        nuevo.setApellidos(apellidos);
        nuevo.setUsuario(user);
        nuevo.setCorreo(correo);
        nuevo.setContraseña(pass1);

        if (usuarioDAO.registrar(nuevo)) {
            JOptionPane.showMessageDialog(vista.dlg_registrarusuario,
                    "Usuario registrado correctamente. Ya puede iniciar sesión.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            vista.txtUsuario.setText(user);
            vista.txtContraseña.setText("");
            vista.dlg_registrarusuario.dispose();
        } else {
            JOptionPane.showMessageDialog(vista.dlg_registrarusuario,
                    "No se pudo registrar el usuario (el usuario o correo ya existe).",
                    "Error de registro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCamposRegistro() {
        vista.txtNombresUsuario.setText("");
        vista.txtApellidosUsuario.setText("");
        vista.txtUsuarioReg.setText("");
        vista.txtCorreoUsuario.setText("");
        vista.txtContraseñaReg.setText("");
        vista.txtContraseñaRegConf.setText("");
    }
}
