package Controladores;
import Modelo.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CtrlGenerador2D{

    private final JPanel panelContenedor;
    private final JLabel lblCursor;
    private JFreeChart chartActual;

    public CtrlGenerador2D(JPanel panelContenedor, JLabel lblCursor) {
        this.panelContenedor = panelContenedor;
        this.lblCursor = lblCursor;
    }

    // Vista 1: Curvas de Nivel f(x,y) = k
    public void mostrarCurvasDeNivel(CtrlMotorCalculo motor, ResultadoCalculo res) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (res.cotas != null && !res.cotas.isEmpty()) {
            for (CotaGraduacion c : res.cotas) {
                XYSeries serie = new XYSeries(String.format("%.0f ml (h=%.2f cm)", c.getVolumenMl(), c.getAlturaCm()));
                for (int i = 0; i <= 72; i++) {
                    double theta = 2.0 * Math.PI * i / 72.0;
                    double r = motor.hallarRadioEnNivel(c.getAlturaCm(), theta);
                    serie.add(r * Math.cos(theta), r * Math.sin(theta));
                }
                dataset.addSeries(serie);
            }
        }

        // Borde superior
        XYSeries borde = new XYSeries(String.format("Borde h=%.2f cm", res.alturaIngresada));
        for (int i = 0; i <= 72; i++) {
            double theta = 2.0 * Math.PI * i / 72.0;
            double r = motor.hallarRadioEnNivel(res.alturaIngresada, theta);
            borde.add(r * Math.cos(theta), r * Math.sin(theta));
        }
        dataset.addSeries(borde);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Curvas de Nivel f(x, y) = k", "Eje X (cm)", "Eje Y (cm)",
            dataset, PlotOrientation.VERTICAL, true, true, false
        );
        estilizarGraficoOscuro(chart);
        renderizarEnPanel(chart, motor, true);
    }

    // Vista 2: Perfil de Corte Transversal z(r) con espesor
    public void mostrarPerfilEnvase(CtrlMotorCalculo motor, ResultadoCalculo res) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries paredInt = new XYSeries("Pared Interior z = f(r,0)");
        XYSeries paredExt = new XYSeries("Pared Exterior (+Espesor)");

        double zMin = motor.evaluarZ(0, 0);
        for (int i = 0; i <= 40; i++) {
            double z = zMin + (res.alturaIngresada - zMin) * i / 40.0;
            double r = motor.hallarRadioEnNivel(z, 0.0);
            paredInt.add(r, z);
            paredInt.add(-r, z);
            paredExt.add(r + res.espesor, z - res.espesor * 0.4);
            paredExt.add(-(r + res.espesor), z - res.espesor * 0.4);
        }
        dataset.addSeries(paredInt);
        dataset.addSeries(paredExt);

        if (res.cotas != null) {
            for (CotaGraduacion c : res.cotas) {
                XYSeries nivel = new XYSeries(String.format("Marca %.0f ml", c.getVolumenMl()));
                nivel.add(-c.getRadioCm(), c.getAlturaCm());
                nivel.add(c.getRadioCm(), c.getAlturaCm());
                dataset.addSeries(nivel);
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Perfil Transversal y Marcas de Nivel", "Radio r (cm)", "Altura z (cm)",
            dataset, PlotOrientation.VERTICAL, true, true, false
        );
        estilizarGraficoOscuro(chart);
        renderizarEnPanel(chart, motor, false);
    }

    // Vista 3: Curva de Graduación V(h)
    public void mostrarCurvaGraduacion(ResultadoCalculo res) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries serieVH = new XYSeries("Capacidad V(h)");
        serieVH.add(0, 0);
        if (res.cotas != null) {
            for (CotaGraduacion c : res.cotas) {
                serieVH.add(c.getAlturaCm(), c.getVolumenMl());
            }
        }
        serieVH.add(res.alturaIngresada, res.volumenCalculado);
        dataset.addSeries(serieVH);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Curva de Graduación Volumétrica V(h)", "Altura h (cm)", "Volumen (ml)",
            dataset, PlotOrientation.VERTICAL, true, true, false
        );
        estilizarGraficoOscuro(chart);
        renderizarEnPanel(chart, null, false);
    }

    private void estilizarGraficoOscuro(JFreeChart chart) {
        Color fondo = new Color(43, 43, 43);
        Color rejilla = new Color(85, 85, 85);

        chart.setBackgroundPaint(fondo);
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(fondo);
            chart.getLegend().setItemPaint(Color.LIGHT_GRAY);
        }

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(34, 34, 34));
        plot.setDomainGridlinePaint(rejilla);
        plot.setRangeGridlinePaint(rejilla);
        plot.setOutlinePaint(rejilla);

        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        domain.setTickLabelPaint(Color.LIGHT_GRAY);
        domain.setLabelPaint(Color.WHITE);
        range.setTickLabelPaint(Color.LIGHT_GRAY);
        range.setLabelPaint(Color.WHITE);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.2f));
        }
        plot.setRenderer(renderer);
    }

    private void renderizarEnPanel(JFreeChart chart, CtrlMotorCalculo motor, boolean esVistaXY) {
        this.chartActual = chart;
        ChartPanel cp = new ChartPanel(chart);
        cp.setMouseWheelEnabled(true);

        cp.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent e) {}

            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
                if (lblCursor == null) return;
                Point2D p = cp.translateScreenToJava2D(e.getTrigger().getPoint());
                Rectangle2D plotArea = cp.getScreenDataArea();
                XYPlot plot = chart.getXYPlot();
                double x = plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
                double y = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());
                double z = (esVistaXY && motor != null) ? motor.evaluarZ(x, y) : y;
                lblCursor.setText(String.format("Cursor:  X: %.2f ; Y: %.2f ; Z: %.2f", x, y, z));
            }
        });

        panelContenedor.removeAll();
        panelContenedor.setLayout(new BorderLayout());
        panelContenedor.add(cp, BorderLayout.CENTER);
        panelContenedor.revalidate();
        panelContenedor.repaint();
    }

    public BufferedImage capturarImagen2D(int ancho, int alto) {
        if (chartActual == null) return null;
        return chartActual.createBufferedImage(ancho, alto);
    }
}