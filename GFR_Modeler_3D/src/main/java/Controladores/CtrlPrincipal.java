package Controladores;

import DAO.MaterialDAO;
import DAO.ProyectoDAO;
import Modelo.CotaGraduacion;
import Modelo.Material;
import Modelo.Parametro;
import Modelo.Proyecto;
import Modelo.ResultadoCalculo;
import Modelo.Usuario;
import Utilidades.ConexionBD;
import Vistas.frmLogin;
import Vistas.frmPrincipal;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class CtrlPrincipal implements ActionListener {

    private final frmPrincipal vista;
    private final Usuario usuarioActual;
    private final ProyectoDAO proyectoDAO;
    private final MaterialDAO materialDAO;

    // 4 Controladores orquestados
    private final CtrlMotorCalculo motorCalculo;
    private final CtrlGenerador2D generador2D;
    private final CtrlGenerador3D generador3D;
    // CtrlExportador opera mediante métodos estáticos

    // Estado del modelo y sesión
    private Proyecto proyectoActual;
    private Material materialActual;
    private ResultadoCalculo resultadoActual;
    private List<Proyecto> listaProyectosMemoria;
    private List<Material> listaMaterialesMemoria;
    private int plantillaSeleccionada = 1;
    private boolean modoWireframe = false;

    public CtrlPrincipal(frmPrincipal vista, Usuario usuarioActual) {
        this.vista = vista;
        this.usuarioActual = usuarioActual;
        this.proyectoDAO = new ProyectoDAO();
        this.materialDAO = new MaterialDAO();

        // Instanciación de controladores auxiliares conectando las vistas de frmPrincipal
        this.motorCalculo = new CtrlMotorCalculo();
        this.generador2D = new CtrlGenerador2D(vista.controlador2D, vista.lblCursorPosicion);
        this.generador3D = new CtrlGenerador3D(vista.controlador3D);

        inicializar();
    }

    private void inicializar() {
        // Título de la ventana con datos del usuario activo
        if (usuarioActual != null) {
            String nom = usuarioActual.getNombreCompleto();
            if (nom == null || nom.trim().isEmpty()) {
                nom = (usuarioActual.getNombres() + " " + usuarioActual.getApellidos()).trim();
            }
            if (nom.isEmpty()) nom = usuarioActual.getUsuario();
            vista.setTitle("GFR Modeler 3D — Usuario: " + nom + " (@" + usuarioActual.getUsuario() + ")");
        } else {
            vista.setTitle("GFR Modeler 3D — Modelado y Cálculo de Envases");
        }

        // Estado inicial de la base de datos y tolerancia
        actualizarEstadoConexionBD();
        vista.lblintegracion.setText("±0.018%");

        // Inicializar materiales
        cargarMaterialesDesdeBD();

        // Configurar modelo de la tabla de variables y cotas
        configurarTablaVariables();

        // Listener de selección en tabla de materiales
        vista.tbl_materiales.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = vista.tbl_materiales.getSelectedRow();
                if (fila >= 0 && listaMaterialesMemoria != null && fila < listaMaterialesMemoria.size()) {
                    Material m = listaMaterialesMemoria.get(fila);
                    vista.txtDensidad.setText(String.format(Locale.US, "%.3f", m.getDensidadGcm3()));
                    vista.txtPrecio.setText(String.format(Locale.US, "%.2f", m.getPrecioCm3()));
                }
            }
        });

        // Registrar ActionListeners centralizados
        registrarEventos();

        // Cargar plantilla por defecto (Vaso estándar 350 ml) y ejecutar cálculo inicial
        seleccionarPlantilla(1);
        ejecutarCalculoCompleto();
    }

    private void registrarEventos() {
        // Tab Archivo
        vista.btnNuevo.addActionListener(this);
        vista.btnAbrir.addActionListener(this);
        vista.btnGuardar.addActionListener(this);
        vista.btnActualizar.addActionListener(this);
        vista.btnExportarPNG.addActionListener(this);
        vista.btnExportarCSV.addActionListener(this);
        vista.btnExportarSTIL.addActionListener(this);
        vista.btnExportarDXF.addActionListener(this);
        vista.btnExportarPDF.addActionListener(this);
        vista.btnExportarPNG1.addActionListener(this); // Acerca de
        vista.btnCerrarSesion.addActionListener(this);

        // Tab Modelado
        vista.btnLagrange.addActionListener(this); // Perfil del envase
        vista.btnGradienteDerivadas.addActionListener(this); // Marcas de graduación
        vista.btnCurvasnivel.addActionListener(this); // Curvas de nivel
        vista.btnSuperficie.addActionListener(this); // Malla 3D alambre/sólido
        vista.btnPlantillas.addActionListener(this);

        // Tab Cálculo
        vista.btnVolumen.addActionListener(this);
        vista.btnAlturaObjetivo.addActionListener(this);
        vista.btnTablaGradiente.addActionListener(this);
        vista.btnAreaPared.addActionListener(this);
        vista.btnResina.addActionListener(this);
        vista.btnCosto.addActionListener(this);
        vista.btnCalculartodo.addActionListener(this);
        vista.btnCalcularVaso.addActionListener(this);

        // Tab Análisis
        vista.btnAnalizartodo.addActionListener(this);
        vista.btnAproximaciones.addActionListener(this);
        vista.btnSensibilidad.addActionListener(this);
        vista.btnMateriales.addActionListener(this);
        vista.btnDesmoldar.addActionListener(this);

        // Botones de reporte de consola A, B, C
        vista.btnIniciarSesion2.addActionListener(this); // Botón A
        vista.btnIniciarSesion1.addActionListener(this); // Botón B
        vista.btnIniciarSesion3.addActionListener(this); // Botón C

        // JDialog dlg_abrirproyecto
        vista.btnBuscar.addActionListener(this);
        vista.txtBuscar.addActionListener(this);
        vista.btnAbrirProyecto.addActionListener(this);
        vista.btnCambiarNombre.addActionListener(this);
        vista.btnEliminarProyecto.addActionListener(this);

        // JDialog dlg_plantillas
        vista.btnVaso.addActionListener(this);
        vista.btnTazon.addActionListener(this);
        vista.btnVasoabombado.addActionListener(this);
        vista.btnCopa.addActionListener(this);
        vista.btnSeleccionarPlantilla.addActionListener(this);

        // JDialog dlg_materiales
        vista.btnSeleccionarMaterial.addActionListener(this);

        // JDialog dlg_sensibilidad
        vista.btnSimular.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();

        // ==========================================
        // 1. ACCIONES DE ARCHIVO
        // ==========================================
        if (s == vista.btnNuevo) {
            accionNuevo();
        } else if (s == vista.btnAbrir) {
            abrirDialogoProyectos();
        } else if (s == vista.btnGuardar) {
            accionGuardarProyecto();
        } else if (s == vista.btnActualizar) {
            accionActualizarProyecto();
        } else if (s == vista.btnExportarPNG) {
            exportarGraficaPNG();
        } else if (s == vista.btnExportarCSV) {
            exportarDatosCSV();
        } else if (s == vista.btnExportarSTIL) {
            exportarMallaSTL();
        } else if (s == vista.btnExportarDXF) {
            exportarPlanosDXF();
        } else if (s == vista.btnExportarPDF) {
            exportarInformePDF();
        } else if (s == vista.btnExportarPNG1) {
            mostrarAcercaDe();
        } else if (s == vista.btnCerrarSesion) {
            cerrarSesion();

        // ==========================================
        // 2. ACCIONES DE MODELADO Y GRÁFICOS 2D/3D
        // ==========================================
        } else if (s == vista.btnCurvasnivel) {
            mostrarCurvasNivel();
        } else if (s == vista.btnLagrange) {
            mostrarPerfilEnvase();
        } else if (s == vista.btnGradienteDerivadas) {
            mostrarMarcasGraduacion();
        } else if (s == vista.btnSuperficie) {
            alternarSuperficie3D();
        } else if (s == vista.btnPlantillas) {
            abrirDialogoPlantillas();

        // ==========================================
        // 3. ACCIONES DE CÁLCULO
        // ==========================================
        } else if (s == vista.btnCalcularVaso || s == vista.btnCalculartodo) {
            ejecutarCalculoCompleto();
        } else if (s == vista.btnVolumen) {
            mostrarDetalleVolumen();
        } else if (s == vista.btnAlturaObjetivo) {
            mostrarDetalleAltura();
        } else if (s == vista.btnTablaGradiente) {
            mostrarDetalleTablaGradiente();
        } else if (s == vista.btnAreaPared) {
            mostrarDetalleArea();
        } else if (s == vista.btnResina) {
            mostrarDetalleResina();
        } else if (s == vista.btnCosto) {
            mostrarDetalleCosto();

        // ==========================================
        // 4. ACCIONES DE ANÁLISIS
        // ==========================================
        } else if (s == vista.btnAnalizartodo) {
            ejecutarAnalisisCompletoConsola();
        } else if (s == vista.btnAproximaciones) {
            mostrarDetalleAproximaciones();
        } else if (s == vista.btnDesmoldar) {
            mostrarDetalleDesmolde();
        } else if (s == vista.btnSensibilidad) {
            abrirDialogoSensibilidad();
        } else if (s == vista.btnMateriales) {
            abrirDialogoMateriales();

        // ==========================================
        // 5. BOTONES RÁPIDOS DE CONSOLA (A, B, C)
        // ==========================================
        } else if (s == vista.btnIniciarSesion2) {
            mostrarReporteA();
        } else if (s == vista.btnIniciarSesion1) {
            mostrarReporteB();
        } else if (s == vista.btnIniciarSesion3) {
            mostrarReporteC();

        // ==========================================
        // 6. DIÁLOGO: ABRIR PROYECTO
        // ==========================================
        } else if (s == vista.btnBuscar || s == vista.txtBuscar) {
            filtrarProyectos();
        } else if (s == vista.btnAbrirProyecto) {
            cargarProyectoSeleccionado();
        } else if (s == vista.btnCambiarNombre) {
            renombrarProyectoSeleccionado();
        } else if (s == vista.btnEliminarProyecto) {
            eliminarProyectoSeleccionado();

        // ==========================================
        // 7. DIÁLOGO: PLANTILLAS
        // ==========================================
        } else if (s == vista.btnVaso) {
            seleccionarPlantilla(1);
        } else if (s == vista.btnTazon) {
            seleccionarPlantilla(2);
        } else if (s == vista.btnVasoabombado) {
            seleccionarPlantilla(3);
        } else if (s == vista.btnCopa) {
            seleccionarPlantilla(4);
        } else if (s == vista.btnSeleccionarPlantilla) {
            aplicarPlantilla();

        // ==========================================
        // 8. DIÁLOGO: MATERIALES
        // ==========================================
        } else if (s == vista.btnSeleccionarMaterial) {
            aplicarMaterialSeleccionado();

        // ==========================================
        // 9. DIÁLOGO: SENSIBILIDAD
        // ==========================================
        } else if (s == vista.btnSimular) {
            ejecutarSimulacionSensibilidad();
        }
    }

    // =========================================================================
    // NÚCLEO DE CÁLCULO Y ORQUESTACIÓN PRINCIPAL
    // =========================================================================

    private void ejecutarCalculoCompleto() {
        String funcionTexto = vista.txtFuncion1.getText().trim();
        double alturaH = ((Number) vista.spn_altura.getValue()).doubleValue();
        double espesorE = ((Number) vista.spn_espesor.getValue()).doubleValue();
        double volumenObj = ((Number) vista.spn_volumen.getValue()).doubleValue();

        if (funcionTexto.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese la función f(x, y) de la pared del envase.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (espesorE <= 0) {
            espesorE = 0.20;
            vista.spn_espesor.setValue(espesorE);
        }

        if (materialActual == null) {
            materialActual = new Material(1, "Vidrio Templado", 2.50, 0.18);
        }

        try {
            // 1. Delegar cálculo integral y numérico al Motor de Cálculo
            ResultadoCalculo res = motorCalculo.ejecutarAnalisisCompleto(funcionTexto, alturaH, espesorE, volumenObj, materialActual);
            this.resultadoActual = res;

            // Si la altura fue calculada automáticamente por volumen objetivo, actualizar el spinner
            vista.spn_altura.setValue(res.alturaIngresada);

            // 2. Delegar generación y renderizado 2D (por defecto Curvas de Nivel)
            generador2D.mostrarCurvasDeNivel(motorCalculo, res);

            // 3. Delegar actualización de la malla 3D en JavaFX
            generador3D.actualizarEnvase3D(motorCalculo, res.alturaIngresada, res.espesor);

            // 4. Actualizar tabla de graduación y cotas
            actualizarTablaVariables(res.cotas);

            // 5. Imprimir reporte ejecutivo en la consola
            vista.tA_consola.setText(res.reporteA);

            // Actualizar datos de sensibilidad ideales
            vista.lbl_volumen_ideal.setText(String.format(Locale.US, "%.2f ml", res.volumenCalculado));
            vista.lbl_estado_ideal.setText(res.desmoldable ? "APROBADO" : "REVISAR");

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(vista, "Error en la expresión matemática: " + ex.getMessage(), "Error de Sintaxis", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(vista, "Error durante la ejecución del análisis: " + ex.getMessage(), "Error de Cálculo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // VISTAS 2D Y 3D (TAB MODELADO)
    // =========================================================================

    private void mostrarCurvasNivel() {
        if (verificarCalculo()) {
            generador2D.mostrarCurvasDeNivel(motorCalculo, resultadoActual);
            vista.tA_consola.append("\n[2D] Mostrando Curvas de Nivel en el plano XY.");
        }
    }

    private void mostrarPerfilEnvase() {
        if (verificarCalculo()) {
            generador2D.mostrarPerfilEnvase(motorCalculo, resultadoActual);
            vista.tA_consola.append("\n[2D] Mostrando Perfil Transversal z(r) y marcas de cota.");
        }
    }

    private void mostrarMarcasGraduacion() {
        if (verificarCalculo()) {
            generador2D.mostrarCurvaGraduacion(resultadoActual);
            vista.tA_consola.append("\n[2D] Mostrando Curva de Graduación Volumétrica V(h).");
        }
    }

    private void alternarSuperficie3D() {
        if (verificarCalculo()) {
            modoWireframe = !modoWireframe;
            generador3D.setModoWireframe(modoWireframe);
            generador3D.actualizarEnvase3D(motorCalculo, resultadoActual.alturaIngresada, resultadoActual.espesor);
            vista.tA_consola.append("\n[3D] Modo visualización cambiado a: " + (modoWireframe ? "Malla de alambre (Wireframe)" : "Sólido sombreado (Shaded)"));
        }
    }

    // =========================================================================
    // BOTONES DE REPORTE Y CÁLCULO INDIVIDUAL (TAB CÁLCULO Y ANÁLISIS)
    // =========================================================================

    private void mostrarDetalleVolumen() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(String.format(Locale.US,
                "=== CÁLCULO DE VOLUMEN (INTEGRAL DOBLE) ===\n" +
                "Función de la pared: z = %s\n" +
                "Altura analizada (h): %.2f cm\n" +
                "Capacidad útil V(h): %.2f ml (cm³)\n" +
                "Método: Coordenadas Polares ∬ [h - f(r,θ)] r dr dθ\n" +
                "Tolerancia de error numérico: ±0.018%%",
                resultadoActual.funcion, resultadoActual.alturaIngresada, resultadoActual.volumenCalculado));
    }

    private void mostrarDetalleAltura() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(String.format(Locale.US,
                "=== ALTURA REQUERIDA ===\n" +
                "Altura actual configurada: %.2f cm\n" +
                "Volumen objetivo meta: %.2f ml\n" +
                "Altura exacta para meta V(h)=Meta: %.2f cm\n" +
                "Radio en el borde superior: %.2f cm",
                resultadoActual.alturaIngresada, resultadoActual.volumenObjetivo,
                resultadoActual.alturaParaObjetivo, resultadoActual.radioBorde));
    }

    private void mostrarDetalleTablaGradiente() {
        if (!verificarCalculo()) return;
        actualizarTablaVariables(resultadoActual.cotas);
        vista.tA_consola.setText(String.format(Locale.US,
                "=== TABLA DE GRADUACIÓN VOLUMÉTRICA ===\n" +
                "Total de cotas generadas: %d marcas (cada 50 ml)\n" +
                "Consulte la tabla inferior para ver el detalle de nivel, radio, área y costo acumulado.",
                resultadoActual.cotas != null ? resultadoActual.cotas.size() : 0));
    }

    private void mostrarDetalleArea() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(String.format(Locale.US,
                "=== ÁREA SUPERFICIAL DE LA PARED ===\n" +
                "Integral de superficie A = ∬ sqrt(1 + ||∇f||²) dA\n" +
                "Área lateral calculada: %.2f cm²\n" +
                "Altura de referencia: %.2f cm",
                resultadoActual.areaPared, resultadoActual.alturaIngresada));
    }

    private void mostrarDetalleResina() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(String.format(Locale.US,
                "=== CONSUMO DE MATERIAL / RESINA ===\n" +
                "Material: %s\n" +
                "Densidad: %.3f g/cm³\n" +
                "Espesor de pared (e): %.2f cm\n" +
                "Volumen neto de material (A * e): %.2f cm³\n" +
                "Masa total calculada: %.2f g",
                materialActual.getNombreMaterial(), materialActual.getDensidadGcm3(),
                resultadoActual.espesor, resultadoActual.volumenMaterial, resultadoActual.masaGramos));
    }

    private void mostrarDetalleCosto() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(String.format(Locale.US,
                "=== ANÁLISIS DE COSTO UNITARIO ===\n" +
                "Material: %s\n" +
                "Precio por cm³: S/ %.2f\n" +
                "Consumo de material: %.2f cm³\n" +
                "COSTO DE MATERIAL POR ENVASE: S/ %.2f",
                materialActual.getNombreMaterial(), materialActual.getPrecioCm3(),
                resultadoActual.volumenMaterial, resultadoActual.costoTotal));
    }

    private void ejecutarAnalisisCompletoConsola() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(resultadoActual.reporteA + "\n\n" + resultadoActual.reporteB + "\n\n" + resultadoActual.reporteC);
    }

    private void mostrarDetalleAproximaciones() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(resultadoActual.reporteC);
    }

    private void mostrarDetalleDesmolde() {
        if (!verificarCalculo()) return;
        vista.tA_consola.setText(resultadoActual.reporteB);
    }

    private void mostrarReporteA() {
        if (verificarCalculo()) vista.tA_consola.setText(resultadoActual.reporteA);
    }

    private void mostrarReporteB() {
        if (verificarCalculo()) vista.tA_consola.setText(resultadoActual.reporteB);
    }

    private void mostrarReporteC() {
        if (verificarCalculo()) vista.tA_consola.setText(resultadoActual.reporteC);
    }

    // =========================================================================
    // EXPORTACIÓN (DELEGACIÓN A CtrlExportador)
    // =========================================================================

    private void exportarGraficaPNG() {
        if (!verificarCalculo()) return;
        BufferedImage img = generador2D.capturarImagen2D(900, 650);
        if (img == null) {
            JOptionPane.showMessageDialog(vista, "No hay gráfica 2D disponible para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Gráfica 2D a PNG");
        fc.setSelectedFile(new File("grafica_envase_2d.png"));
        fc.setFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".png");
                CtrlExportador.exportarPNG(destino, img);
                JOptionPane.showMessageDialog(vista, "Gráfica 2D guardada correctamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Error al exportar PNG: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarDatosCSV() {
        if (!verificarCalculo()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Cotas de Graduación a CSV");
        fc.setSelectedFile(new File("cotas_graduacion.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".csv");
                CtrlExportador.exportarCSV(destino, resultadoActual);
                JOptionPane.showMessageDialog(vista, "Datos exportados correctamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Error al exportar CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarMallaSTL() {
        if (!verificarCalculo()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Modelo 3D a STL (Impresión 3D)");
        fc.setSelectedFile(new File("envase_modelado_3d.stl"));
        fc.setFileFilter(new FileNameExtensionFilter("Malla Estereolitografía (*.stl)", "stl"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".stl");
                CtrlExportador.exportarSTL(destino, motorCalculo, resultadoActual.alturaIngresada, resultadoActual.espesor);
                JOptionPane.showMessageDialog(vista, "Malla STL exportada correctamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Error al exportar STL: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarPlanosDXF() {
        if (!verificarCalculo()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Planos CAD a DXF");
        fc.setSelectedFile(new File("planos_curvas_envase.dxf"));
        fc.setFileFilter(new FileNameExtensionFilter("Dibujo CAD DXF (*.dxf)", "dxf"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".dxf");
                CtrlExportador.exportarDXF(destino, motorCalculo, resultadoActual);
                JOptionPane.showMessageDialog(vista, "Planos vectoriales guardados correctamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Error al exportar DXF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarInformePDF() {
        if (!verificarCalculo()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Reporte Técnico a PDF");
        fc.setSelectedFile(new File("Reporte_Tecnico_Envase.pdf"));
        fc.setFileFilter(new FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".pdf");
                BufferedImage img2D = generador2D.capturarImagen2D(700, 360);
                BufferedImage img3D = generador3D.capturarImagen3D();
                String nombreProyecto = (proyectoActual != null) ? proyectoActual.getNombreProyecto() : "Diseño Libre";

                CtrlExportador.exportarPDF(destino, nombreProyecto, resultadoActual, img2D, img3D);
                JOptionPane.showMessageDialog(vista, "Reporte técnico PDF generado correctamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Error al generar informe PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // GESTIÓN DE PROYECTOS (DAO Y JDIALOG dlg_abrirproyecto)
    // =========================================================================

    private void accionNuevo() {
        proyectoActual = null;
        seleccionarPlantilla(1);
        vista.tA_consola.setText("=== NUEVO PROYECTO INICIADO ===\nIngrese los parámetros o elija una plantilla.");
        ejecutarCalculoCompleto();
    }

    private void abrirDialogoProyectos() {
        if (usuarioActual == null) {
            JOptionPane.showMessageDialog(vista, "No se ha autenticado ningún usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarListaProyectos("");
        vista.dlg_abrirproyecto.pack();
        vista.dlg_abrirproyecto.setLocationRelativeTo(vista);
        vista.dlg_abrirproyecto.setVisible(true);
    }

    private void filtrarProyectos() {
        String texto = vista.txtBuscar.getText().trim();
        cargarListaProyectos(texto);
    }

    private void cargarListaProyectos(String filtro) {
        if (usuarioActual == null) return;
        this.listaProyectosMemoria = proyectoDAO.listarPorUsuario(usuarioActual.getIdUsuario(), filtro);

        DefaultTableModel modelo = (DefaultTableModel) vista.tbl_proyectos.getModel();
        modelo.setRowCount(0);

        if (listaProyectosMemoria != null) {
            for (Proyecto p : listaProyectosMemoria) {
                String vol = (p.getParametro() != null) ? String.format(Locale.US, "%.1f ml", p.getParametro().getVolumenCalculadoMl()) : "-";
                String func = (p.getParametro() != null) ? p.getParametro().getFuncionPared() : "-";
                modelo.addRow(new Object[]{
                    p.getNombreProyecto(),
                    func,
                    vol,
                    p.getFechaCreacion() != null ? p.getFechaCreacion() : "-",
                    p.getFechaModificacion() != null ? p.getFechaModificacion() : "-"
                });
            }
        }
    }

    private void cargarProyectoSeleccionado() {
        int fila = vista.tbl_proyectos.getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "Seleccione un proyecto de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        this.proyectoActual = p;

        if (p.getParametro() != null) {
            Parametro par = p.getParametro();
            vista.txtFuncion1.setText(par.getFuncionPared());
            vista.spn_altura.setValue(par.getAlturaCm());
            vista.spn_espesor.setValue(par.getEspesorCm());
            vista.spn_volumen.setValue(par.getVolumenObjetivoMl());
        }

        if (p.getMaterial() != null) {
            this.materialActual = p.getMaterial();
        }

        vista.dlg_abrirproyecto.setVisible(false);
        ejecutarCalculoCompleto();
        vista.tA_consola.append("\nProyecto cargado: " + p.getNombreProyecto());
    }

    private void renombrarProyectoSeleccionado() {
        int fila = vista.tbl_proyectos.getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "Seleccione un proyecto para cambiar el nombre.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        String nuevo = JOptionPane.showInputDialog(vista.dlg_abrirproyecto, "Nuevo nombre para el proyecto:", p.getNombreProyecto());
        if (nuevo != null && !nuevo.trim().isEmpty()) {
            if (proyectoDAO.renombrar(p.getIdProyecto(), nuevo.trim())) {
                JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "Nombre actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                p.setNombreProyecto(nuevo.trim());
                if (proyectoActual != null && proyectoActual.getIdProyecto() == p.getIdProyecto()) {
                    proyectoActual.setNombreProyecto(nuevo.trim());
                }
                cargarListaProyectos(vista.txtBuscar.getText().trim());
            } else {
                JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "No se pudo actualizar el nombre.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarProyectoSeleccionado() {
        int fila = vista.tbl_proyectos.getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "Seleccione un proyecto para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        int resp = JOptionPane.showConfirmDialog(vista.dlg_abrirproyecto,
                "¿Está seguro de eliminar permanentemente el proyecto '" + p.getNombreProyecto() + "'?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (resp == JOptionPane.YES_OPTION) {
            if (proyectoDAO.eliminar(p.getIdProyecto())) {
                JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "Proyecto eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                if (proyectoActual != null && proyectoActual.getIdProyecto() == p.getIdProyecto()) {
                    proyectoActual = null;
                }
                cargarListaProyectos(vista.txtBuscar.getText().trim());
            } else {
                JOptionPane.showMessageDialog(vista.dlg_abrirproyecto, "No se pudo eliminar el proyecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void accionGuardarProyecto() {
        if (!verificarCalculo()) return;

        if (proyectoActual == null || proyectoActual.getIdProyecto() == 0) {
            String nombre = JOptionPane.showInputDialog(vista, "Ingrese el nombre del proyecto:", "Guardar Proyecto", JOptionPane.QUESTION_MESSAGE);
            if (nombre == null || nombre.trim().isEmpty()) return;

            Proyecto nuevo = new Proyecto();
            nuevo.setIdUsuario(usuarioActual != null ? usuarioActual.getIdUsuario() : 1);
            nuevo.setIdMaterial(materialActual != null ? materialActual.getIdMaterial() : 1);
            nuevo.setNombreProyecto(nombre.trim());

            Parametro par = new Parametro();
            par.setFuncionPared(resultadoActual.funcion);
            par.setAlturaCm(resultadoActual.alturaIngresada);
            par.setEspesorCm(resultadoActual.espesor);
            par.setVolumenObjetivoMl(resultadoActual.volumenObjetivo);
            par.setVolumenCalculadoMl(resultadoActual.volumenCalculado);
            par.setAreaParedCm2(resultadoActual.areaPared);
            par.setVolumenMaterialCm3(resultadoActual.volumenMaterial);
            par.setCostoEstimado(resultadoActual.costoTotal);
            nuevo.setParametro(par);

            int idGenerado = proyectoDAO.guardarNuevo(nuevo);
            if (idGenerado > 0) {
                nuevo.setIdProyecto(idGenerado);
                this.proyectoActual = nuevo;
                JOptionPane.showMessageDialog(vista, "Proyecto '" + nombre + "' guardado con éxito.", "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(vista, "No se pudo guardar el proyecto en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            accionActualizarProyecto();
        }
    }

    private void accionActualizarProyecto() {
        if (proyectoActual == null || proyectoActual.getIdProyecto() == 0) {
            accionGuardarProyecto();
            return;
        }

        if (!verificarCalculo()) return;

        proyectoActual.setIdMaterial(materialActual != null ? materialActual.getIdMaterial() : 1);
        Parametro par = proyectoActual.getParametro();
        if (par == null) par = new Parametro();

        par.setFuncionPared(resultadoActual.funcion);
        par.setAlturaCm(resultadoActual.alturaIngresada);
        par.setEspesorCm(resultadoActual.espesor);
        par.setVolumenObjetivoMl(resultadoActual.volumenObjetivo);
        par.setVolumenCalculadoMl(resultadoActual.volumenCalculado);
        par.setAreaParedCm2(resultadoActual.areaPared);
        par.setVolumenMaterialCm3(resultadoActual.volumenMaterial);
        par.setCostoEstimado(resultadoActual.costoTotal);
        proyectoActual.setParametro(par);

        if (proyectoDAO.actualizarExistente(proyectoActual)) {
            JOptionPane.showMessageDialog(vista, "Proyecto '" + proyectoActual.getNombreProyecto() + "' actualizado con éxito.", "Actualizado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo actualizar el proyecto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PLANTILLAS (JDIALOG dlg_plantillas)
    // =========================================================================

    private void abrirDialogoPlantillas() {
        vista.dlg_plantillas.pack();
        vista.dlg_plantillas.setLocationRelativeTo(vista);
        vista.dlg_plantillas.setVisible(true);
    }

    private void seleccionarPlantilla(int id) {
        this.plantillaSeleccionada = id;
        switch (id) {
            case 1: // Vaso estándar (350 ml)
                vista.txtFuncion1.setText("0.25 * (x^2 + y^2)");
                vista.spn_altura.setValue(7.5);
                vista.spn_espesor.setValue(0.20);
                vista.spn_volumen.setValue(350.0);
                break;
            case 2: // Tazón / Bowl (500 ml)
                vista.txtFuncion1.setText("0.15 * (x^2 + y^2)");
                vista.spn_altura.setValue(6.0);
                vista.spn_espesor.setValue(0.30);
                vista.spn_volumen.setValue(500.0);
                break;
            case 3: // Vaso abombado (500 ml)
                vista.txtFuncion1.setText("0.20 * (x^2 + y^2)");
                vista.spn_altura.setValue(10.0);
                vista.spn_espesor.setValue(0.25);
                vista.spn_volumen.setValue(500.0);
                break;
            case 4: // Copa (250 ml)
                vista.txtFuncion1.setText("0.35 * (x^2 + y^2)");
                vista.spn_altura.setValue(8.5);
                vista.spn_espesor.setValue(0.15);
                vista.spn_volumen.setValue(250.0);
                break;
        }
    }

    private void aplicarPlantilla() {
        seleccionarPlantilla(plantillaSeleccionada);
        vista.dlg_plantillas.setVisible(false);
        ejecutarCalculoCompleto();
    }

    // =========================================================================
    // GESTIÓN DE MATERIALES (JDIALOG dlg_materiales)
    // =========================================================================

    private void abrirDialogoMateriales() {
        cargarMaterialesDesdeBD();
        vista.dlg_materiales.pack();
        vista.dlg_materiales.setLocationRelativeTo(vista);
        vista.dlg_materiales.setVisible(true);
    }

    private void cargarMaterialesDesdeBD() {
        this.listaMaterialesMemoria = materialDAO.listar();
        if (listaMaterialesMemoria == null || listaMaterialesMemoria.isEmpty()) {
            listaMaterialesMemoria = new ArrayList<>();
            listaMaterialesMemoria.add(new Material(1, "Vidrio Templado", 2.500, 0.18));
            listaMaterialesMemoria.add(new Material(2, "Resina PET", 1.380, 0.12));
            listaMaterialesMemoria.add(new Material(3, "Polipropileno (PP)", 0.910, 0.09));
        }

        if (materialActual == null && !listaMaterialesMemoria.isEmpty()) {
            materialActual = listaMaterialesMemoria.get(0);
        }

        DefaultTableModel modelo = (DefaultTableModel) vista.tbl_materiales.getModel();
        modelo.setRowCount(0);

        for (Material m : listaMaterialesMemoria) {
            modelo.addRow(new Object[]{
                m.getNombreMaterial(),
                String.format(Locale.US, "%.3f", m.getDensidadGcm3()),
                String.format(Locale.US, "%.2f", m.getPrecioCm3())
            });
        }
    }

    private void aplicarMaterialSeleccionado() {
        int fila = vista.tbl_materiales.getSelectedRow();
        if (fila >= 0 && listaMaterialesMemoria != null && fila < listaMaterialesMemoria.size()) {
            Material m = listaMaterialesMemoria.get(fila);

            // Permite actualizar en base de datos si el usuario editó densidad o precio
            try {
                String strDens = vista.txtDensidad.getText().trim();
                String strPrec = vista.txtPrecio.getText().trim();
                if (!strDens.isEmpty() && !strPrec.isEmpty()) {
                    double dens = Double.parseDouble(strDens);
                    double prec = Double.parseDouble(strPrec);
                    if (dens > 0 && prec > 0) {
                        materialDAO.actualizar(m.getIdMaterial(), dens, prec);
                        m.setDensidadGcm3(dens);
                        m.setPrecioCm3(prec);
                    }
                }
            } catch (Exception ignored) {}

            this.materialActual = m;
            vista.dlg_materiales.setVisible(false);

            if (resultadoActual != null) {
                ejecutarCalculoCompleto();
            }
        } else {
            vista.dlg_materiales.setVisible(false);
        }
    }

    // =========================================================================
    // ANÁLISIS DE SENSIBILIDAD (JDIALOG dlg_sensibilidad)
    // =========================================================================

    private void abrirDialogoSensibilidad() {
        if (!verificarCalculo()) return;

        vista.lbl_volumen_ideal.setText(String.format(Locale.US, "%.2f ml", resultadoActual.volumenCalculado));
        vista.lbl_estado_ideal.setText((resultadoActual.volumenObjetivo > 0 && Math.abs(resultadoActual.volumenCalculado - resultadoActual.volumenObjetivo) <= 5.0) ? "CUMPLE META" : "NOMINAL");
        vista.spn_coeficiente.setValue(1.0);
        vista.lbl_volumen_simulado.setText("-");
        vista.lbl_estado_simulado.setText("-");

        vista.dlg_sensibilidad.pack();
        vista.dlg_sensibilidad.setLocationRelativeTo(vista);
        vista.dlg_sensibilidad.setVisible(true);
    }

    private void ejecutarSimulacionSensibilidad() {
        if (!verificarCalculo()) return;

        double factorK = ((Number) vista.spn_coeficiente.getValue()).doubleValue();
        if (factorK <= 0) factorK = 1.0;

        double volSimulado = motorCalculo.simularDesviacionCurvatura(resultadoActual.funcion, factorK, resultadoActual.alturaIngresada);
        vista.lbl_volumen_simulado.setText(String.format(Locale.US, "%.2f ml", volSimulado));

        double desviacionPorcentual = ((volSimulado - resultadoActual.volumenCalculado) / resultadoActual.volumenCalculado) * 100.0;
        String dictamen = Math.abs(desviacionPorcentual) <= 5.0 ? "TOLERABLE (±5%)" : "CRÍTICO (>5%)";
        vista.lbl_estado_simulado.setText(String.format(Locale.US, "%+.2f %% (%s)", desviacionPorcentual, dictamen));
    }

    // =========================================================================
    // UTILIDADES DE INTERFAZ Y VALIDACIÓN
    // =========================================================================

    private void configurarTablaVariables() {
        String[] columnas = {"Nivel (ml)", "Altura (cm)", "Radio (cm)", "Área (cm²)", "Insumo (cm³)", "Costo (S/.)"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        vista.tbl_variables.setModel(modelo);
    }

    private void actualizarTablaVariables(List<CotaGraduacion> cotas) {
        DefaultTableModel modelo = (DefaultTableModel) vista.tbl_variables.getModel();
        modelo.setRowCount(0);
        if (cotas != null) {
            for (CotaGraduacion c : cotas) {
                modelo.addRow(new Object[]{
                    String.format(Locale.US, "%.1f", c.getVolumenMl()),
                    String.format(Locale.US, "%.2f", c.getAlturaCm()),
                    String.format(Locale.US, "%.2f", c.getRadioCm()),
                    String.format(Locale.US, "%.2f", c.getAreaAcumuladaCm2()),
                    String.format(Locale.US, "%.2f", c.getResinaAcumuladaCm3()),
                    String.format(Locale.US, "%.2f", c.getCostoAcumulado())
                });
            }
        }
    }

    private void actualizarEstadoConexionBD() {
        boolean conectada = ConexionBD.verificarConexion();
        vista.lblEstadoBD.setText(conectada ? "Conectado" : "Offline");
        vista.lblEstadoBD.setForeground(conectada ? new Color(46, 204, 113) : new Color(231, 76, 60));
    }

    private boolean verificarCalculo() {
        if (resultadoActual == null) {
            ejecutarCalculoCompleto();
        }
        return resultadoActual != null;
    }

    private File normalizarExtension(File f, String extensionDeseada) {
        String ruta = f.getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(extensionDeseada.toLowerCase())) {
            return new File(ruta + extensionDeseada);
        }
        return f;
    }

    private void mostrarAcercaDe() {
        String info = "GFR Modeler 3D v1.0.0\n" +
                      "Arquitectura MVC en Java Swing con FlatLaf\n" +
                      "Cálculo Multivariable aplicado a Ingeniería de Envases\n\n" +
                      "• Autor: Walter Dario Maza Cuadrado\n" +
                      "• Institución: Universidad César Vallejo (UCV)\n" +
                      "• Motores: Apache Commons Math 3, Exp4j, JFreeChart, JavaFX 3D, OpenPDF\n" +
                      "• Base de datos: MySQL Connector/J\n\n" +
                      "© 2026 Todos los derechos reservados.";
        JOptionPane.showMessageDialog(vista, info, "Acerca de GFR Modeler 3D", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cerrarSesion() {
        int resp = JOptionPane.showConfirmDialog(vista,
                "¿Está seguro de que desea cerrar la sesión actual?",
                "Cerrar Sesión", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (resp == JOptionPane.YES_OPTION) {
            vista.dispose();
            frmLogin login = new frmLogin();
            new CtrlLogin(login);
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        }
    }
}

