package Controladores;

import DAO.*;
import Modelo.*;
import Vistas.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Arrays;
import javax.swing.*;

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
        vista.getTxtContraseña().setText("");
        vista.getLblRegistrarse().setCursor(new Cursor(Cursor.HAND_CURSOR));
        URL urlIconOriginal = getClass().getResource("/Utilidades/GFR_Modeler_3D_icon.png");
        URL urlIcon128 = getClass().getResource("/Utilidades/GFR_Modeler_3D_icon_128.png");
        if (urlIconOriginal != null && urlIcon128 != null) {
            vista.setIconImages(Arrays.asList(
                    new ImageIcon(urlIcon128).getImage(),
                    new ImageIcon(urlIconOriginal).getImage()
            ));
        } else if (urlIconOriginal != null) {
            vista.setIconImage(new ImageIcon(urlIconOriginal).getImage());
        }
        // Registrar ActionListeners centralizados
        vista.getBtnIniciarSesion().addActionListener(this);
        vista.getTxtContraseña().addActionListener(this); // Iniciar sesión con tecla Enter
        vista.getTxtUsuario().addActionListener(this);
        vista.getBtnSeleccionarPlantilla1().addActionListener(this); // Botón de registro en dlg_registrarusuario

        // Evento de clic sobre el enlace de registro
        vista.getLblRegistrarse().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirDialogoRegistro();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object fuente = e.getSource();

        if (fuente == vista.getBtnIniciarSesion() || fuente == vista.getTxtContraseña() || fuente == vista.getTxtUsuario()) {
            procesarLogin();
        } else if (fuente == vista.getBtnSeleccionarPlantilla1()) {
            procesarRegistro();
        }
    }

    private void abrirDialogoRegistro() {
        limpiarCamposRegistro();
        vista.getDlgRegistrarusuario().pack();
        vista.getDlgRegistrarusuario().setLocationRelativeTo(vista);
        vista.getDlgRegistrarusuario().setVisible(true);
    }

    private void procesarLogin() {
        String user = vista.getTxtUsuario().getText().trim();
        String pass = new String(vista.getTxtContraseña().getPassword());

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
        String nombres = vista.getTxtNombresUsuario().getText().trim();
        String apellidos = vista.getTxtApellidosUsuario().getText().trim();
        String user = vista.getTxtUsuarioReg().getText().trim();
        String correo = vista.getTxtCorreoUsuario().getText().trim();
        String pass1 = new String(vista.getTxtContraseñaReg().getPassword());
        String pass2 = new String(vista.getTxtContraseñaRegConf().getPassword());

        if (nombres.isEmpty() || apellidos.isEmpty() || user.isEmpty() || correo.isEmpty() || pass1.isEmpty()) {
            JOptionPane.showMessageDialog(vista.getDlgRegistrarusuario(),
                    "Complete todos los campos del formulario.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass1.equals(pass2)) {
            JOptionPane.showMessageDialog(vista.getDlgRegistrarusuario(),
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
            JOptionPane.showMessageDialog(vista.getDlgRegistrarusuario(),
                    "Usuario registrado correctamente. Ya puede iniciar sesión.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            vista.getTxtUsuario().setText(user);
            vista.getTxtContraseña().setText("");
            vista.getDlgRegistrarusuario().dispose();
        } else {
            JOptionPane.showMessageDialog(vista.getDlgRegistrarusuario(),
                    "No se pudo registrar el usuario (el usuario o correo ya existe).",
                    "Error de registro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCamposRegistro() {
        vista.getTxtNombresUsuario().setText("");
        vista.getTxtApellidosUsuario().setText("");
        vista.getTxtUsuarioReg().setText("");
        vista.getTxtCorreoUsuario().setText("");
        vista.getTxtContraseñaReg().setText("");
        vista.getTxtContraseñaRegConf().setText("");
    }
}
