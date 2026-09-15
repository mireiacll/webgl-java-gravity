package core;

import static org.lwjgl.opengl.GL20.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Same job as ScreenShader (draws a texture full-screen) but the fragment shader
// also multiplies the sampled alpha down - this is what lets old trail pixels
// fade out exponentially instead of getting wiped every frame.
public class FadeShader {
    private int program;
    private int positionLoc;
    private int texCoordLoc;
    private int textureLoc;
    private int fadeLoc;

    public FadeShader() {
        program = buildProgram();
        positionLoc = glGetAttribLocation(program, "a_position");
        texCoordLoc = glGetAttribLocation(program, "a_texCoord");
        textureLoc = glGetUniformLocation(program, "u_texture");
        fadeLoc = glGetUniformLocation(program, "u_fade");
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
            System.err.println("Fade shader compilation failed: " + glGetShaderInfoLog(shader));
            return -1;
        }
        return shader;
    }

    private int buildProgram() {
        String vertexShaderSource = loadResource("/shaders/screenVertex.glsl"); // reused, no changes needed
        String fragmentShaderSource = loadResource("/shaders/fadeFragment.glsl");

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
            System.err.println("Fade program linking failed: " + glGetProgramInfoLog(prog));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return prog;
    }

    public int getProgram() { return program; }
    public int getPositionLoc() { return positionLoc; }
    public int getTexCoordLoc() { return texCoordLoc; }
    public int getTextureLoc() { return textureLoc; }
    public int getFadeLoc() { return fadeLoc; }
}