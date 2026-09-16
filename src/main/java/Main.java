import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL;

import core.FadeRenderer;
import core.MireiaFBO;
import core.PointTextureBuilder;
import core.PointsRenderer;
import core.ScreenRenderer;
import core.Texture;
import core.ParticlePingPongBuffer;
import core.ParticleUpdateRenderer;
import core.VelocityTextureBuilder;
import geometry.MireiaPoint;

public class Main {

    // fraction of alpha kept each frame
    private static final float TRAIL_FADE = 0.99f;

    // gravity is a constant acceleration applied uniformly to every particle.
    // gl_Position.y = 1.0 - 2.0*pos.y (an inverted y), so POSITIVE y-velocity
    private static final float GRAVITY_X = 0.1f;
    private static final float GRAVITY_Y = 0.5f;
    private static final float DAMPING = 1.0f;           // per-frame drag, 1.0 = none
    private static final float RESTITUTION = 0.7f;       // energy kept per bounce, 1.0 = elastic, 0.0 = dead stop
    private static final float MAX_INITIAL_SPEED = 0.2f; // cap on each particle's random starting speed

    public static void main(String[] args) {

        // ---- window + GL context ----
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long window = glfwCreateWindow(800, 600, "Hello Window", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetFramebufferSize(window, windowWidth, windowHeight);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // ---- particle data ----
        List<MireiaPoint> particles = generateParticles(100);

        BufferedImage particlePositionImage = PointTextureBuilder.build(particles);
        Texture particlePositionTexture = new Texture(particlePositionImage, false); // data texture, no flip
        float particlesRes = PointTextureBuilder.gridSize(particles.size());
        int res = (int) particlesRes;

        // ---- renderers ----
        ScreenRenderer screenRenderer = new ScreenRenderer();
        PointsRenderer pointsRenderer = new PointsRenderer(particles.size());
        pointsRenderer.setPointSize(2.0f); // set once here, not every frame
        ParticleUpdateRenderer particleUpdateRenderer = new ParticleUpdateRenderer();
        FadeRenderer fadeRenderer = new FadeRenderer();

        // particle position + velocity, ping-ponged together
        // attachment 0 = velocity (raw GL_RG32F floats)
        // attachment 1 = position (packed 8-bit). 
        ParticlePingPongBuffer particlePingPong = new ParticlePingPongBuffer(res, res);

        particlePingPong.current().bindPositionOnly();
        screenRenderer.render(particlePositionTexture.getTextureId());
        particlePingPong.current().unbind();

        int randomVelocityTexture = VelocityTextureBuilder.build(particles.size(), res, MAX_INITIAL_SPEED);
        particlePingPong.current().bindVelocityOnly();
        screenRenderer.render(randomVelocityTexture);
        particlePingPong.current().unbind();

        // on-screen trail: a single fixed-resolution FBO, faded in-place by FadeRenderer.
        MireiaFBO trailFBO = new MireiaFBO(windowWidth[0], windowHeight[0], GL_LINEAR, GL_LINEAR);
        clearToTransparent(trailFBO);

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            windowWidth[0] = w;
            windowHeight[0] = h;
            glViewport(0, 0, w, h);
        });

        long lastTime = System.nanoTime();

        // ---- render loop ----
        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            dt = Math.min(Math.max(dt, 0.001f), 1f / 30f);
            lastTime = now;

            // 1. update velocity (gravity + edge rebound) 
            particlePingPong.next().bind();
            particleUpdateRenderer.render(
                particlePingPong.current().getVelocityBuffer(),
                particlePingPong.current().getPositionBuffer(),
                GRAVITY_X, GRAVITY_Y, dt, DAMPING, RESTITUTION);
            particlePingPong.next().unbind();
            particlePingPong.swap();

            // 2. Fade the existing single trail texture in place, then draw the
            //    current particle points on top of that same texture 
            trailFBO.bind();
            fadeRenderer.render(trailFBO.getColorBuffer(), TRAIL_FADE);

            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            pointsRenderer.render(particlePingPong.current().getPositionBuffer(), particlesRes);
            glDisable(GL_BLEND);

            trailFBO.unbind();

            // 3. composite: just the trail+particles layer over a cleared background
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, windowWidth[0], windowHeight[0]);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            screenRenderer.render(trailFBO.getColorBuffer());
            glDisable(GL_BLEND);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        glfwTerminate();
    }

    // wipes an FBO to fully transparent black - otherwise its first frame is whatever garbage the GPU handed back
    private static void clearToTransparent(MireiaFBO fbo) {
        fbo.bind();
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT);
        fbo.unbind();
    }

    // random scatter particles
    private static List<MireiaPoint> generateParticles(int count) {
        List<MireiaPoint> particles = new ArrayList<>(count);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * 2f - 1f;
            float y = random.nextFloat() * 2f - 1f;
            particles.add(new MireiaPoint(x, y));
        }
        return particles;
    }
}