import Controladores.CtrlPrincipal;
import DAO.ProyectoDAO;
import Modelo.Parametro;
import Modelo.Proyecto;
import Modelo.Usuario;
import Utilidades.ConexionBD;
import Vistas.frmPrincipal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Pruebas_TituloYGuardado {

    private frmPrincipal vista;
    private Usuario usuario;
    private CtrlPrincipal controlador;
    private ProyectoDAO proyectoDAO;

    @BeforeEach
    void setUp() {
        vista = new frmPrincipal();
        usuario = new Usuario("Walter Maza", "walter", "walter@example.com", "pass123");
        usuario.setIdUsuario(1);
        controlador = new CtrlPrincipal(vista, usuario);
        proyectoDAO = new ProyectoDAO();
    }

    @Test
    @DisplayName("1. Estado inicial y reinicio de proyecto nuevo resuelven el título con 'Sin título'")
    void testEstadoInicialYReinicioSinTitulo() {
        // Verificación de estado inicial
        String tituloInicial = vista.getTitle();
        assertNotNull(tituloInicial, "El título de la ventana no debe ser nulo");
        assertTrue(tituloInicial.contains("Sin título"),
                "El título inicial debe contener 'Sin título'. Título obtenido: " + tituloInicial);
        assertTrue(tituloInicial.contains("Usuario: Walter Maza (@walter)"),
                "El título debe contener los datos del usuario logueado. Título obtenido: " + tituloInicial);

        // Asignar un proyecto temporal para cambiar de estado
        Proyecto temp = new Proyecto();
        temp.setIdProyecto(10);
        temp.setNombreProyecto("Diseño Preliminar");
        controlador.setProyectoActual(temp);
        assertTrue(vista.getTitle().contains("Diseño Preliminar"), "El título debe reflejar el proyecto activo");
        assertFalse(vista.getTitle().contains("Sin título"), "El título ya no debe decir 'Sin título'");

        // Reiniciar proyecto nuevo (equivalente al reinicio tras confirmar 'Sí' en nuevo proyecto)
        controlador.reiniciarProyectoNuevo();
        String tituloReiniciado = vista.getTitle();
        assertNull(controlador.getProyectoActual(), "El proyecto actual debe ser nulo tras reiniciar");
        assertTrue(tituloReiniciado.contains("Sin título"),
                "Tras reiniciar el proyecto el título debe volver a 'Sin título'. Título obtenido: " + tituloReiniciado);
    }

    @Test
    @DisplayName("2. Asignación de proyecto guardado con id > 0 y nombre cambia dinámicamente el título")
    void testAsignacionProyectoCambiaTituloDinamico() {
        Proyecto proyecto = new Proyecto();
        proyecto.setIdProyecto(15);
        proyecto.setNombreProyecto("Vaso ExpoWork 350ml");

        controlador.setProyectoActual(proyecto);

        String titulo = vista.getTitle();
        assertNotNull(titulo, "El título no debe ser nulo");
        assertTrue(titulo.contains("Vaso ExpoWork 350ml"),
                "El título debe contener el nombre del proyecto activo 'Vaso ExpoWork 350ml'. Título obtenido: " + titulo);
        assertFalse(titulo.contains("Sin título"),
                "El título no debe contener 'Sin título' cuando hay un proyecto activo guardado");
        assertTrue(titulo.startsWith("GFR Modeler 3D — Vaso ExpoWork 350ml"),
                "El formato del título debe respetar la estructura estándar del editor");

        // Verificación de sugerencia de archivo para exportaciones
        String sugeridoPng = controlador.obtenerNombreSugeridoExportacion("grafica_envase_2d.png", ".png");
        assertEquals("Vaso_ExpoWork_350ml.png", sugeridoPng,
                "El nombre sugerido para PNG debe ser el nombre del proyecto sanitizado");

        String sugeridoPdf = controlador.obtenerNombreSugeridoExportacion("Reporte_Tecnico_Envase.pdf", ".pdf");
        assertEquals("Vaso_ExpoWork_350ml.pdf", sugeridoPdf,
                "El nombre sugerido para PDF debe ser el nombre del proyecto sanitizado");

        // Al volver a null o proyecto sin persistir, el sugerido debe volver al default
        controlador.setProyectoActual(null);
        assertEquals("grafica_envase_2d.png", controlador.obtenerNombreSugeridoExportacion("grafica_envase_2d.png", ".png"));
    }

    @Test
    @DisplayName("3. Persistencia y sobrescritura en MySQL: mantiene idProyecto sin duplicar filas")
    void testSobrescrituraProyectoEnMySQL() {
        Assumptions.assumeTrue(ConexionBD.verificarConexion(), "MySQL no disponible en entorno local");

        String nombreUnico = "Test_Sobrescritura_" + System.currentTimeMillis();
        int idGenerado = -1;

        try {
            // 1. Guardar un nuevo proyecto
            Proyecto p = new Proyecto();
            p.setIdUsuario(1);
            p.setIdMaterial(1);
            p.setNombreProyecto(nombreUnico);

            Parametro par = new Parametro();
            par.setFuncionPared("0.4 * (x^2 + y^2)");
            par.setAlturaCm(9.5);
            par.setEspesorCm(0.25);
            par.setVolumenObjetivoMl(350.0);
            par.setVolumenCalculadoMl(352.1);
            par.setAreaParedCm2(195.4);
            par.setVolumenMaterialCm3(48.8);
            par.setCostoEstimado(8.78);
            p.setParametro(par);

            idGenerado = proyectoDAO.guardarNuevo(p);
            assertTrue(idGenerado > 0, "El idProyecto debe ser generado por la BD");
            p.setIdProyecto(idGenerado);

            // Verificar que existe exactamente 1 proyecto con ese nombre
            List<Proyecto> listaInicial = proyectoDAO.listarPorUsuario(1, nombreUnico);
            assertNotNull(listaInicial);
            assertEquals(1, listaInicial.size(), "Debe existir exactamente 1 proyecto registrado inicialmente");
            assertEquals(idGenerado, listaInicial.get(0).getIdProyecto());

            // 2. Sobrescribir con nuevos valores sobre el mismo idProyecto y mismo nombre
            p.setNombreProyecto(nombreUnico);
            p.setIdMaterial(1);
            Parametro parMod = p.getParametro();
            parMod.setIdProyecto(idGenerado);
            parMod.setAlturaCm(10.2); // Altura actualizada
            parMod.setVolumenCalculadoMl(380.0); // Volumen actualizado
            parMod.setCostoEstimado(9.65); // Costo actualizado

            boolean exitoActualizar = proyectoDAO.actualizarExistente(p);
            assertTrue(exitoActualizar, "actualizarExistente debe retornar true");

            // 3. Comprobar que NO se crearon filas duplicadas y el idProyecto es exactamente el mismo
            List<Proyecto> listaSobrescrita = proyectoDAO.listarPorUsuario(1, nombreUnico);
            assertNotNull(listaSobrescrita);
            assertEquals(1, listaSobrescrita.size(), "No se deben haber creado registros duplicados tras sobrescribir");
            assertEquals(idGenerado, listaSobrescrita.get(0).getIdProyecto(), "El idProyecto debe ser exactamente el mismo");

            // Comprobar que los parámetros actualizados persisten
            Proyecto recuperado = listaSobrescrita.get(0);
            assertNotNull(recuperado.getParametro());
            assertEquals(10.2, recuperado.getParametro().getAlturaCm(), 0.001);
            assertEquals(380.0, recuperado.getParametro().getVolumenCalculadoMl(), 0.001);

            // 4. Comprobar actualización del título de la ventana con el controlador
            controlador.setProyectoActual(p);
            assertTrue(vista.getTitle().contains(nombreUnico), "El título de la ventana debe mostrar el proyecto sobrescrito");

        } finally {
            // Limpieza obligatoria del registro de prueba en la base de datos
            if (idGenerado > 0) {
                proyectoDAO.eliminar(idGenerado);
                List<Proyecto> postEliminacion = proyectoDAO.listarPorUsuario(1, nombreUnico);
                assertTrue(postEliminacion == null || postEliminacion.isEmpty(),
                        "El proyecto de prueba debe quedar eliminado completamente de la base de datos");
            }
        }
    }

    @Test
    @DisplayName("4. Título sin usuario logueado (modo sin sesión o independiente)")
    void testTituloSinUsuarioLogueado() {
        CtrlPrincipal ctrlSinUser = new CtrlPrincipal(vista, null);
        assertEquals("GFR Modeler 3D — Sin título", vista.getTitle(),
                "Cuando usuarioActual es null, el título no debe incluir la sección de usuario");

        Proyecto p = new Proyecto();
        p.setIdProyecto(5);
        p.setNombreProyecto("Copa Cristal");
        ctrlSinUser.setProyectoActual(p);
        assertEquals("GFR Modeler 3D — Copa Cristal", vista.getTitle(),
                "Con proyecto activo y sin usuario, debe mostrar 'GFR Modeler 3D — Copa Cristal'");
    }

    @Test
    @DisplayName("5. Título con proyecto no persistido (id <= 0) o nombre en blanco")
    void testTituloProyectoInvalidoONoPersistido() {
        Proyecto pNoPersistido = new Proyecto();
        pNoPersistido.setIdProyecto(0);
        pNoPersistido.setNombreProyecto("Borrador En Memoria");
        controlador.setProyectoActual(pNoPersistido);
        assertTrue(vista.getTitle().contains("Sin título"),
                "Un proyecto con idProyecto <= 0 debe ser considerado 'Sin título'");

        Proyecto pNombreVacio = new Proyecto();
        pNombreVacio.setIdProyecto(8);
        pNombreVacio.setNombreProyecto("   ");
        controlador.setProyectoActual(pNombreVacio);
        assertTrue(vista.getTitle().contains("Sin título"),
                "Un proyecto con nombre en blanco debe ser considerado 'Sin título'");
    }
}
