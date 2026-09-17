import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import core.FadeRenderer;
import core.MireiaFBO;
import core.ObstacleRenderer;
import core.PointTextureBuilder;
import core.PointsRenderer;
import core.ScreenRenderer;
import core.Texture;
import core.ParticlePingPongBuffer;
import core.ParticleUpdateRenderer;
import core.VelocityTextureBuilder;
import geometry.MireiaPoint;

public class Main {

    private static final int DEFAULT_PARTICLE_COUNT = 100;

    // fraction of alpha kept each frame
    private static final float TRAIL_FADE = 0.99f;

    // gravity is a constant acceleration applied uniformly to every particle.
    // gl_Position.y = 1.0 - 2.0*pos.y (an inverted y), so POSITIVE y-velocity
    private static final float GRAVITY_X = 0f;
    private static final float GRAVITY_Y = 9.8f;         // real gravity, m/s^2

    // defaults for the values the user can tweak before each run
    private static final float DEFAULT_DAMPING = 0.999f;           // per-frame drag, 1.0 = none
    private static final float DEFAULT_RESTITUTION = 0.9f;       // energy kept per bounce, 1.0 = elastic, 0.0 = dead stop
    private static final float DEFAULT_MAX_INITIAL_SPEED = 20.0f; // cap on each particle's random starting speed, m/s
    private static final float DEFAULT_WORLD_WIDTH_METERS = 1000f; // scene width in meters - height is derived from the window's aspect ratio

    // obstacle mask: drawn in Paint, black lines on a white canvas
    private static final String OBSTACLE_IMAGE_PATH = "/textures/obstacles2.png";
    private static final float DEFAULT_OBSTACLE_ELASTICITY = 0.9f; // energy kept per obstacle bounce, same idea as restitution
    private static final float[] OBSTACLE_COLOR = { 0.9f, 0.25f, 0.25f }; // red - distinct from the white particles

    // Start disabled unless IDLE, Stop disabled unless RUNNING, Replay disabled unless STOPPED.
    private enum SimState { IDLE, RUNNING, STOPPED }

    // Particle count changes the *size* of GPU resources (PointsRenderer's VBO,
    // ParticlePingPongBuffer's textures), so unlike the float parameters it needs
    // its own pointsRenderer/particlePingPong/res/particlesRes rebuilt alongside it.
    private static final class SimResources {
        final PointsRenderer pointsRenderer;
        final ParticlePingPongBuffer particlePingPong;
        final int res;
        final float particlesRes;

        SimResources(PointsRenderer pointsRenderer, ParticlePingPongBuffer particlePingPong, int res, float particlesRes) {
            this.pointsRenderer = pointsRenderer;
            this.particlePingPong = particlePingPong;
            this.res = res;
            this.particlesRes = particlesRes;
        }
    }

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

        // ---- ImGui setup ----
        ImGui.createContext();
        ImGuiImplGlfw imguiGlfw = new ImGuiImplGlfw();
        imguiGlfw.init(window, true);
        ImGuiImplGl3 imguiGl3 = new ImGuiImplGl3();
        imguiGl3.init("#version 330 core");

        // ---- renderers that don't depend on particle count/run parameters ----
        ScreenRenderer screenRenderer = new ScreenRenderer();
        ParticleUpdateRenderer particleUpdateRenderer = new ParticleUpdateRenderer();
        FadeRenderer fadeRenderer = new FadeRenderer();
        ObstacleRenderer obstacleRenderer = new ObstacleRenderer();

        // on-screen trail: a single fixed-resolution FBO, faded in-place by FadeRenderer.
        MireiaFBO trailFBO = new MireiaFBO(windowWidth[0], windowHeight[0], GL_LINEAR, GL_LINEAR);
        clearToTransparent(trailFBO);

