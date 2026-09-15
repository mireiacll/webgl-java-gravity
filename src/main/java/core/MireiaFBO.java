package core;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

// Minimal render-to-texture target: one color attachment + a depth renderbuffer
// so 3D geometry still z-tests correctly while rendering into it.
public class MireiaFBO {
    private int width;
    private int height;
    private int framebuffer;
    private int colorBuffer;
    private int depthBuffer;
    private int gl_tex_min_filter = GL_NEAREST;
    private int gl_tex_mag_filter = GL_NEAREST;

    // storage format for the color attachment - defaults preserve every existing
    // call site (packed 8-bit RGBA), but a velocity buffer can request GL_RG32F
    // instead so raw signed floats round-trip with no pack/unpack shader math.
    private int internalFormat = GL_RGBA8;
    private int format = GL_RGBA;
    private int type = GL_UNSIGNED_BYTE;

    public MireiaFBO(int width, int height) {
        this.width = width;
        this.height = height;
        build();
    }

    public MireiaFBO(int width, int height, int gl_tex_min_filter, int gl_tex_mag_filter) {
        this.width = width;
        this.height = height;
        this.gl_tex_min_filter = gl_tex_min_filter;
        this.gl_tex_mag_filter = gl_tex_mag_filter;
        build();
    }

    // e.g. new MireiaFBO(res, res, GL_NEAREST, GL_NEAREST, GL_RG32F, GL_RG, GL_FLOAT)
    // for a velocity ping-pong buffer that stores raw (vx, vy) floats.
    public MireiaFBO(int width, int height, int gl_tex_min_filter, int gl_tex_mag_filter,
                      int internalFormat, int format, int type) {
        this.width = width;
        this.height = height;
        this.gl_tex_min_filter = gl_tex_min_filter;
        this.gl_tex_mag_filter = gl_tex_mag_filter;
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
        build();
    }

    private int createColorTexture(int attachmentPoint, int width, int height) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, gl_tex_min_filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, gl_tex_mag_filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, attachmentPoint, GL_TEXTURE_2D, texture, 0);
        return texture;
    }

    private void build() {
        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        colorBuffer = createColorTexture(GL_COLOR_ATTACHMENT0, width, height);

        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("MireiaFBO: framebuffer incomplete, status: " + Integer.toHexString(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    // redirects subsequent draw calls into this FBO instead of the screen
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // resizing means the old GPU resources are the wrong size - build new ones,
    // stretch whatever was in the old ones into the new size, then delete the old ones.
    public void setSize(int newWidth, int newHeight) {
        int oldFramebuffer = framebuffer;
        int oldColorBuffer = colorBuffer;
        int oldDepthBuffer = depthBuffer;
        int oldWidth = width;
        int oldHeight = height;

        this.width = newWidth;
        this.height = newHeight;
        build(); // overwrites framebuffer/colorBuffer/depthBuffer with the new ones

        glBindFramebuffer(GL_READ_FRAMEBUFFER, oldFramebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        glBlitFramebuffer(0, 0, oldWidth, oldHeight, 0, 0, newWidth, newHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glDeleteFramebuffers(oldFramebuffer);
        glDeleteTextures(oldColorBuffer);
        glDeleteRenderbuffers(oldDepthBuffer);
    }

    public int getFramebuffer() { return framebuffer; }
    public int getColorBuffer() { return colorBuffer; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}