package core;

// Ping-pongs the particle simulation's combined velocity+position MRT FBO.
// Position and velocity are always swapped together now, since one draw call
// writes both - replaces having two separate PingPongBuffers kept in lockstep.
public class ParticlePingPongBuffer {
    private final ParticleFBO bufferA;
    private final ParticleFBO bufferB;
    private boolean aIsCurrent = true;

    public ParticlePingPongBuffer(int width, int height) {
        bufferA = new ParticleFBO(width, height);
        bufferB = new ParticleFBO(width, height);
    }

    // last frame's finalized state - sample this as input in the update shader
    public ParticleFBO current() { return aIsCurrent ? bufferA : bufferB; }

    // this frame's target - bind and draw the next state into this
    public ParticleFBO next() { return aIsCurrent ? bufferB : bufferA; }

    // call after finishing the write into next() - current()/next() swap for the next frame
    public void swap() { aIsCurrent = !aIsCurrent; }
}