        // obstacle mask - loaded once, sampled by u_obstacles in the update shader
        // AND drawn on screen by obstacleRenderer so you can see what you drew.
        // flip=false: row order has to match posUV's top-down convention, not the
        // "normal image" flip - see Texture.java.
        Texture obstacleTexture;
        float obstacleTexelX;
        float obstacleTexelY;
        float[] obstacleBoundsUV;
        try (InputStream in = Main.class.getResourceAsStream(OBSTACLE_IMAGE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + OBSTACLE_IMAGE_PATH);
            }
            BufferedImage obstacleImage = ImageIO.read(in);
            if (obstacleImage == null) {
                throw new IOException("ImageIO couldn't decode " + OBSTACLE_IMAGE_PATH);
            }
            obstacleTexture = new Texture(obstacleImage, false);
            obstacleTexelX = 1f / obstacleImage.getWidth();
            obstacleTexelY = 1f / obstacleImage.getHeight();
            obstacleBoundsUV = computeObstacleBoundsUV(obstacleImage);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load obstacle image at " + OBSTACLE_IMAGE_PATH
                + " - make sure it's at src/main/resources/textures/obstacles.png", e);
        }

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            windowWidth[0] = w;
            windowHeight[0] = h;
            glViewport(0, 0, w, h);
        });

        // ---- editable run parameters (ImGui widgets take a length-1 array the same
        //      way the framebuffer-size callback above takes int[1]) ----
        int[] particleCount = { DEFAULT_PARTICLE_COUNT };
        float[] damping = { DEFAULT_DAMPING };
        float[] restitution = { DEFAULT_RESTITUTION };
        float[] maxInitialSpeed = { DEFAULT_MAX_INITIAL_SPEED };
        float[] worldWidth = { DEFAULT_WORLD_WIDTH_METERS };
        float[] obstacleElasticity = { DEFAULT_OBSTACLE_ELASTICITY };

        // particle-count-sized resources - (re)built by buildSimResources() on every Start
        SimResources sim = buildSimResources(particleCount[0]);
        PointsRenderer pointsRenderer = sim.pointsRenderer;
        ParticlePingPongBuffer particlePingPong = sim.particlePingPong;
        int res = sim.res;
        float particlesRes = sim.particlesRes;

        SimState state = SimState.IDLE;
        long lastTime = System.nanoTime();

        // ---- render loop ----
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            dt = Math.min(Math.max(dt, 0.001f), 1f / 30f);
            lastTime = now;

            // recomputed every frame since the window can resize
            float worldHeight = worldWidth[0] * windowHeight[0] / (float) windowWidth[0];

            if (state == SimState.RUNNING) {
                // 1. update velocity (gravity + edge rebound + obstacle rebound)
                particlePingPong.next().bind();
                particleUpdateRenderer.render(
                    particlePingPong.current().getVelocityBuffer(),
                    particlePingPong.current().getPositionBuffer(),
                    obstacleTexture.getTextureId(), obstacleTexelX, obstacleTexelY,
                    obstacleBoundsUV[0], obstacleBoundsUV[1], obstacleBoundsUV[2], obstacleBoundsUV[3],
                    obstacleElasticity[0],
                    GRAVITY_X, GRAVITY_Y, worldWidth[0], worldHeight, dt, damping[0], restitution[0]);
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
            }
            // when STOPPED: skip the update+draw above entirely, so the trail
            // buffer keeps whatever was in it and the frame stays frozen.

            // 3. composite: obstacle overlay first, then the trail+particles layer, over a cleared background.
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, windowWidth[0], windowHeight[0]);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            obstacleRenderer.render(obstacleTexture.getTextureId(), OBSTACLE_COLOR[0], OBSTACLE_COLOR[1], OBSTACLE_COLOR[2]);
            screenRenderer.render(trailFBO.getColorBuffer());
            glDisable(GL_BLEND);

            // ---- ImGui panel: Start / Stop / Replay + pre-run parameters ----
            imguiGl3.newFrame();
            imguiGlfw.newFrame();
            ImGui.newFrame();

            ImGui.begin("Simulation Controls");
            ImGui.text("State: " + state);
            ImGui.separator();

            ImGui.beginDisabled(state != SimState.IDLE);
            if (ImGui.button("Start")) {
                // particle count changes buffer sizes, so rebuild those resources first
                pointsRenderer.dispose();
                particlePingPong.dispose();
                SimResources newSim = buildSimResources(particleCount[0]);
                pointsRenderer = newSim.pointsRenderer;
                particlePingPong = newSim.particlePingPong;
                res = newSim.res;
                particlesRes = newSim.particlesRes;

                resetSimulation(particlePingPong, trailFBO, screenRenderer, res, particleCount[0], maxInitialSpeed[0]);
                lastTime = System.nanoTime();
                state = SimState.RUNNING;
            }
            ImGui.endDisabled();

            ImGui.sameLine();
            ImGui.beginDisabled(state != SimState.RUNNING);
            if (ImGui.button("Stop")) {
                state = SimState.STOPPED;
            }
            ImGui.endDisabled();

            ImGui.sameLine();
            ImGui.beginDisabled(state != SimState.STOPPED);
            if (ImGui.button("Delete")) {
                clearToTransparent(trailFBO);
                state = SimState.IDLE; // back to IDLE so parameters are editable again before the next run
            }
            ImGui.endDisabled();

            ImGui.separator();
            ImGui.text("Parameters (locked while running)");
            ImGui.text("Screen width: " + windowWidth[0] + " px");
            ImGui.beginDisabled(state != SimState.IDLE);
            ImGui.dragInt("Particle count", particleCount, 1, 1, 20000);
            ImGui.dragFloat("Damping", damping, 0.01f, 0.0f, 2.0f, "%.2f");
            ImGui.dragFloat("Restitution", restitution, 0.01f, 0.0f, 1.0f, "%.2f");
            ImGui.dragFloat("Obstacle elasticity", obstacleElasticity, 0.01f, 0.0f, 1.0f, "%.2f");
            ImGui.dragFloat("Max initial speed (m/s)", maxInitialSpeed, 0.5f, 0.0f, 200.0f, "%.1f");
            ImGui.dragFloat("World width (m)", worldWidth, 5.0f, 10.0f, 5000.0f, "%.0f");
            ImGui.endDisabled();

            ImGui.end();

            ImGui.render();
            imguiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
        }

        pointsRenderer.dispose();
        particlePingPong.dispose();
        imguiGl3.shutdown();
        imguiGlfw.shutdown();
        ImGui.destroyContext();

        glfwTerminate();
    }

    // wipes an FBO to fully transparent black - otherwise its first frame is whatever garbage the GPU handed back
    private static void clearToTransparent(MireiaFBO fbo) {
        fbo.bind();
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT);
        fbo.unbind();
    }

    // scans the obstacle image once for its bounding box in UV space 
    private static float[] computeObstacleBoundsUV(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minX = width, minY = height, maxX = -1, maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                float luminance = (r + g + b) / 3f / 255f;
                if (luminance < 0.5f) { // dark = obstacle, same threshold as the shader
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) {
            // nothing drawn - empty bounds, every overlap test in the shader
            // fails and the march never runs for anyone
            return new float[] { 1f, 1f, 0f, 0f };
        }

        // pad by one texel so the gradient sample (which reads neighbors) never
        // gets clipped right at the bounding box edge
        float padX = 1f / width, padY = 1f / height;
        float minU = Math.max(0f, minX / (float) width - padX);
        float minV = Math.max(0f, minY / (float) height - padY);
        float maxU = Math.min(1f, (maxX + 1) / (float) width + padX);
        float maxV = Math.min(1f, (maxY + 1) / (float) height + padY);
        return new float[] { minU, minV, maxU, maxV };
    }

    // builds the particle-count-sized resources - called on startup and on every Start click
    private static SimResources buildSimResources(int particleCount) {
        PointsRenderer pointsRenderer = new PointsRenderer(particleCount);
        pointsRenderer.setPointSize(2.0f);

        float particlesRes = PointTextureBuilder.gridSize(particleCount);
        int res = (int) particlesRes;
        ParticlePingPongBuffer particlePingPong = new ParticlePingPongBuffer(res, res);

        return new SimResources(pointsRenderer, particlePingPong, res, particlesRes);
    }

    // (Re)seeds the particle sim for a fresh run
    private static void resetSimulation(ParticlePingPongBuffer particlePingPong, MireiaFBO trailFBO,
                                         ScreenRenderer screenRenderer, int res, int particleCount,
                                         float maxInitialSpeed) {
        clearToTransparent(trailFBO);

        List<MireiaPoint> particles = generateParticles(particleCount);

        BufferedImage particlePositionImage = PointTextureBuilder.build(particles);
        Texture particlePositionTexture = new Texture(particlePositionImage, false); // data texture, no flip
        particlePingPong.current().bindPositionOnly();
        screenRenderer.render(particlePositionTexture.getTextureId());
        particlePingPong.current().unbind();

        int randomVelocityTexture = VelocityTextureBuilder.build(particleCount, res, maxInitialSpeed);
        particlePingPong.current().bindVelocityOnly();
        screenRenderer.render(randomVelocityTexture);
        particlePingPong.current().unbind();

        // these source textures are only needed to seed the buffer above - unlike the
        // original single-run version, Start can now run many times per session, so they
        // must be freed here or every Start/Replay leaks a texture.
        glDeleteTextures(particlePositionTexture.getTextureId());
        glDeleteTextures(randomVelocityTexture);
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