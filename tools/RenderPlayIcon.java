import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * Renders the Google Play hi-res icon (512 x 512 PNG) from the same geometry as the adaptive
 * launcher icon in app/src/main/res/drawable/ic_launcher_{background,foreground}.xml, so the store
 * listing shows exactly what lands on the home screen.
 *
 * The adaptive icon is drawn on a 108 dp canvas of which launchers show the central 72 dp; the
 * outer ring exists for parallax only. Play wants a full-bleed square and applies its own corner
 * mask, so this renders that visible 72 dp window scaled to 512 px, with no rounding of its own.
 *
 * Run from the repository root (JDK 11+, no build step):
 *   java tools/RenderPlayIcon.java [output.png] [size]
 */
public final class RenderPlayIcon {

    private static final double CANVAS = 108.0;
    private static final double VISIBLE = 72.0;
    private static final double INSET = (CANVAS - VISIBLE) / 2.0;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "docs/release/assets/play-icon-512.png");
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 512;

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Map the visible 72 dp window of the 108 dp canvas onto the whole image.
        double scale = size / VISIBLE;
        g.setTransform(new AffineTransform(scale, 0, 0, scale, -INSET * scale, -INSET * scale));
        Rectangle2D canvas = new Rectangle2D.Double(0, 0, CANVAS, CANVAS);

        // ic_launcher_background: app background fading to the elevated surface ...
        g.setPaint(new LinearGradientPaint(
            new Point2D.Double(54, 0), new Point2D.Double(54, 108),
            new float[] {0f, 1f},
            new Color[] {argb(0xFF141D33), argb(0xFF070B14)}));
        g.fill(canvas);
        // ... with a faint icy glow in the top-right.
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(84, 24), 70f, new Point2D.Double(84, 24),
            new float[] {0f, 1f},
            new Color[] {argb(0x3D5EE7FF), argb(0x005EE7FF)},
            MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(canvas);

        // ic_launcher_foreground: soft glow disc behind the bolt ...
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(54, 54), 30f, new Point2D.Double(54, 54),
            new float[] {0f, 0.6f, 1f},
            new Color[] {argb(0x665EE7FF), argb(0x1A5EE7FF), argb(0x005EE7FF)},
            MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(24, 24, 60, 60));

        // ... and the bolt: M59.6,31.8 L45.2,52.9 L53.6,52.9 L47.6,76.2 L62.4,54.9 L54,54.9 Z
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(59.6, 31.8);
        bolt.lineTo(45.2, 52.9);
        bolt.lineTo(53.6, 52.9);
        bolt.lineTo(47.6, 76.2);
        bolt.lineTo(62.4, 54.9);
        bolt.lineTo(54.0, 54.9);
        bolt.closePath();
        g.setPaint(new LinearGradientPaint(
            new Point2D.Double(48, 32), new Point2D.Double(60, 76),
            new float[] {0f, 1f},
            new Color[] {argb(0xFF5EE7FF), argb(0xFF4DA3FF)}));
        g.fill(bolt);
        g.setPaint(argb(0xFF7FEFFF));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(bolt);
        g.dispose();

        Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
        ImageIO.write(image, "png", out);
        System.out.println("Wrote " + out + " (" + size + "x" + size + ", " + out.length() + " bytes)");
    }

    /** Android's AARRGGBB literal as an AWT colour. */
    private static Color argb(int value) {
        return new Color((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, (value >>> 24) & 0xFF);
    }
}
