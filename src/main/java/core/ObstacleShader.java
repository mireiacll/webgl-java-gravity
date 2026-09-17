package core;

import static org.lwjgl.opengl.GL20.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ObstacleShader {
    private int program;
    private int positionLoc;
    private int texCoordLoc;
    private int textureLoc;
    private int colorLoc;

    public ObstacleShader() {
        program = buildProgram();
        positionLoc = glGetAttribLocation(program, "a_position");
        texCoordLoc = glGetAttribLocation(program, "a_texCoord");
        textureLoc = glGetUniformLocation(program, "u_texture");
        colorLoc = glGetUniformLocation(program, "u_color");
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
            System.err.println("Obstacle shader compilation failed: " + glGetShaderInfoLog(shader));
            return -1;
        }
        return shader;
    }

    private int buildProgram() {
        String vertexSource = loadResource("/shaders/screenVertex.glsl"); // reused, no changes needed
        String fragmentSource = loadResource("/shaders/obstacleFragment.glsl");

        int vertexShader = compile(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compile(GL_FRAGMENT_SHADER, fragmentSource);
        if (vertexShader == -1 || fragmentShader == -1) return -1;

        int prog = glCreateProgram();
        glAttachShader(prog, vertexShader);
        glAttachShader(prog, fragmentShader);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Obstacle program linking failed: " + glGetProgramInfoLog(prog));
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return prog;
    }

    public int getProgram() { return program; }
    public int getPositionLoc() { return positionLoc; }
    public int getTexCoordLoc() { return texCoordLoc; }
    public int getTextureLoc() { return textureLoc; }
    public int getColorLoc() { return colorLoc; }
}