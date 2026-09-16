#version 330 core

in vec2 v_texCoord;

// attachment 0 = velocity (GL_RG32F, raw floats)
// attachment 1 = position (GL_RGBA8, packed 8-bit encoding) 
layout(location = 0) out vec4 outVelocity;
layout(location = 1) out vec4 outPosition;

uniform sampler2D u_velocity; // current velocity, raw (vx, vy) floats - GL_RG32F, no packing
uniform sampler2D u_position; // current position, packed 8-bit encoding
uniform vec2 u_gravity;       // constant acceleration, applied the same to every particle
uniform float u_dt;
uniform float u_damping;        // 1.0 = no damping; <1.0 gradually bleeds off speed (e.g. drag)
uniform float u_edgeElasticity; // 1.0 = perfectly elastic screen-edge rebound; <1.0 loses rebound energy

void main() {
    // decode current position 
    vec4 posColor = texture(u_position, v_texCoord);
    vec2 pos = vec2(
        posColor.r / 255.0 + posColor.b,
        posColor.g / 255.0 + posColor.a);

    // velocity update 
    vec2 velocity = texture(u_velocity, v_texCoord).rg;

    velocity += u_gravity * u_dt;
    velocity *= u_damping;

    // rebound: predict where this velocity would move the particle this frame,
    // flip whichever axis would carry it past a screen edge, keep only a
    // fraction of that axis's velocity so the impact loses energy.
    vec2 predicted = pos + velocity * u_dt;
    if (predicted.x < 0.0 || predicted.x > 1.0) velocity.x = -velocity.x * u_edgeElasticity;
    if (predicted.y < 0.0 || predicted.y > 1.0) velocity.y = -velocity.y * u_edgeElasticity;

    // position update
    vec2 newPos = clamp(pos + velocity * u_dt, 0.0, 1.0);

    // storage is GL_RG
    outVelocity = vec4(velocity, 0.0, 1.0);

    // re-encode: same packing pointVertex.glsl expects
    outPosition = vec4(
        fract(newPos.x * 255.0),
        fract(newPos.y * 255.0),
        floor(newPos.x * 255.0) / 255.0,
        floor(newPos.y * 255.0) / 255.0);
}
