package Servicios;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.*;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javax.swing.JPanel;

public class SrvGenerador3D {

    private final JFXPanel jfxPanel;
    private Group contenedorMalla;
    private double anchorX, anchorY;
    private double anchorAngleX = 22.0, anchorAngleY = -30.0;
    private final Rotate rotateX = new Rotate(22.0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30.0, Rotate.Y_AXIS);
    private double zoomZ = -34.0;
    private PerspectiveCamera camera;
    private boolean modoWireframe = false;

    public SrvGenerador3D(JPanel panelContenedorSwing) {
        Platform.setImplicitExit(false);
        this.jfxPanel = new JFXPanel();
        panelContenedorSwing.removeAll();
        panelContenedorSwing.setLayout(new BorderLayout());
        panelContenedorSwing.add(jfxPanel, BorderLayout.CENTER);
        panelContenedorSwing.revalidate();

        Platform.runLater(this::inicializarEscena);
    }

    private void inicializarEscena() {
        contenedorMalla = new Group();
        contenedorMalla.getTransforms().addAll(rotateX, rotateY);

        AmbientLight luzAmbiente = new AmbientLight(Color.rgb(155, 165, 175));
        PointLight luzPrincipal = new PointLight(Color.WHITE);
        luzPrincipal.setTranslateX(25);
        luzPrincipal.setTranslateY(-30);
        luzPrincipal.setTranslateZ(-35);

        Group root = new Group(contenedorMalla, luzAmbiente, luzPrincipal);
        Scene scene = new Scene(root, 500, 420, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.rgb(54, 54, 54));

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(500.0);
        camera.setTranslateZ(zoomZ);
        scene.setCamera(camera);

        // Rotación con arrastre de mouse
        scene.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        scene.setOnMouseDragged(e -> {
            rotateX.setAngle(anchorAngleX - (anchorY - e.getSceneY()) * 0.45);
            rotateY.setAngle(anchorAngleY + (anchorX - e.getSceneX()) * 0.45);
        });

        // Zoom con rueda
        scene.setOnScroll(e -> {
            zoomZ += e.getDeltaY() * 0.04;
            zoomZ = Math.max(-90.0, Math.min(-10.0, zoomZ));
            camera.setTranslateZ(zoomZ);
        });

        jfxPanel.setScene(scene);
    }

    public void setModoWireframe(boolean wireframe) {
        this.modoWireframe = wireframe;
    }

    public void actualizarEnvase3D(SrvMotorCalculo motor, double alturaH, double espesorE) {
        Platform.runLater(() -> {
            if (contenedorMalla == null) return;
            contenedorMalla.getChildren().clear();

            int nTheta = 48; // Optimizado para 60 FPS fluidos en Ryzen 5
            int nZ = 32;
            double zMin = motor.evaluarZ(0, 0);
            if (alturaH <= zMin) return;

            TriangleMesh mesh = new TriangleMesh();
            mesh.getTexCoords().addAll(0, 0);

            double centroY = (alturaH - zMin) / 2.0;

            // Puntos pared interior (0 .. (nZ+1)*nTheta - 1)
            for (int iz = 0; iz <= nZ; iz++) {
                double z = zMin + (alturaH - zMin) * iz / nZ;
                for (int it = 0; it < nTheta; it++) {
                    double theta = 2.0 * Math.PI * it / nTheta;
                    double r = motor.hallarRadioEnNivel(z, theta);
                    float x = (float) (r * Math.cos(theta));
                    float y = (float) (-(z - zMin - centroY));
                    float zCoord = (float) (r * Math.sin(theta));
                    mesh.getPoints().addAll(x, y, zCoord);
                }
            }

            int offsetExt = (nZ + 1) * nTheta;
            // Puntos pared exterior con espesor E
            for (int iz = 0; iz <= nZ; iz++) {
                double z = zMin + (alturaH - zMin) * iz / nZ;
                for (int it = 0; it < nTheta; it++) {
                    double theta = 2.0 * Math.PI * it / nTheta;
                    double r = motor.hallarRadioEnNivel(z, theta) + espesorE;
                    float x = (float) (r * Math.cos(theta));
                    float y = (float) (-(z - zMin - centroY - espesorE * 0.5));
                    float zCoord = (float) (r * Math.sin(theta));
                    mesh.getPoints().addAll(x, y, zCoord);
                }
            }

            // Caras laterales (interior y exterior)
            for (int iz = 0; iz < nZ; iz++) {
                for (int it = 0; it < nTheta; it++) {
                    int itNext = (it + 1) % nTheta;

                    int i00 = iz * nTheta + it;
                    int i01 = iz * nTheta + itNext;
                    int i10 = (iz + 1) * nTheta + it;
                    int i11 = (iz + 1) * nTheta + itNext;

                    mesh.getFaces().addAll(i00, 0, i10, 0, i11, 0);
                    mesh.getFaces().addAll(i00, 0, i11, 0, i01, 0);

                    int e00 = offsetExt + i00;
                    int e01 = offsetExt + i01;
                    int e10 = offsetExt + i10;
                    int e11 = offsetExt + i11;

                    mesh.getFaces().addAll(e00, 0, e11, 0, e10, 0);
                    mesh.getFaces().addAll(e00, 0, e01, 0, e11, 0);
                }
            }

            // Anillo superior (borde del espesor)
            for (int it = 0; it < nTheta; it++) {
                int itNext = (it + 1) % nTheta;
                int topIn0 = nZ * nTheta + it;
                int topIn1 = nZ * nTheta + itNext;
                int topOut0 = offsetExt + topIn0;
                int topOut1 = offsetExt + topIn1;

                mesh.getFaces().addAll(topIn0, 0, topOut0, 0, topOut1, 0);
                mesh.getFaces().addAll(topIn0, 0, topOut1, 0, topIn1, 0);
            }

            // Tapa inferior del anillo base (iz = 0) uniendo interior y exterior
            for (int it = 0; it < nTheta; it++) {
                int itNext = (it + 1) % nTheta;
                int botIn0 = it;
                int botIn1 = itNext;
                int botOut0 = offsetExt + botIn0;
                int botOut1 = offsetExt + botIn1;

                mesh.getFaces().addAll(botIn0, 0, botIn1, 0, botOut1, 0);
                mesh.getFaces().addAll(botIn0, 0, botOut1, 0, botOut0, 0);
            }

            MeshView meshView = new MeshView(mesh);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.rgb(72, 169, 230, 0.72));
            material.setSpecularColor(Color.WHITE);
            meshView.setMaterial(material);
            meshView.setCullFace(CullFace.NONE);
            meshView.setDrawMode(modoWireframe ? DrawMode.LINE : DrawMode.FILL);

            contenedorMalla.getChildren().add(meshView);
        });
    }

    public BufferedImage capturarImagen3D() {
        final BufferedImage[] resultado = new BufferedImage[1];
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                if (jfxPanel.getScene() != null) {
                    WritableImage img = jfxPanel.getScene().snapshot(null);
                    resultado[0] = SwingFXUtils.fromFXImage(img, null);
                }
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        return resultado[0];
    }
}