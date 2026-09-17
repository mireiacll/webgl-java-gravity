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

// Draws the obstacle mask as a tinted, semi-transparent overlay 
public class ObstacleRenderer {
    private ObstacleShader shader;
    private int vao;
    private int vertexBuffer;

    public ObstacleRenderer() {
        this.shader = new ObstacleShader();
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

    public void render(int texture, float r, float g, float b) {
        glUseProgram(shader.getProgram());
        glBindVertexArray(vao);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glUniform1i(shader.getTextureLoc(), 0);
        glUniform3f(shader.getColorLoc(), r, g, b);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
    }
}