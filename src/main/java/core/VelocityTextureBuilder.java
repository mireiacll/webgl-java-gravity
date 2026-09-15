package core;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

// Builds a GL_RG32F data texture of random (vx, vy) velocity vectors 
public class VelocityTextureBuilder {

    public static int build(int particleCount, int side, float maxSpeed) {
        FloatBuffer buffer = memAllocFloat(side * side * 2);
        try {
            Random random = new Random();
            for (int i = 0; i < side * side; i++) {
                if (i < particleCount) {
                    float angle = random.nextFloat() * (float) (Math.PI * 2);
                    float speed = random.nextFloat() * maxSpeed;
                    buffer.put((float) Math.cos(angle) * speed);
                    buffer.put((float) Math.sin(angle) * speed);
                } else {
                    buffer.put(0f).put(0f); // unused cells past particleCount - harmless zeros
                }
            }
            buffer.flip();

            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, side, side, 0, GL_RG, GL_FLOAT, buffer);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D, 0);

            return texture;
        } finally {
            memFree(buffer);
        }
    }
}