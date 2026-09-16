#version 330 core

in vec2 v_texCoord;

// MRT: attachment 0 = velocity (GL_RG32F, raw floats), attachment 1 = position
// (GL_RGBA8, packed 8-bit encoding) - see ParticleFBO. One draw call writes both.
layout(location = 0) out vec4 outVelocity;
layout(location = 1) out vec4 outPosition;

uniform sampler2D u_velocity; // meters/second - GL_RG32F, no packing
uniform sampler2D u_position; // packed 8-bit encoding of a normalized [0,1] screen position
uniform vec2 u_gravity;       // meters/second^2 - now a real physical value, e.g. (0.0, 9.8)
uniform vec2 u_worldSize;     // scene size in meters: (worldWidth, worldHeight)
uniform float u_dt;
uniform float u_damping;        // 1.0 = no damping; <1.0 gradually bleeds off speed (e.g. drag)
uniform float u_edgeElasticity; // 1.0 = perfectly elastic screen-edge rebound; <1.0 loses rebound energy

void main() {
    // decode current position 
    vec4 posColor = texture(u_position, v_texCoord);
    vec2 posUV = vec2(
        posColor.r / 255.0 + posColor.b,
        posColor.g / 255.0 + posColor.a);

    vec2 pos = posUV * u_worldSize;

    // velocity update
    vec2 velocity = texture(u_velocity, v_texCoord).rg;

    velocity += u_gravity * u_dt;
    velocity *= u_damping;

    // rebound: predict where this velocity would move the particle this frame,
    // flip whichever axis would carry it past a screen edge, keep only a
    // fraction of that axis's velocity so the impact loses energy.
    vec2 predicted = pos + velocity * u_dt;
    if (predicted.x < 0.0 || predicted.x > u_worldSize.x) velocity.x = -velocity.x * u_edgeElasticity;
    if (predicted.y < 0.0 || predicted.y > u_worldSize.y) velocity.y = -velocity.y * u_edgeElasticity;

    // position update
    vec2 newPos = clamp(pos + velocity * u_dt, vec2(0.0), u_worldSize);
    vec2 newPosUV = newPos / u_worldSize;

    outVelocity = vec4(velocity, 0.0, 1.0);

    // re-encode: same packing pointVertex.glsl expects
    outPosition = vec4(
        fract(newPosUV.x * 255.0),
        fract(newPosUV.y * 255.0),
        floor(newPosUV.x * 255.0) / 255.0,
        floor(newPosUV.y * 255.0) / 255.0);
}
