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

// Replaces VelocityRenderer + PositionRenderer
public class ParticleUpdateRenderer {
    private ParticleUpdateShader shader;
    private int vao;
    private int vertexBuffer;

    public ParticleUpdateRenderer() {
        this.shader = new ParticleUpdateShader();
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

    // velocityTexId/positionTexId = last frame's finalized textures (read from).
    // Caller must bind() the target ParticleFBO (both attachments) before calling this.
    public void render(int velocityTexId, int positionTexId, float gravityX, float gravityY,
                        float dt, float damping, float edgeElasticity) {
        glUseProgram(shader.getProgram());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, velocityTexId);
        glUniform1i(shader.getUVelocityLoc(), 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, positionTexId);
        glUniform1i(shader.getUPositionLoc(), 1);

        glUniform2f(shader.getUGravityLoc(), gravityX, gravityY);
        glUniform1f(shader.getUDtLoc(), dt);
        glUniform1f(shader.getUDampingLoc(), damping);
        glUniform1f(shader.getUEdgeElasticityLoc(), edgeElasticity);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
}