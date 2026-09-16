package core;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.FloatBuffer;

public class PointsRenderer {
    private PointShader shader;
    private int vao;
    private int vbo;
    private int particleCount;
    private float pointSize = 4.0f;

    public PointsRenderer(int particleCount) {
        this.shader = new PointShader();
        this.particleCount = particleCount;

        float[] indices = new float[particleCount];
        for (int i = 0; i < particleCount; i++) indices[i] = i;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = memAllocFloat(indices.length);
        try {
            buffer.put(indices).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW); // set once, never changes
        } finally {
            memFree(buffer);
        }

        int indexLoc = shader.getIndexLoc();
        glEnableVertexAttribArray(indexLoc);
        glVertexAttribPointer(indexLoc, 1, GL_FLOAT, false, Float.BYTES, 0);

        glBindVertexArray(0);
    }

    public PointShader getShader() { return shader; }
    public void setPointSize(float size) { this.pointSize = size; }

    // particlesTexId/particlesRes = the position texture and its square side length.
    // Points are drawn plain white - no field, no color ramp needed here at all.
    public void render(int particlesTexId, float particlesRes) {
        glEnable(GL_PROGRAM_POINT_SIZE);
        glUseProgram(shader.getProgram());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, particlesTexId);
        glUniform1i(shader.getUParticlesLoc(), 0);
        glUniform1f(shader.getUParticlesResLoc(), particlesRes);

        glUniform1f(shader.getUPointSizeLoc(), pointSize);

        glBindVertexArray(vao);
        glDrawArrays(GL_POINTS, 0, particleCount);
        glBindVertexArray(0);
    }

    // needed now that particle count can change at runtime:
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}