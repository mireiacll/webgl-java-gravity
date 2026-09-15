package core;

import java.awt.image.BufferedImage;
import java.util.List;
import geometry.MireiaPoint;

public class PointTextureBuilder {

    // matches the decode in pointVertex.glsl: color.r/255 + color.b
    // fine = fractional remainder (byte), coarse = integer part of value*255 (byte)
    private static int[] packFloat255(float value) {
        float v = Math.max(0f, Math.min(1f, value)) * 255f;
        int coarse = (int) Math.floor(v);
        int fine = Math.round((v - coarse) * 255f);
        if (fine > 255) fine = 255; // guard rounding at the top edge
        return new int[] { fine, coarse };
    }

    // texture MUST be square for u_particles_res to mean anything -
    // side = smallest square that fits every particle, extra cells just go unused
    public static int gridSize(int count) {
        return (int) Math.ceil(Math.sqrt(Math.max(count, 1)));
    }

    public static BufferedImage build(List<MireiaPoint> particles) {
        int side = gridSize(particles.size());
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < particles.size(); i++) {
            float[] ndc = particles.get(i).toArray();
            float u = (ndc[0] + 1f) * 0.5f;
            float v = (ndc[1] + 1f) * 0.5f;

            int[] x = packFloat255(u); // x[0]=fine, x[1]=coarse
            int[] y = packFloat255(v); // y[0]=fine, y[1]=coarse

            // R=fineX, G=fineY, B=coarseX, A=coarseY
            int argb = (y[1] << 24) | (x[0] << 16) | (y[0] << 8) | x[1];

            image.setRGB(i % side, i / side, argb);
        }

        return image;
    }
}