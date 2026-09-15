package core;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

// Same full-screen-quad setup as ScreenRenderer, but writes the sampled texture
// back out with its alpha multiplied down. Used every frame on the trail buffer
// instead of clearing it, so old particle positions fade out exponentially
// (1, 0.5, 0.25, 0.125...) rather than vanishing outright.
public class FadeRenderer {
    private FadeShader shader;
    private int vao;
    private int vertexBuffer;

    public FadeRenderer() {
        this.shader = new FadeShader();
        buildQuad();
    }

    private void buildQuad() {
        float[] data = new float[] {
            -1, -1, 0,  0, 0,
             1, -1, 0,  1, 0,
             1,  1, 0,  1, 1,

            -1, -1, 0,  0, 0,
             1,  1, 0,  1, 1,
            -1,  1, 0,  0, 1,
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);

        FloatBuffer buffer = memAllocFloat(data.length);
        try {
            buffer.put(data).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            memFree(buffer);
        }

        int stride = 5 * Float.BYTES;

        int positionLoc = shader.getPositionLoc();
        glEnableVertexAttribArray(positionLoc);
        glVertexAttribPointer(positionLoc, 3, GL_FLOAT, false, stride, 0);

        int texCoordLoc = shader.getTexCoordLoc();
        glEnableVertexAttribArray(texCoordLoc);
        glVertexAttribPointer(texCoordLoc, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

        glBindVertexArray(0);
    }

    // fade: fraction of alpha kept from the source texture, 0..1 (e.g. 0.5 = halves every frame)
    public void render(int texture, float fade) {
        glUseProgram(shader.getProgram());
        glBindVertexArray(vao);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glUniform1i(shader.getTextureLoc(), 0);
        glUniform1f(shader.getFadeLoc(), fade);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
    }
}