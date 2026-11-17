package Servicios;

import Modelo.CotaGraduacion;
import Modelo.ResultadoCalculo;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SrvGenerador2D {

    private final JPanel panelContenedor;
    private final JLabel lblCursor;
    private JFreeChart chartActual;

    public SrvGenerador2D(JPanel panelContenedor, JLabel lblCursor) {
        this.panelContenedor = panelContenedor;
        this.lblCursor = lblCursor;
    }

    // Vista 1: Curvas de Nivel f(x,y) = k (Proporción 1:1 circular)
    public void mostrarCurvasDeNivel(SrvMotorCalculo motor, ResultadoCalculo res) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (res.cotas != null && !res.cotas.isEmpty()) {
            for (CotaGraduacion c : res.cotas) {
                // autoSort = false para evitar líneas en zig-zag al cerrar círculos
                XYSeries serie = new XYSeries(String.format("%.0f ml (h=%.2f cm)", c.getVolumenMl(), c.getAlturaCm()), false);
                for (int i = 0; i <= 72; i++) {
                    double theta = 2.0 * Math.PI * i / 72.0;
                    double r = motor.hallarRadioEnNivel(c.getAlturaCm(), theta);
                    serie.add(r * Math.cos(theta), r * Math.sin(theta));
                }
                dataset.addSeries(serie);
            }
        }

        // Borde superior
        XYSeries borde = new XYSeries(String.format("Borde h=%.2f cm", res.alturaIngresada), false);
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

        // Proporción 1:1 en ejes X e Y para evitar óvalos achatados
        double maxR = motor.hallarRadioEnNivel(res.alturaIngresada, 0.0);
        if (res.cotas != null) {
            for (CotaGraduacion c : res.cotas) {
                if (c.getRadioCm() > maxR) maxR = c.getRadioCm();
            }
        }
        if (maxR <= 0.1) maxR = 5.0;
        double limite = Math.ceil(maxR * 1.15 * 10.0) / 10.0;

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(-limite, limite);
        plot.getRangeAxis().setRange(-limite, limite);

        renderizarEnPanel(chart, motor, true);
    }

    // Vista 2: Perfil de Corte Transversal z(r) con recorrido continuo sin cruce por el centro
    public void mostrarPerfilEnvase(SrvMotorCalculo motor, ResultadoCalculo res) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        // autoSort = false para respetar el orden continuo del contorno
        XYSeries paredInt = new XYSeries("Pared Interior z = f(r,0)", false);
        XYSeries paredExt = new XYSeries("Pared Exterior (+Espesor)", false);

        double zMin = motor.evaluarZ(0, 0);
        int pasos = 50;

        // Pared interior: 1° Recorre la pared izquierda desde arriba (h) hacia la base usando -r
        for (int i = pasos; i >= 0; i--) {
            double z = zMin + (res.alturaIngresada - zMin) * i / (double) pasos;
            double r = motor.hallarRadioEnNivel(z, 0.0);
            paredInt.add(-r, z);
        }
        // Pared interior: 2° Recorre la pared derecha desde la base hacia arriba (h) usando +r
        for (int i = 0; i <= pasos; i++) {
            double z = zMin + (res.alturaIngresada - zMin) * i / (double) pasos;
            double r = motor.hallarRadioEnNivel(z, 0.0);
            paredInt.add(r, z);
        }

        // Pared exterior con espesor: de arriba a base por la izquierda (-rExt)
        for (int i = pasos; i >= 0; i--) {
            double z = zMin + (res.alturaIngresada - zMin) * i / (double) pasos;
            double r = motor.hallarRadioEnNivel(z, 0.0);
            double rExt = r + res.espesor;
            double zExt = z - res.espesor * 0.4;
            paredExt.add(-rExt, zExt);
        }
        // Pared exterior: de la base hacia arriba por la derecha (+rExt)
        for (int i = 0; i <= pasos; i++) {
            double z = zMin + (res.alturaIngresada - zMin) * i / (double) pasos;
            double r = motor.hallarRadioEnNivel(z, 0.0);
            double rExt = r + res.espesor;
            double zExt = z - res.espesor * 0.4;
            paredExt.add(rExt, zExt);
        }

        dataset.addSeries(paredInt);
        dataset.addSeries(paredExt);

        if (res.cotas != null) {
            for (CotaGraduacion c : res.cotas) {
                XYSeries nivel = new XYSeries(String.format("Marca %.0f ml", c.getVolumenMl()), false);
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
        XYSeries serieVH = new XYSeries("Capacidad V(h)", false);
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

    private void renderizarEnPanel(JFreeChart chart, SrvMotorCalculo motor, boolean esVistaXY) {
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
                lblCursor.setText(String.format(Locale.US, "Cursor:  X: %.2f ; Y: %.2f ; Z: %.2f", x, y, z));
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
