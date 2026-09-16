package core;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.*;

// Multi-render-target FBO for the particle sim: one framebuffer, two color attachments
//   attachment 0 -> velocity, GL_RG32F, raw (vx, vy) floats
//   attachment 1 -> position, GL_RGBA8, packed 8-bit encoding (same as before)
public class ParticleFBO {
    private final int width;
    private final int height;
    private int framebuffer;
    private int velocityBuffer;
    private int positionBuffer;

    // draw-buffer configs, built once and reused - glDrawBuffers wants an
    // IntBuffer, and this runs every frame so we don't want to allocate one each time.
    private final IntBuffer drawBoth = BufferUtils.createIntBuffer(2);
    private final IntBuffer drawVelocityOnly = BufferUtils.createIntBuffer(1);
    private final IntBuffer drawPositionOnly = BufferUtils.createIntBuffer(1);

    public ParticleFBO(int width, int height) {
        this.width = width;
        this.height = height;

        drawBoth.put(GL_COLOR_ATTACHMENT0).put(GL_COLOR_ATTACHMENT1).flip();
        drawVelocityOnly.put(GL_COLOR_ATTACHMENT0).flip();
        drawPositionOnly.put(GL_COLOR_ATTACHMENT1).flip();

        build();
    }

    private int createAttachment(int attachmentPoint, int internalFormat, int format, int type) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, attachmentPoint, GL_TEXTURE_2D, texture, 0);
        return texture;
    }

    private void build() {
        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        velocityBuffer = createAttachment(GL_COLOR_ATTACHMENT0, GL_RG32F, GL_RG, GL_FLOAT);
        positionBuffer = createAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);

        glDrawBuffers(drawBoth);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ParticleFBO: framebuffer incomplete, status: " + Integer.toHexString(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // normal simulation pass - both attachments are write targets
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
        glDrawBuffers(drawBoth);
    }

    // seeding helpers: narrow to a single attachment so screenRenderer.render()
    public void bindVelocityOnly() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
        glDrawBuffers(drawVelocityOnly);
    }

    public void bindPositionOnly() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
        glDrawBuffers(drawPositionOnly);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // needed now that particle count can change at runtime: a resized
    // ParticlePingPongBuffer means these two ParticleFBOs get replaced, so the
    // old framebuffer + textures must be freed or every Start leaks GPU memory.
    public void dispose() {
        glDeleteFramebuffers(framebuffer);
        glDeleteTextures(velocityBuffer);
        glDeleteTextures(positionBuffer);
    }

    public int getVelocityBuffer() { return velocityBuffer; }
    public int getPositionBuffer() { return positionBuffer; }
}