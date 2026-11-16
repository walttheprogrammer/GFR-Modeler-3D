package Controladores;

import DAO.*;
import Modelo.*;
import Utilidades.*;
import Vistas.*;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.*;
import java.text.ParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

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
    private int vista2DActiva = 1;

    public CtrlPrincipal(frmPrincipal vista, Usuario usuarioActual) {
        this.vista = vista;
        this.usuarioActual = usuarioActual;
        this.proyectoDAO = new ProyectoDAO();
        this.materialDAO = new MaterialDAO();
        this.motorCalculo = new CtrlMotorCalculo();
        this.generador2D = new CtrlGenerador2D(vista.getControlador2D(), vista.getLblCursorPosicion());
        this.generador3D = new CtrlGenerador3D(vista.getControlador3D());
        inicializar();
    }

    private void inicializar() {
        actualizarTituloVentana();
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

        actualizarEstadoConexionBD();
        new javax.swing.Timer(4000, e -> new Thread(() -> {
            boolean conectada = ConexionBD.verificarConexion();
            SwingUtilities.invokeLater(() -> {
                vista.getLblEstadoBD().setText(conectada ? "Conectado" : "Offline");
                vista.getLblEstadoBD().setForeground(conectada ? new Color(46, 204, 113) : new Color(231, 76, 60));
            });
        }).start()).start();
        vista.getLblintegracion().setText("±0.018%");
        cargarMaterialesDesdeBD();
        configurarTablaVariables();

        vista.getTA_consola().setMargin(new Insets(10, 12, 10, 12));
        vista.getTA_consola().setLineWrap(true);
        vista.getTA_consola().setWrapStyleWord(true);
        vista.getTA_consola().setEditable(false);

        configurarTooltips();

        vista.getTblMateriales().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = vista.getTblMateriales().getSelectedRow();
                if (fila >= 0 && listaMaterialesMemoria != null && fila < listaMaterialesMemoria.size()) {
                    Material m = listaMaterialesMemoria.get(fila);
                    vista.getTxtDensidad().setText(String.format(Locale.US, "%.3f", m.getDensidadGcm3()));
                    vista.getTxtPrecio().setText(String.format(Locale.US, "%.2f", m.getPrecioCm3()));
                }
            }
        });

        registrarEventos();
        seleccionarPlantilla(1);
        ejecutarCalculoCompleto();
    }

    private void registrarEventos() {
        // Tab Archivo
        vista.getBtnNuevo().addActionListener(this);
        vista.getBtnAbrir().addActionListener(this);
        vista.getBtnGuardar().addActionListener(this);
        vista.getBtnActualizar().addActionListener(this);
        vista.getBtnExportarPNG().addActionListener(this);
        vista.getBtnExportarCSV().addActionListener(this);
        vista.getBtnExportarSTIL().addActionListener(this);
        vista.getBtnExportarDXF().addActionListener(this);
        vista.getBtnExportarPDF().addActionListener(this);
        vista.getBtnExportarPNG1().addActionListener(this); // Acerca de
        vista.getBtnCerrarSesion().addActionListener(this);

        // Tab Modelado
        vista.getBtnLagrange().addActionListener(this); // Perfil del envase
        vista.getBtnGradienteDerivadas().addActionListener(this); // Marcas de graduación
        vista.getBtnCurvasnivel().addActionListener(this); // Curvas de nivel
        vista.getBtnSuperficie().addActionListener(this); // Malla 3D alambre/sólido
        vista.getBtnPlantillas().addActionListener(this);

        // Tab Cálculo
        vista.getBtnVolumen().addActionListener(this);
        vista.getBtnAlturaObjetivo().addActionListener(this);
        vista.getBtnTablaGradiente().addActionListener(this);
        vista.getBtnAreaPared().addActionListener(this);
        vista.getBtnResina().addActionListener(this);
        vista.getBtnCosto().addActionListener(this);
        vista.getBtnCalculartodo().addActionListener(this);
        vista.getBtnCalcularVaso().addActionListener(this);

        // Tab Análisis
        vista.getBtnAnalizartodo().addActionListener(this);
        vista.getBtnAproximaciones().addActionListener(this);
        vista.getBtnSensibilidad().addActionListener(this);
        vista.getBtnMateriales().addActionListener(this);
        vista.getBtnDesmoldar().addActionListener(this);

        // Botones de reporte de consola 
        vista.getBtnReporteA().addActionListener(this);
        vista.getBtnReporteB().addActionListener(this);
        vista.getBtnReporteC().addActionListener(this);

        // JDialog dlg_abrirproyecto
        vista.getBtnBuscar().addActionListener(this);
        vista.getTxtBuscar().addActionListener(this);
        vista.getBtnAbrirProyecto().addActionListener(this);
        vista.getBtnCambiarNombre().addActionListener(this);
        vista.getBtnEliminarProyecto().addActionListener(this);

        // JDialog dlg_plantillas
        vista.getBtnVaso().addActionListener(this);
        vista.getBtnTazon().addActionListener(this);
        vista.getBtnVasoabombado().addActionListener(this);
        vista.getBtnCopa().addActionListener(this);
        vista.getBtnSeleccionarPlantilla().addActionListener(this);

        // JDialog dlg_materiales
        vista.getBtnSeleccionarMaterial().addActionListener(this);

        // JDialog dlg_sensibilidad
        vista.getBtnSimular().addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();

        if (s == vista.getBtnNuevo()) {
            accionNuevo();
        } else if (s == vista.getBtnAbrir()) {
            abrirDialogoProyectos();
        } else if (s == vista.getBtnGuardar()) {
            accionGuardarProyecto();
        } else if (s == vista.getBtnActualizar()) {
            accionActualizarProyecto();
        } else if (s == vista.getBtnExportarPNG()) {
            exportarGraficaPNG();
        } else if (s == vista.getBtnExportarCSV()) {
            exportarDatosCSV();
        } else if (s == vista.getBtnExportarSTIL()) {
            exportarMallaSTL();
        } else if (s == vista.getBtnExportarDXF()) {
            exportarPlanosDXF();
        } else if (s == vista.getBtnExportarPDF()) {
            exportarInformePDF();
        } else if (s == vista.getBtnExportarPNG1()) {
            mostrarAcercaDe();
        } else if (s == vista.getBtnCerrarSesion()) {
            cerrarSesion();
        } else if (s == vista.getBtnCurvasnivel()) {
            mostrarCurvasNivel();
        } else if (s == vista.getBtnLagrange()) {
            mostrarPerfilEnvase();
        } else if (s == vista.getBtnGradienteDerivadas()) {
            mostrarMarcasGraduacion();
        } else if (s == vista.getBtnSuperficie()) {
            alternarSuperficie3D();
        } else if (s == vista.getBtnPlantillas()) {
            abrirDialogoPlantillas();
        } else if (s == vista.getBtnCalcularVaso() || s == vista.getBtnCalculartodo()) {
            ejecutarCalculoCompleto();
        } else if (s == vista.getBtnVolumen()) {
            mostrarDetalleVolumen();
        } else if (s == vista.getBtnAlturaObjetivo()) {
            mostrarDetalleAltura();
        } else if (s == vista.getBtnTablaGradiente()) {
            mostrarDetalleTablaGradiente();
        } else if (s == vista.getBtnAreaPared()) {
            mostrarDetalleArea();
        } else if (s == vista.getBtnResina()) {
            mostrarDetalleResina();
        } else if (s == vista.getBtnCosto()) {
            mostrarDetalleCosto();
        } else if (s == vista.getBtnAnalizartodo()) {
            ejecutarAnalisisCompletoConsola();
        } else if (s == vista.getBtnAproximaciones()) {
            mostrarDetalleAproximaciones();
        } else if (s == vista.getBtnDesmoldar()) {
            mostrarDetalleDesmolde();
        } else if (s == vista.getBtnSensibilidad()) {
            abrirDialogoSensibilidad();
        } else if (s == vista.getBtnMateriales()) {
            abrirDialogoMateriales();
        } else if (s == vista.getBtnReporteA()) {
            mostrarReporteA();
        } else if (s == vista.getBtnReporteB()) {
            mostrarReporteB();
        } else if (s == vista.getBtnReporteC()) {
            mostrarReporteC();
        } else if (s == vista.getBtnBuscar() || s == vista.getTxtBuscar()) {
            filtrarProyectos();
        } else if (s == vista.getBtnAbrirProyecto()) {
            cargarProyectoSeleccionado();
        } else if (s == vista.getBtnCambiarNombre()) {
            renombrarProyectoSeleccionado();
        } else if (s == vista.getBtnEliminarProyecto()) {
            eliminarProyectoSeleccionado();
        } else if (s == vista.getBtnVaso()) {
            seleccionarPlantilla(1);
        } else if (s == vista.getBtnTazon()) {
            seleccionarPlantilla(2);
        } else if (s == vista.getBtnVasoabombado()) {
            seleccionarPlantilla(3);
        } else if (s == vista.getBtnCopa()) {
            seleccionarPlantilla(4);
        } else if (s == vista.getBtnSeleccionarPlantilla()) {
            aplicarPlantilla();
        } else if (s == vista.getBtnSeleccionarMaterial()) {
            aplicarMaterialSeleccionado();
        } else if (s == vista.getBtnSimular()) {
            ejecutarSimulacionSensibilidad();
        }
    }
    
    private void ejecutarCalculoCompleto() {
        try {
            vista.getSpnAltura().commitEdit();
            vista.getSpnEspesor().commitEdit();
            vista.getSpnVolumen().commitEdit();
        } catch (ParseException ignored) {}

        String funcionTexto = vista.getTxtFuncion1().getText().trim();
        double alturaH = ((Number) vista.getSpnAltura().getValue()).doubleValue();
        double espesorE = ((Number) vista.getSpnEspesor().getValue()).doubleValue();
        double volumenObj = ((Number) vista.getSpnVolumen().getValue()).doubleValue();

        if (funcionTexto.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese la función f(x, y) de la pared del envase.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (espesorE <= 0) {
            espesorE = 0.20;
            vista.getSpnEspesor().setValue(espesorE);
        }
        if (materialActual == null) {
            materialActual = new Material(1, "Vidrio Templado", 2.50, 0.18);
        }

        try {
            ResultadoCalculo res = motorCalculo.ejecutarAnalisisCompleto(funcionTexto, alturaH, espesorE, volumenObj, materialActual);
            this.resultadoActual = res;

            vista.getSpnAltura().setValue(res.alturaIngresada);
            actualizarVista2DActiva(res);

            generador3D.setModoWireframe(modoWireframe);
            generador3D.actualizarEnvase3D(motorCalculo, res.alturaIngresada, res.espesor);

            actualizarTablaVariables(res.cotas);
            vista.getTA_consola().setText(formatearReporteConsola(res.reporteA));
            vista.getLblVolumenIdeal().setText(String.format(Locale.US, "%.2f ml", res.volumenCalculado));
            vista.getLblEstadoIdeal().setText(res.desmoldable ? "APROBADO" : "REVISAR");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(vista, "Error en la expresión matemática: " + ex.getMessage(), "Error de Sintaxis", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(vista, "Error durante la ejecución del análisis: " + ex.getMessage(), "Error de Cálculo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarVista2DActiva(ResultadoCalculo res) {
        if (vista2DActiva == 2) {
            generador2D.mostrarPerfilEnvase(motorCalculo, res);
        } else if (vista2DActiva == 3) {
            generador2D.mostrarCurvaGraduacion(res);
        } else {
            generador2D.mostrarCurvasDeNivel(motorCalculo, res);
        }
    }

    private void mostrarCurvasNivel() {
        if (verificarCalculo()) {
            vista2DActiva = 1;
            generador2D.mostrarCurvasDeNivel(motorCalculo, resultadoActual);
            vista.getTA_consola().append("\n[2D] Mostrando Curvas de Nivel en el plano XY.");
        }
    }

    private void mostrarPerfilEnvase() {
        if (verificarCalculo()) {
            vista2DActiva = 2;
            generador2D.mostrarPerfilEnvase(motorCalculo, resultadoActual);
            vista.getTA_consola().append("\n[2D] Mostrando Perfil Transversal z(r) y marcas de cota.");
        }
    }

    private void mostrarMarcasGraduacion() {
        if (verificarCalculo()) {
            vista2DActiva = 3;
            generador2D.mostrarCurvaGraduacion(resultadoActual);
            vista.getTA_consola().append("\n[2D] Mostrando Curva de Graduación Volumétrica V(h).");
        }
    }

    private void alternarSuperficie3D() {
        if (verificarCalculo()) {
            modoWireframe = !modoWireframe;
            generador3D.setModoWireframe(modoWireframe);
            generador3D.actualizarEnvase3D(motorCalculo, resultadoActual.alturaIngresada, resultadoActual.espesor);
            vista.getTA_consola().append("\n[3D] Modo visualización cambiado a: " + (modoWireframe ? "Malla de alambre (Wireframe)" : "Sólido sombreado (Shaded)"));
        }
    }

    private void mostrarDetalleVolumen() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== VOLUMEN V(h) ===\n" +
                "Función: z = %s\n" +
                "Altura (h): %.2f cm\n" +
                "V(h): %.2f ml\n" +
                "Integral: ∬ [h - f(r,θ)] r dr dθ\n" +
                "Tolerancia: ±%.3f%%",
                resultadoActual.funcion, resultadoActual.alturaIngresada,
                resultadoActual.volumenCalculado, resultadoActual.errorNumericoPorcentaje));
    }

    private void mostrarDetalleAltura() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== ALTURA OBJETIVO ===\n" +
                "Altura (h): %.2f cm (Objetivo: %.2f cm)\n" +
                "V objetivo: %.2f ml\n" +
                "Borde (r): %.2f cm",
                resultadoActual.alturaIngresada, resultadoActual.alturaParaObjetivo,
                resultadoActual.volumenObjetivo, resultadoActual.radioBorde));
    }

    private void mostrarDetalleTablaGradiente() {
        if (!verificarCalculo()) return;
        actualizarTablaVariables(resultadoActual.cotas);
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== TABLA DE GRADUACIÓN ===\n" +
                "Marcas: %d (cada 50 ml) | Ver tabla inferior",
                resultadoActual.cotas != null ? resultadoActual.cotas.size() : 0));
    }

    private void mostrarDetalleArea() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== ÁREA DE SUPERFICIE DE PARED ===\n" +
                "A = ∬ √(1 + ||∇f||²) dA\n" +
                "Área (A): %.2f cm²\n" +
                "Altura (h): %.2f cm",
                resultadoActual.areaPared, resultadoActual.alturaIngresada));
    }

    private void mostrarDetalleResina() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== RESINA Y MATERIAL ===\n" +
                "Material: %s | Densidad: %.3f g/cm³\n" +
                "Espesor (e): %.2f cm\n" +
                "Insumo (A·e): %.2f cm³ (%.1f g)",
                materialActual.getNombreMaterial(), materialActual.getDensidadGcm3(),
                resultadoActual.espesor, resultadoActual.volumenMaterial, resultadoActual.masaGramos));
    }

    private void mostrarDetalleCosto() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(String.format(Locale.US,
                "=== COSTO UNITARIO ===\n" +
                "Material: %s | Precio: S/ %.2f/cm³\n" +
                "Insumo: %.2f cm³\n" +
                "Costo: S/ %.2f",
                materialActual.getNombreMaterial(), materialActual.getPrecioCm3(),
                resultadoActual.volumenMaterial, resultadoActual.costoTotal));
    }

    private void ejecutarAnalisisCompletoConsola() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(resultadoActual.reporteA + "\n\n" + resultadoActual.reporteB + "\n\n" + resultadoActual.reporteC);
    }

    private void mostrarDetalleAproximaciones() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(resultadoActual.reporteC);
    }

    private void mostrarDetalleDesmolde() {
        if (!verificarCalculo()) return;
        vista.getTA_consola().setText(resultadoActual.reporteB);
    }

    private void mostrarReporteA() {
        if (verificarCalculo()) vista.getTA_consola().setText(resultadoActual.reporteA);
    }

    private void mostrarReporteB() {
        if (verificarCalculo()) vista.getTA_consola().setText(resultadoActual.reporteB);
    }

    private void mostrarReporteC() {
        if (verificarCalculo()) vista.getTA_consola().setText(resultadoActual.reporteC);
    }
    
    private String formatearReporteConsola(String reporte) {
        if (reporte == null) return "";
        return reporte.replaceAll("(?<!\\n)\\nDiagnóstico Técnico:", "\n\nDiagnóstico Técnico:");
    }
    private void exportarGraficaPNG() {
        if (!verificarCalculo()) return;
        BufferedImage img = generador2D.capturarImagen2D(900, 650);
        if (img == null) {
            JOptionPane.showMessageDialog(vista, "No hay gráfica 2D disponible para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Gráfica 2D a PNG");
        fc.setSelectedFile(new File(obtenerNombreSugeridoExportacion("grafica_envase_2d.png", ".png")));
        fc.setFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".png");
                if (!confirmarSobrescritura(destino)) return;
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
        fc.setSelectedFile(new File(obtenerNombreSugeridoExportacion("cotas_graduacion.csv", ".csv")));
        fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".csv");
                if (!confirmarSobrescritura(destino)) return;
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
        fc.setSelectedFile(new File(obtenerNombreSugeridoExportacion("envase_modelado_3d.stl", ".stl")));
        fc.setFileFilter(new FileNameExtensionFilter("Malla Estereolitografía (*.stl)", "stl"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".stl");
                if (!confirmarSobrescritura(destino)) return;
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
        fc.setSelectedFile(new File(obtenerNombreSugeridoExportacion("planos_curvas_envase.dxf", ".dxf")));
        fc.setFileFilter(new FileNameExtensionFilter("Dibujo CAD DXF (*.dxf)", "dxf"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".dxf");
                if (!confirmarSobrescritura(destino)) return;
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
        fc.setSelectedFile(new File(obtenerNombreSugeridoExportacion("Reporte_Tecnico_Envase.pdf", ".pdf")));
        fc.setFileFilter(new FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf"));

        if (fc.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION) {
            try {
                File destino = normalizarExtension(fc.getSelectedFile(), ".pdf");
                if (!confirmarSobrescritura(destino)) return;
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

    private void accionNuevo() {
        int opcion = JOptionPane.showOptionDialog(
            vista,
            "Se realizaron cambios y se perderán al generar un nuevo proyecto\n¿Desea crear un nuevo proyecto?",
            "Confirmar Nuevo Proyecto",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[]{"Sí", "No"},
            "Sí"
        );

        if (opcion != 0) {
            return;
        }

        proyectoActual = null;
        actualizarTituloVentana();
        seleccionarPlantilla(1);
        vista.getTA_consola().setText("=== NUEVO PROYECTO INICIADO ===\nIngrese los parámetros o elija una plantilla.");
        ejecutarCalculoCompleto();
    }

    private void abrirDialogoProyectos() {
        if (usuarioActual == null) {
            JOptionPane.showMessageDialog(vista, "No se ha autenticado ningún usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarListaProyectos("");
        vista.getDlgAbrirproyecto().pack();
        vista.getDlgAbrirproyecto().setLocationRelativeTo(vista);
        vista.getDlgAbrirproyecto().setVisible(true);
    }

    private void filtrarProyectos() {
        String texto = vista.getTxtBuscar().getText().trim();
        cargarListaProyectos(texto);
    }

    private void cargarListaProyectos(String filtro) {
        if (usuarioActual == null) return;
        this.listaProyectosMemoria = proyectoDAO.listarPorUsuario(usuarioActual.getIdUsuario(), filtro);

        DefaultTableModel modelo = (DefaultTableModel) vista.getTblProyectos().getModel();
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
        int fila = vista.getTblProyectos().getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "Seleccione un proyecto de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        this.proyectoActual = p;
        actualizarTituloVentana();

        if (p.getParametro() != null) {
            Parametro par = p.getParametro();
            vista.getTxtFuncion1().setText(par.getFuncionPared());
            vista.getSpnAltura().setValue(par.getAlturaCm());
            vista.getSpnEspesor().setValue(par.getEspesorCm());
            vista.getSpnVolumen().setValue(par.getVolumenObjetivoMl());
        }

        if (p.getMaterial() != null) {
            this.materialActual = p.getMaterial();
        }

        vista.getDlgAbrirproyecto().setVisible(false);
        ejecutarCalculoCompleto();
        vista.getTA_consola().append("\nProyecto cargado: " + p.getNombreProyecto());
    }

    private void renombrarProyectoSeleccionado() {
        int fila = vista.getTblProyectos().getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "Seleccione un proyecto para cambiar el nombre.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        String nuevo = JOptionPane.showInputDialog(vista.getDlgAbrirproyecto(), "Nuevo nombre para el proyecto:", p.getNombreProyecto());
        if (nuevo != null && !nuevo.trim().isEmpty()) {
            if (proyectoDAO.renombrar(p.getIdProyecto(), nuevo.trim())) {
                JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "Nombre actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                p.setNombreProyecto(nuevo.trim());
                if (proyectoActual != null && proyectoActual.getIdProyecto() == p.getIdProyecto()) {
                    proyectoActual.setNombreProyecto(nuevo.trim());
                    actualizarTituloVentana();
                }
                cargarListaProyectos(vista.getTxtBuscar().getText().trim());
            } else {
                JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "No se pudo actualizar el nombre.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarProyectoSeleccionado() {
        int fila = vista.getTblProyectos().getSelectedRow();
        if (fila < 0 || listaProyectosMemoria == null || fila >= listaProyectosMemoria.size()) {
            JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "Seleccione un proyecto para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proyecto p = listaProyectosMemoria.get(fila);
        int resp = JOptionPane.showConfirmDialog(vista.getDlgAbrirproyecto(),
                "¿Está seguro de eliminar permanentemente el proyecto '" + p.getNombreProyecto() + "'?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (resp == JOptionPane.YES_OPTION) {
            if (proyectoDAO.eliminar(p.getIdProyecto())) {
                JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "Proyecto eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                if (proyectoActual != null && proyectoActual.getIdProyecto() == p.getIdProyecto()) {
                    proyectoActual = null;
                    actualizarTituloVentana();
                }
                cargarListaProyectos(vista.getTxtBuscar().getText().trim());
            } else {
                JOptionPane.showMessageDialog(vista.getDlgAbrirproyecto(), "No se pudo eliminar el proyecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void accionGuardarProyecto() {
        if (!verificarCalculo()) return;

        if (proyectoActual == null || proyectoActual.getIdProyecto() == 0) {
            String nombre = JOptionPane.showInputDialog(vista, "Ingrese el nombre del proyecto:", "Guardar Proyecto", JOptionPane.QUESTION_MESSAGE);
            if (nombre == null || nombre.trim().isEmpty()) return;

            nombre = nombre.trim();
            int idUsuario = usuarioActual != null ? usuarioActual.getIdUsuario() : 1;
            int idMaterial = materialActual != null ? materialActual.getIdMaterial() : 1;

            List<Proyecto> listaExistentes = proyectoDAO.listarPorUsuario(idUsuario, "");
            Proyecto existente = null;
            if (listaExistentes != null) {
                for (Proyecto p : listaExistentes) {
                    if (p.getNombreProyecto() != null && p.getNombreProyecto().trim().equalsIgnoreCase(nombre)) {
                        existente = p;
                        break;
                    }
                }
            }

            if (existente != null) {
                Object[] opciones = {"Sí", "No"};
                int resp = JOptionPane.showOptionDialog(
                        vista,
                        "Ya existe un proyecto llamado \"" + nombre + "\".\n¿Desea sobrescribirlo con los datos actuales?",
                        "Confirmar sobrescritura",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        opciones,
                        opciones[1]
                );
                if (resp != 0) return;

                this.proyectoActual = existente;
                this.proyectoActual.setNombreProyecto(nombre);
                this.proyectoActual.setIdMaterial(idMaterial);

                Parametro par = new Parametro();
                par.setIdProyecto(this.proyectoActual.getIdProyecto());
                par.setFuncionPared(resultadoActual.funcion);
                par.setAlturaCm(resultadoActual.alturaIngresada);
                par.setEspesorCm(resultadoActual.espesor);
                par.setVolumenObjetivoMl(resultadoActual.volumenObjetivo);
                par.setVolumenCalculadoMl(resultadoActual.volumenCalculado);
                par.setAreaParedCm2(resultadoActual.areaPared);
                par.setVolumenMaterialCm3(resultadoActual.volumenMaterial);
                par.setCostoEstimado(resultadoActual.costoTotal);
                this.proyectoActual.setParametro(par);

                if (proyectoDAO.actualizarExistente(this.proyectoActual)) {
                    actualizarTituloVentana();
                    JOptionPane.showMessageDialog(vista, "Proyecto '" + nombre + "' sobrescrito con éxito.", "Guardado", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(vista, "No se pudo actualizar el proyecto en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                Proyecto nuevo = new Proyecto();
                nuevo.setIdUsuario(idUsuario);
                nuevo.setIdMaterial(idMaterial);
                nuevo.setNombreProyecto(nombre);

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
                    actualizarTituloVentana();
                    JOptionPane.showMessageDialog(vista, "Proyecto '" + nombre + "' guardado con éxito.", "Guardado", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(vista, "No se pudo guardar el proyecto en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            accionActualizarProyecto();
        }
    }

    private void accionActualizarProyecto() {
        ejecutarCalculoCompleto();
        if (resultadoActual != null) {
            vista.getTA_consola().append("\n\n[Actualizado] Parámetros, panel 2D y modelo 3D sincronizados.");
        }
    }

    private void abrirDialogoPlantillas() {
        vista.getDlgPlantillas().pack();
        vista.getDlgPlantillas().setLocationRelativeTo(vista);
        vista.getDlgPlantillas().setVisible(true);
    }

    private void seleccionarPlantilla(int id) {
        this.plantillaSeleccionada = id;
        switch (id) {
            case 1: // Vaso estándar (350 ml)
                vista.getTxtFuncion1().setText("0.4 * (x^2 + y^2)");
                vista.getSpnAltura().setValue(9.5);
                vista.getSpnEspesor().setValue(0.25);
                vista.getSpnVolumen().setValue(350.0);
                break;
            case 2: // Tazón / Bowl (500 ml)
                vista.getTxtFuncion1().setText("0.15 * (x^2 + y^2)");
                vista.getSpnAltura().setValue(6.0);
                vista.getSpnEspesor().setValue(0.30);
                vista.getSpnVolumen().setValue(500.0);
                break;
            case 3: // Vaso abombado (500 ml)
                vista.getTxtFuncion1().setText("0.0012 * (x^2 + y^2)^3 + 0.10 * (x^2 + y^2)");
                vista.getSpnAltura().setValue(11.35);
                vista.getSpnEspesor().setValue(0.25);
                vista.getSpnVolumen().setValue(500.0);
                break;
            case 4: // Copa (250 ml)
                vista.getTxtFuncion1().setText("4.5 * (1 - exp(-5 * (x^2 + y^2))) + 0.12 * (x^2 + y^2)");
                vista.getSpnAltura().setValue(8.85);
                vista.getSpnEspesor().setValue(0.15);
                vista.getSpnVolumen().setValue(250.0);
                break;
        }
    }

    private void aplicarPlantilla() {
        seleccionarPlantilla(plantillaSeleccionada);
        vista.getDlgPlantillas().setVisible(false);
        ejecutarCalculoCompleto();
    }

    private void abrirDialogoMateriales() {
        cargarMaterialesDesdeBD();
        vista.getDlgMateriales().pack();
        vista.getDlgMateriales().setLocationRelativeTo(vista);
        vista.getDlgMateriales().setVisible(true);
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

        DefaultTableModel modelo = (DefaultTableModel) vista.getTblMateriales().getModel();
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
        int fila = vista.getTblMateriales().getSelectedRow();
        if (fila >= 0 && listaMaterialesMemoria != null && fila < listaMaterialesMemoria.size()) {
            Material m = listaMaterialesMemoria.get(fila);

            // Permite actualizar en base de datos si el usuario editó densidad o precio
            try {
                String strDens = vista.getTxtDensidad().getText().trim();
                String strPrec = vista.getTxtPrecio().getText().trim();
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
            vista.getDlgMateriales().setVisible(false);

            if (resultadoActual != null) {
                ejecutarCalculoCompleto();
            }
        } else {
            vista.getDlgMateriales().setVisible(false);
        }
    }

    private void abrirDialogoSensibilidad() {
        if (!verificarCalculo()) return;

        vista.getLblVolumenIdeal().setText(String.format(Locale.US, "%.2f ml", resultadoActual.volumenCalculado));
        vista.getLblEstadoIdeal().setText((resultadoActual.volumenObjetivo > 0 && Math.abs(resultadoActual.volumenCalculado - resultadoActual.volumenObjetivo) <= 5.0) ? "CUMPLE META" : "NOMINAL");
        vista.getSpnCoeficiente().setValue(1.0);
        vista.getLblVolumenSimulado().setText("-");
        vista.getLblEstadoSimulado().setText("-");

        vista.getDlgSensibilidad().pack();
        vista.getDlgSensibilidad().setLocationRelativeTo(vista);
        vista.getDlgSensibilidad().setVisible(true);
    }

    private void ejecutarSimulacionSensibilidad() {
        if (!verificarCalculo()) return;

        double factorK = ((Number) vista.getSpnCoeficiente().getValue()).doubleValue();
        if (factorK <= 0) factorK = 1.0;

        double volSimulado = motorCalculo.simularDesviacionCurvatura(resultadoActual.funcion, factorK, resultadoActual.alturaIngresada);
        vista.getLblVolumenSimulado().setText(String.format(Locale.US, "%.2f ml", volSimulado));

        double desviacionPorcentual = ((volSimulado - resultadoActual.volumenCalculado) / resultadoActual.volumenCalculado) * 100.0;
        String dictamen = Math.abs(desviacionPorcentual) <= 5.0 ? "TOLERABLE (±5%)" : "CRÍTICO (>5%)";
        vista.getLblEstadoSimulado().setText(String.format(Locale.US, "%+.2f %% (%s)", desviacionPorcentual, dictamen));
    }

    private void configurarTablaVariables() {
        String[] columnas = {"Nivel (ml)", "Altura (cm)", "Radio (cm)", "Área (cm²)", "Insumo (cm³)", "Costo (S/.)"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        vista.getTblVariables().setModel(modelo);

        // Alinear todas las columnas a la derecha (son todas numéricas)
        DefaultTableCellRenderer rendererDerecha = new DefaultTableCellRenderer();
        rendererDerecha.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col = 0; col < columnas.length; col++) {
            vista.getTblVariables().getColumnModel().getColumn(col).setCellRenderer(rendererDerecha);
        }
    }

    private void actualizarTablaVariables(List<CotaGraduacion> cotas) {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTblVariables().getModel();
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
        vista.getLblEstadoBD().setText(conectada ? "Conectado" : "Offline");
        vista.getLblEstadoBD().setForeground(conectada ? new Color(46, 204, 113) : new Color(231, 76, 60));
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

    private boolean confirmarSobrescritura(File destino) {
        if (destino != null && destino.exists()) {
            Object[] opciones = {"Sí", "No"};
            int resp = JOptionPane.showOptionDialog(
                    vista,
                    "El archivo \"" + destino.getName() + "\" ya existe.\n¿Desea reemplazarlo?",
                    "Confirmar reemplazo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    opciones,
                    opciones[1]
            );
            return resp == 0;
        }
        return true;
    }

    private void actualizarTituloVentana() {
        String nombreDocumento = "Sin título";
        if (proyectoActual != null && proyectoActual.getIdProyecto() > 0
                && proyectoActual.getNombreProyecto() != null
                && !proyectoActual.getNombreProyecto().trim().isEmpty()) {
            nombreDocumento = proyectoActual.getNombreProyecto().trim();
        }

        if (usuarioActual != null) {
            String nom = usuarioActual.getNombreCompleto();
            if (nom == null || nom.trim().isEmpty()) {
                nom = (usuarioActual.getNombres() + " " + usuarioActual.getApellidos()).trim();
            }
            if (nom.isEmpty()) {
                nom = usuarioActual.getUsuario();
            }
            vista.setTitle("GFR Modeler 3D — " + nombreDocumento + " — Usuario: " + nom + " (@" + usuarioActual.getUsuario() + ")");
        } else {
            vista.setTitle("GFR Modeler 3D — " + nombreDocumento);
        }
    }

    public String obtenerNombreSugeridoExportacion(String nombrePorDefecto, String extension) {
        if (proyectoActual != null && proyectoActual.getIdProyecto() > 0
                && proyectoActual.getNombreProyecto() != null
                && !proyectoActual.getNombreProyecto().trim().isEmpty()) {
            String sanitizado = proyectoActual.getNombreProyecto().trim()
                    .replaceAll("[\\\\/:*?\"<>|]", "_")
                    .replaceAll("\\s+", "_");
            return sanitizado + extension;
        }
        return nombrePorDefecto;
    }

    public frmPrincipal getVista() {
        return vista;
    }

    public Proyecto getProyectoActual() {
        return proyectoActual;
    }

    public void setProyectoActual(Proyecto proyectoActual) {
        this.proyectoActual = proyectoActual;
        actualizarTituloVentana();
    }

    public void reiniciarProyectoNuevo() {
        this.proyectoActual = null;
        actualizarTituloVentana();
    }

    private void mostrarAcercaDe() {
        String info = "GFR Modeler 3D v1.5.6\n\n" +
                      "Autor: Walter D. Maza Cuadrado\n" +
                      "Asignatura: Cálculo Integral y Ecuaciones Diferenciales\n" +
                      "Institución: Universidad César Vallejo (UCV) - Campus Lima Norte\n\n" +
                      "Librerías:\n" +
                      "  • FlatLaf v3.7.2\n" +
                      "  • MySQL Connector/J v26.10.0\n" +
                      "  • Apache Commons Math v3.6.1\n" +
                      "  • exp4j v0.4.8\n" +
                      "  • JFreeChart v1.5.6\n" +
                      "  • JavaFX 3D / Swing v28\n" +
                      "  • OpenPDF v2.4.0\n" +
                      "  • cvsclient v20060125\n" +
                      "  • JUnit Jupiter v6.1.3 (Framework para pruebas unitarias)\n" +
                      "IDE: Apache NetBeans 29\n\n" +
                      "Sistema gestor de base de datos: MySQL / MariaDB\n" +
                      "JDK 25 LTS\n" +
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

    // =========================================================================
    // SISTEMA DE AYUDAS VISUALES (TOOLTIPS) INFORMATIVAS Y TÉCNICAS
    // =========================================================================

    private void setToolTip(JComponent comp, String text) {
        if (comp != null) {
            comp.setToolTipText(text);
        }
    }

    private void configurarTooltips() {
        // Calibración de retardos para lectura cómoda en demostraciones
        ToolTipManager.sharedInstance().setInitialDelay(350);
        ToolTipManager.sharedInstance().setDismissDelay(12000);

        // ---------------------------------------------------------------------
        // 1. PRIORIDAD ALTA: Botones de Reportes de Consola (A, B, C) en HTML
        // ---------------------------------------------------------------------
        setToolTip(vista.getBtnReporteA(),
            "<html><div style='width:320px;'><b>Reporte de Cálculo Volumétrico y Cubicación</b><br>" +
            "Muestra la capacidad V(h) por integral doble, altura despejada, radio de borde, " +
            "área superficial de pared, consumo de material (cm³ y gramos) y costo unitario de fabricación.</div></html>");

        setToolTip(vista.getBtnReporteB(),
            "<html><div style='width:320px;'><b>Reporte de Análisis de Gradiente y Desmolde</b><br>" +
            "Evalúa el vector gradiente ∇f = (∂f/∂x, ∂f/∂y) en el borde, la magnitud de pendiente " +
            "y el ángulo de inclinación de pared para verificar la extracción sin fricción del molde (umbral ≥ 3.0°).</div></html>");

        setToolTip(vista.getBtnReporteC(),
            "<html><div style='width:320px;'><b>Reporte de Optimización Lagrange y Comparativa CAD</b><br>" +
            "Determina las dimensiones óptimas (r*, h*) de mínimo consumo de material mediante " +
            "multiplicadores de Lagrange (∇V = λ∇A) y contrasta el volumen integral exacto frente a cilindro y tronco de cono.</div></html>");

        // ---------------------------------------------------------------------
        // 2. Parámetros de Entrada, Tablas y Barra de Estado (frmPrincipal)
        // ---------------------------------------------------------------------
        setToolTip(vista.getTxtFuncion1(),
            "Ecuación explícita z = f(x, y) de la pared interior del envase en cm (ej. 0.5 * (x^2 + y^2)). " +
            "Debe formar una superficie cerrada creciente desde el origen.");

        setToolTip(vista.getSpnAltura(),
            "Altura total del envase en cm. Tip: ingrese 0 para que el motor despeje automáticamente la altura exacta según el volumen objetivo.");

        setToolTip(vista.getSpnEspesor(),
            "Espesor de la pared del envase en cm (ej. 0.20 cm = 2 mm). Multiplica el área superficial para estimar el consumo de insumo.");

        setToolTip(vista.getSpnVolumen(),
            "Capacidad volumétrica objetivo en mililitros (ml) para las marcas de graduación y el despeje inverso de altura.");

        setToolTip(vista.getTblVariables(),
            "Tabla de cotas de graduación cada 50 ml: muestra altura de llenado, radio interno, área acumulada, volumen de material y costo progresivo.");

        setToolTip(vista.getTA_consola(),
            "Consola de resultados técnicos: muestra reportes volumétricos, análisis de gradiente, optimización Lagrange y diagnósticos.");

        setToolTip(vista.getLblintegracion(),
            "Margen de error numérico estimado de la integración doble en coordenadas polares (< 0.1% respecto al valor analítico).");

        setToolTip(vista.getLblEstadoBD(),
            "Estado de conexión en tiempo real con el servidor para persistencia de proyectos y materiales.");

        setToolTip(vista.getLblCursorPosicion(),
            "Coordenadas actuales del cursor (X, Y, Z) sobre la gráfica bidimensional.");

        setToolTip(vista.getControlador2D(),
            "Área de visualización 2D: curvas de nivel, perfil transversal z(r) o curva de llenado V(h).");

        setToolTip(vista.getControlador3D(),
            "Visor 3D interactivo: arrastre con el mouse para rotar el envase y use la rueda para zoom.");

        // ---------------------------------------------------------------------
        // 3. Cinta Superior de Herramientas
        // ---------------------------------------------------------------------
        // Pestaña Archivo
        setToolTip(vista.getBtnNuevo(),
            "Reinicia el área de trabajo para comenzar un nuevo diseño desde cero (solicita confirmación).");

        setToolTip(vista.getBtnAbrir(),
            "Abre el gestor de proyectos guardados en la base de datos para cargar, renombrar o eliminar diseños.");

        setToolTip(vista.getBtnGuardar(),
            "Guarda el diseño actual, sus parámetros y material asignado.");

        setToolTip(vista.getBtnActualizar(),
            "Guarda los cambios recientes sobre el proyecto activo en la base de datos.");

        setToolTip(vista.getBtnExportarPNG(),
            "Exporta la gráfica 2D activa (curvas de nivel, perfil o curva V(h)) como imagen PNG.");

        setToolTip(vista.getBtnExportarCSV(),
            "Exporta la tabla numérica de cotas y volúmenes por nivel a un archivo CSV.");

        setToolTip(vista.getBtnExportarSTIL(),
            "Genera y exporta la malla tridimensional sólida con espesor en formato STL para impresión 3D.");

        setToolTip(vista.getBtnExportarDXF(),
            "Exporta las curvas de nivel y el perfil transversal en capas CAD formato DXF.");

        setToolTip(vista.getBtnExportarPDF(),
            "Genera un informe técnico completo en PDF con parámetros, tabla de cotas y gráficas 2D/3D.");

        setToolTip(vista.getBtnExportarPNG1(),
            "Muestra información de versión, autoría, institución y librerías del sistema (Acerca de).");

        setToolTip(vista.getBtnCerrarSesion(),
            "Cierra la sesión del proyectista actual y regresa a la pantalla de inicio de sesión.");

        // Pestaña Modelado
        setToolTip(vista.getBtnSuperficie(),
            "Alterna la visualización 3D entre sólido sombreado y malla de alambre (Wireframe).");

        setToolTip(vista.getBtnCurvasnivel(),
            "Proyecta en 2D las curvas de nivel f(x, y) = k correspondientes a cada marca de graduación.");

        setToolTip(vista.getBtnGradienteDerivadas(),
            "Grafica la curva de graduación volumétrica V(h) en función de la altura de llenado.");

        setToolTip(vista.getBtnLagrange(),
            "Grafica el perfil transversal z(r) del envase junto con las cotas de nivel.");

        setToolTip(vista.getBtnPlantillas(),
            "Abre el catálogo de geometrías predefinidas (Vaso, Tazón, Vaso abombado y Copa).");

        // Pestaña Cálculo
        setToolTip(vista.getBtnCalcularVaso(),
            "Ejecuta el motor de cálculo multivariable, actualiza el modelo 3D, las gráficas 2D y la tabla de cotas.");

        setToolTip(vista.getBtnCalculartodo(),
            "Ejecuta el motor de cálculo multivariable, actualiza el modelo 3D, las gráficas 2D y la tabla de cotas.");

        setToolTip(vista.getBtnVolumen(),
            "Calcula la capacidad volumétrica exacta mediante la integral doble ∬ [h - f(r,θ)] r dr dθ.");

        setToolTip(vista.getBtnAlturaObjetivo(),
            "Muestra la altura despejada y el radio de borde necesarios para alcanzar el volumen objetivo.");

        setToolTip(vista.getBtnTablaGradiente(),
            "Actualiza la tabla inferior con las alturas exactas de graduación cada 50 ml.");

        setToolTip(vista.getBtnAreaPared(),
            "Calcula el área de la superficie lateral mediante ∬ √(1 + ||∇f||²) dA.");

        setToolTip(vista.getBtnResina(),
            "Estima el volumen de material requerido (Área × Espesor) y su peso total en gramos.");

        setToolTip(vista.getBtnCosto(),
            "Calcula el costo unitario de fabricación según el consumo de material y su tarifa por cm³.");

        // Pestaña Análisis
        setToolTip(vista.getBtnAnalizartodo(),
            "Ejecuta y muestra en consola el reporte integral unificado (Cubicación, Gradiente y Lagrange).");

        setToolTip(vista.getBtnAproximaciones(),
            "Contrasta el volumen integral exacto frente a aproximaciones geométricas de cilindro y tronco de cono.");

        setToolTip(vista.getBtnDesmoldar(),
            "Evalúa el gradiente ∇f y verifica si el ángulo de salida de pared permite desmoldar la pieza (≥ 3.0°).");

        setToolTip(vista.getBtnSensibilidad(),
            "Abre el simulador de desviación de curvatura para evaluar tolerancias de fabricación.");

        setToolTip(vista.getBtnMateriales(),
            "Abre el catálogo de materiales (Vidrio, Resina PET, PP) para seleccionar o editar densidad y precio.");

        // ---------------------------------------------------------------------
        // 4. Diálogos Modales (JDialog) y Controles Internos
        // ---------------------------------------------------------------------
        // Diálogo: Abrir Proyecto
        setToolTip(vista.getTxtBuscar(),
            "Escribe parte del nombre del proyecto y presiona Enter o Buscar para filtrar.");

        setToolTip(vista.getBtnBuscar(),
            "Filtra la lista de proyectos registrados por el usuario.");

        setToolTip(vista.getTblProyectos(),
            "Lista de proyectos guardados. Haz clic en una fila para seleccionarlo.");

        setToolTip(vista.getBtnAbrirProyecto(),
            "Carga los parámetros geométricos y el material del proyecto seleccionado en el editor.");

        setToolTip(vista.getBtnCambiarNombre(),
            "Permite editar el nombre del proyecto seleccionado en la base de datos.");

        setToolTip(vista.getBtnEliminarProyecto(),
            "Elimina de forma permanente el proyecto seleccionado de la base de datos.");

        if (vista.getDlgAbrirproyecto() != null) {
            setToolTip(vista.getDlgAbrirproyecto().getRootPane(),
                "Gestor de Proyectos: administración y apertura de diseños almacenados en la base de datos.");
        }

        // Diálogo: Plantillas
        setToolTip(vista.getBtnVaso(),
            "Previsualiza parámetros de Vaso estándar (350 ml, pared parabólica z = 0.4(x² + y²)).");

        setToolTip(vista.getBtnTazon(),
            "Previsualiza parámetros de Tazón / Bowl amplio (500 ml, z = 0.15(x² + y²)).");

        setToolTip(vista.getBtnVasoabombado(),
            "Previsualiza parámetros de Vaso abombado (500 ml, perfil polinómico de grado 6).");

        setToolTip(vista.getBtnCopa(),
            "Previsualiza parámetros de Copa con tallo (250 ml, perfil exponencial-parabólico).");

        setToolTip(vista.getBtnSeleccionarPlantilla(),
            "Aplica la plantilla elegida al formulario principal y ejecuta el cálculo 2D/3D.");

        if (vista.getDlgPlantillas() != null) {
            setToolTip(vista.getDlgPlantillas().getRootPane(),
                "Catálogo de Plantillas: geometrías típicas de recipientes para inicio rápido de modelado.");
        }

        // Diálogo: Materiales
        setToolTip(vista.getTblMateriales(),
            "Catálogo de materiales disponibles. Selecciona una fila para ver o modificar sus propiedades.");

        setToolTip(vista.getTxtDensidad(),
            "Densidad física del material en g/cm³. Puedes editar este valor antes de confirmar.");

        setToolTip(vista.getTxtPrecio(),
            "Costo unitario del insumo en soles por cm³ (S/ / cm³). Puedes editar este valor antes de confirmar.");

        setToolTip(vista.getBtnSeleccionarMaterial(),
            "Asigna el material elegido al envase actual, actualiza densidad/precio y recalcula costos.");

        if (vista.getDlgMateriales() != null) {
            setToolTip(vista.getDlgMateriales().getRootPane(),
                "Gestor de Materiales: selección y calibración de propiedades físicas (densidad y costo unitario).");
        }

        // Diálogo: Análisis de Sensibilidad
        setToolTip(vista.getSpnCoeficiente(),
            "Factor multiplicador k aplicado a la curvatura f(x, y). Ejemplo: k = 1.10 simula un incremento del +10% en la curvatura del molde.");

        setToolTip(vista.getBtnSimular(),
            "Calcula el volumen resultante bajo el factor k y evalúa si la desviación está dentro de la tolerancia (±5%).");

        setToolTip(vista.getLblVolumenIdeal(),
            "Volumen teórico de diseño V(h) bajo curvatura nominal (k = 1.0).");

        setToolTip(vista.getLblEstadoIdeal(),
            "Dictamen de conformidad volumétrica nominal respecto al volumen objetivo.");

        setToolTip(vista.getLblVolumenSimulado(),
            "Volumen perturbado V*(h) resultante de aplicar la desviación de curvatura k·f(x, y).");

        setToolTip(vista.getLblEstadoSimulado(),
            "Porcentaje de desviación volumétrica (V* - V)/V·100% y estado de tolerancia industrial.");

        if (vista.getDlgSensibilidad() != null) {
            setToolTip(vista.getDlgSensibilidad().getRootPane(),
                "Análisis de Sensibilidad: simulación de tolerancias y desviaciones dimensionales de curvatura en molde.");
        }
    }
}