package core;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// VelocityShader + PositionShader
public class ParticleUpdateShader {

    private int program;
    private int positionLoc;
    private int texCoordLoc;
    private int uVelocityLoc;
    private int uPositionLoc;
    private int uGravityLoc;
    private int uWorldSizeLoc;
    private int uDtLoc;
    private int uDampingLoc;
    private int uEdgeElasticityLoc;

    public ParticleUpdateShader() {
        program = buildProgram();
        positionLoc = glGetAttribLocation(program, "a_position");
        texCoordLoc = glGetAttribLocation(program, "a_texCoord");
        uVelocityLoc = glGetUniformLocation(program, "u_velocity");
        uPositionLoc = glGetUniformLocation(program, "u_position");
        uGravityLoc = glGetUniformLocation(program, "u_gravity");
        uWorldSizeLoc = glGetUniformLocation(program, "u_worldSize");
        uDtLoc = glGetUniformLocation(program, "u_dt");
        uDampingLoc = glGetUniformLocation(program, "u_damping");
        uEdgeElasticityLoc = glGetUniformLocation(program, "u_edgeElasticity");
    }

    private String loadResource(String fileName) {
        try (InputStream in = getClass().getResourceAsStream(fileName)) {
            if (in == null) throw new IOException("Resource not found: " + fileName);
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
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Particle update shader compilation failed: " + glGetShaderInfoLog(shader));
            return -1;
        }
        return shader;
    }

    private int buildProgram() {
        String vertexSource = loadResource("/shaders/screenVertex.glsl"); // reused as-is, no changes needed
        String fragmentSource = loadResource("/shaders/particleUpdateFragment.glsl");

        int vertexShader = compile(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compile(GL_FRAGMENT_SHADER, fragmentSource);
        if (vertexShader == -1 || fragmentShader == -1) return -1;

        int prog = glCreateProgram();
        glAttachShader(prog, vertexShader);
        glAttachShader(prog, fragmentShader);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Particle update program linking failed: " + glGetProgramInfoLog(prog));
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return prog;
    }

    public int getProgram() { return program; }
    public int getPositionLoc() { return positionLoc; }
    public int getTexCoordLoc() { return texCoordLoc; }
    public int getUVelocityLoc() { return uVelocityLoc; }
    public int getUPositionLoc() { return uPositionLoc; }
    public int getUGravityLoc() { return uGravityLoc; }
    public int getUWorldSizeLoc() { return uWorldSizeLoc; }
    public int getUDtLoc() { return uDtLoc; }
    public int getUDampingLoc() { return uDampingLoc; }
    public int getUEdgeElasticityLoc() { return uEdgeElasticityLoc; }
}