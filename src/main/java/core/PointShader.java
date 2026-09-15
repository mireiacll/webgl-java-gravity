package core;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PointShader {
    private int program;
    private int indexLoc;
    private int uParticlesLoc;
    private int uParticlesResLoc;
    private int uPointSizeLoc;

    public PointShader() {
        program = buildProgram();
        indexLoc = glGetAttribLocation(program, "a_index");
        uParticlesLoc = glGetUniformLocation(program, "u_particles");
        uParticlesResLoc = glGetUniformLocation(program, "u_particles_res");
        uPointSizeLoc = glGetUniformLocation(program, "u_pointSize");
    }

    private String loadResource(String fileName) {
        try (InputStream in = getClass().getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IOException("Resource not found: " + fileName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            System.err.println("Point shader compilation failed: " + glGetShaderInfoLog(shader));
            return -1;
        }
        return shader;
    }

    private int buildProgram() {
        String vertexShaderSource = loadResource("/shaders/pointVertex.glsl");
        String fragmentShaderSource = loadResource("/shaders/pointFragment.glsl");

        int vertexShader = compile(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compile(GL_FRAGMENT_SHADER, fragmentShaderSource);

        if (vertexShader == -1 || fragmentShader == -1) {
            return -1;
        }

        int prog = glCreateProgram();
        glAttachShader(prog, vertexShader);
        glAttachShader(prog, fragmentShader);
        glLinkProgram(prog);

        int status = glGetProgrami(prog, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            System.err.println("Point program linking failed: " + glGetProgramInfoLog(prog));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return prog;
    }

    public int getProgram() { return program; }
    public int getIndexLoc() { return indexLoc; }
    public int getUParticlesLoc() { return uParticlesLoc; }
    public int getUParticlesResLoc() { return uParticlesResLoc; }
    public int getUPointSizeLoc() { return uPointSizeLoc; }
}