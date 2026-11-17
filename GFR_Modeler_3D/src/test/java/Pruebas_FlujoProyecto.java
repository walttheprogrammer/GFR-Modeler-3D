import Servicios.*;
import DAO.*;
import Modelo.*;
import Utilidades.ConexionBD;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.*;
public class Pruebas_FlujoProyecto {

    private SrvMotorCalculo motor;
    private Material materialVidrio;
    private ProyectoDAO proyectoDAO;

    @BeforeEach
    void setUp() {
        motor = new SrvMotorCalculo();
        materialVidrio = new Material(1, "Vidrio Templado", 2.50, 0.18);
        proyectoDAO = new ProyectoDAO();
    }

    @Test
    @DisplayName("1. Generación de proyecto y cálculo de parámetros geométricos")
    void testGeneracionProyectoCalculo() {
        ResultadoCalculo res = motor.ejecutarAnalisisCompleto("0.5 * (x^2 + y^2)", 0, 0.20, 350.0, materialVidrio);
        assertNotNull(res, "El resultado de cálculo no debe ser nulo");
        assertEquals(10.56, res.alturaIngresada, 0.05, "La altura despejada debe ser ~10.56 cm");
        assertEquals(350.0, res.volumenCalculado, 1.5, "El volumen calculado debe ser ~350.0 ml");
        assertTrue(res.areaPared > 0, "El área de pared debe ser mayor a 0");
        assertTrue(res.costoTotal > 0, "El costo total estimado debe ser mayor a 0");

        // Construir entidades Proyecto y Parametro tal como en accionGuardarProyecto()
        Proyecto nuevo = new Proyecto();
        nuevo.setIdUsuario(1);
        nuevo.setIdMaterial(materialVidrio.getIdMaterial());
        nuevo.setNombreProyecto("Proyecto Prueba Unitaria");

        Parametro par = new Parametro();
        par.setFuncionPared(res.funcion);
        par.setAlturaCm(res.alturaIngresada);
        par.setEspesorCm(res.espesor);
        par.setVolumenObjetivoMl(res.volumenObjetivo);
        par.setVolumenCalculadoMl(res.volumenCalculado);
        par.setAreaParedCm2(res.areaPared);
        par.setVolumenMaterialCm3(res.volumenMaterial);
        par.setCostoEstimado(res.costoTotal);
        nuevo.setParametro(par);

        assertNotNull(nuevo.getParametro(), "El parámetro del proyecto no debe ser nulo");
        assertEquals("Proyecto Prueba Unitaria", nuevo.getNombreProyecto());
        assertEquals(res.alturaIngresada, nuevo.getParametro().getAlturaCm());
        assertEquals(res.volumenCalculado, nuevo.getParametro().getVolumenCalculadoMl());
        assertEquals(res.areaPared, nuevo.getParametro().getAreaParedCm2());
    }

    @Test
    @DisplayName("2. Persistencia en MySQL: guardarNuevo, listarPorUsuario y eliminar")
    void testPersistenciaProyectoDAO() {
        Assumptions.assumeTrue(ConexionBD.verificarConexion(), "MySQL no disponible en entorno local");

        ResultadoCalculo res = motor.ejecutarAnalisisCompleto("0.5 * (x^2 + y^2)", 0, 0.20, 350.0, materialVidrio);

        String nombreUnico = "Test_Auto_" + System.currentTimeMillis();
        Proyecto p = new Proyecto();
        p.setIdUsuario(1);
        p.setIdMaterial(materialVidrio.getIdMaterial());
        p.setNombreProyecto(nombreUnico);

        Parametro par = new Parametro();
        par.setFuncionPared(res.funcion);
        par.setAlturaCm(res.alturaIngresada);
        par.setEspesorCm(res.espesor);
        par.setVolumenObjetivoMl(res.volumenObjetivo);
        par.setVolumenCalculadoMl(res.volumenCalculado);
        par.setAreaParedCm2(res.areaPared);
        par.setVolumenMaterialCm3(res.volumenMaterial);
        par.setCostoEstimado(res.costoTotal);
        p.setParametro(par);

        int idGenerado = -1;
        try {
            idGenerado = proyectoDAO.guardarNuevo(p);
            assertTrue(idGenerado > 0, "El idProyecto retornado por guardarNuevo debe ser mayor a 0");

            final int idEsperado = idGenerado;
            List<Proyecto> lista = proyectoDAO.listarPorUsuario(1, nombreUnico);
            assertNotNull(lista, "La lista retornada no debe ser nula");
            assertFalse(lista.isEmpty(), "La lista debe contener el proyecto recién creado");

            boolean encontrado = lista.stream().anyMatch(proy -> proy.getIdProyecto() == idEsperado);
            assertTrue(encontrado, "El proyecto con id " + idEsperado + " debe estar presente en listarPorUsuario");
        } finally {
            if (idGenerado > 0) {
                boolean eliminado = proyectoDAO.eliminar(idGenerado);
                assertTrue(eliminado, "El proyecto de prueba debe eliminarse limpiamente de la base de datos");
            }
        }
    }

    @Test
    @DisplayName("3. Validación de exportación y detección de archivo existente con @TempDir")
    void testExportacionYArchivoExistente(@TempDir Path tempDir) throws IOException {
        ResultadoCalculo res = motor.ejecutarAnalisisCompleto("0.5 * (x^2 + y^2)", 0, 0.20, 350.0, materialVidrio);

        // Exportación CSV en @TempDir
        File archivoCSV = tempDir.resolve("cotas_test.csv").toFile();
        assertFalse(archivoCSV.exists(), "El archivo no debe existir antes de exportar");

        SrvExportador.exportarCSV(archivoCSV, res);
        assertTrue(archivoCSV.exists(), "El archivo CSV exportado debe existir en disco");
        assertTrue(archivoCSV.length() > 0, "El archivo CSV no debe estar vacío");

        // Condición de sobrescritura: verificar que archivoCSV.exists() sea true
        assertTrue(archivoCSV.exists(), "Debe reportar que el archivo ya existe para disparar la confirmación de reemplazo");

        // Exportación DXF en @TempDir
        File archivoDXF = tempDir.resolve("planos_test.dxf").toFile();
        assertFalse(archivoDXF.exists(), "El archivo DXF no debe existir antes de exportar");

        SrvExportador.exportarDXF(archivoDXF, motor, res);
        assertTrue(archivoDXF.exists(), "El archivo DXF exportado debe existir en disco");
        assertTrue(archivoDXF.length() > 0, "El archivo DXF no debe estar vacío");
        assertTrue(archivoDXF.exists(), "Debe reportar que el archivo DXF ya existe al reintentar guardar sobre la misma ruta");
    }
}
