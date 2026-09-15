package core;

// Alternates between two render targets, frame to frame
// standard GPU behind cellular automata 
// You can't read a texture and write to that same texture in one draw call, so you keep two: 
// current() is last frame's state (read from it), 
// next() is where this frame's update gets written to
public class PingPongBuffer {
    private MireiaFBO bufferA;
    private MireiaFBO bufferB;
    private boolean aIsCurrent = true;

    public PingPongBuffer(int width, int height) {
        bufferA = new MireiaFBO(width, height);
        bufferB = new MireiaFBO(width, height);
    }

    // e.g. new PingPongBuffer(res, res, GL_NEAREST, GL_NEAREST, GL_RG32F, GL_RG, GL_FLOAT)
    // for a velocity buffer storing raw signed floats instead of packed 8-bit color.
    public PingPongBuffer(int width, int height, int gl_tex_min_filter, int gl_tex_mag_filter,
                           int internalFormat, int format, int type) {
        bufferA = new MireiaFBO(width, height, gl_tex_min_filter, gl_tex_mag_filter, internalFormat, format, type);
        bufferB = new MireiaFBO(width, height, gl_tex_min_filter, gl_tex_mag_filter, internalFormat, format, type);
    }

    // last frame's finished state - sample this as input in your update shader
    public MireiaFBO current() { return aIsCurrent ? bufferA : bufferB; }

    // this frame's target - bind and draw the next state into this
    public MireiaFBO next() { return aIsCurrent ? bufferB : bufferA; }

    // call after finishing the write into next() - current()/next() swap for the next frame
    public void swap() { aIsCurrent = !aIsCurrent; }

    public void setSize(int width, int height) {
        bufferA.setSize(width, height);
        bufferB.setSize(width, height);
    }
}