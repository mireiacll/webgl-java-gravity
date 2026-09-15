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

// Draws a texture full-screen using its own dedicated shader.
// Port of ScreenRenderer.js.
public class ScreenRenderer {
    private ScreenShader shader;
    private int vao;
    private int vertexBuffer;

    public ScreenRenderer() {
        this.shader = new ScreenShader();
        buildQuad();
    }

    public ScreenShader getShader() { return shader; }

    private void buildQuad() {
        // interleaved: position(3) + texCoord(2) = 5 floats per vertex,
        // a full-screen quad made of 2 triangles (6 vertices, no index buffer needed)
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

    public void render(int texture) {
        glUseProgram(shader.getProgram());
        glBindVertexArray(vao);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glUniform1i(shader.getTextureLoc(), 0);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
    }
}