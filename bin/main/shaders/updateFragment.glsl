#version 330 core

in vec2 v_texCoord;
out vec4 outColor;

uniform sampler2D u_particles; // current positions
uniform sampler2D u_field;     // vector field
uniform vec2 u_field_min;
uniform vec2 u_field_max;
uniform float u_dt;
uniform float u_time;          // accumulated (wrapped) seconds - seeds the pseudo-random
                                // respawn so it varies frame to frame, not just particle to particle
uniform float u_drop_rate;      // base per-frame chance any particle gets recycled
uniform float u_drop_rate_bump; // extra chance added for faster-moving particles

// no rand() in GLSL - standard hash: run the seed through sin() (chaotic /
// hard to predict) then take a huge multiple of it and keep only the
// fractional part, so the output looks uniformly scattered in [0, 1)
float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    // v_texCoord IS this particle's row/column - the update FBO is sized
    // exactly particlesRes x particlesRes, so one fragment = one particle
    vec4 posColor = texture(u_particles, v_texCoord);
    vec2 pos = vec2(
        posColor.r / 255.0 + posColor.b,
        posColor.g / 255.0 + posColor.a);

    vec2 velocity = mix(u_field_min, u_field_max, texture(u_field, pos).rg) * 0.1;
    vec2 newPos = fract(pos + velocity * u_dt); // fract() wraps particles back on-screen instead of losing them off the edge

    // Recycle a random subset of particles every frame, independent of their
    // own speed - this is the standard technique for these flow-field
    // visualizations (avoids degeneration: without it, particles keep
    // drifting into slow/stagnant regions of the field and pile up there,
    // while fast regions thin out and never get refreshed).
    float speedT = length(velocity) / length(u_field_max);
    float dropRate = u_drop_rate + speedT * u_drop_rate_bump;

    vec2 seed = v_texCoord + vec2(u_time);
    float drop = step(1.0 - dropRate, rand(seed));

    vec2 randomPos = vec2(rand(seed + vec2(17.17, 3.71)), rand(seed + vec2(3.71, 17.17)));
    newPos = mix(newPos, randomPos, drop);

    // same encoding pointVertex.glsl expects
    outColor = vec4(
        fract(newPos.x * 255.0),
        fract(newPos.y * 255.0),
        floor(newPos.x * 255.0) / 255.0,
        floor(newPos.y * 255.0) / 255.0);
}
