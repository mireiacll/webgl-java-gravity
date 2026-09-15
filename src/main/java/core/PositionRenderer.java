package core;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public class PositionRenderer {
    private PositionShader shader;
    private int vao;
    private int vertexBuffer;

    public PositionRenderer() {
        this.shader = new PositionShader();
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

    // positionTexId = current packed-8-bit position texture (read from)
    // velocityTexId = this frame's already-updated GL_RG32F velocity texture
    public void render(int positionTexId, int velocityTexId, float dt) {
        glUseProgram(shader.getProgram());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, positionTexId);
        glUniform1i(shader.getUPositionLoc(), 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, velocityTexId);
        glUniform1i(shader.getUVelocityLoc(), 1);

        glUniform1f(shader.getUDtLoc(), dt);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
}