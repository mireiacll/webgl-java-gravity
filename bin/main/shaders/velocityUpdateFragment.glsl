#version 330 core

in vec2 v_texCoord;
out vec4 outColor;

uniform sampler2D u_velocity; // current velocity, raw (vx, vy) floats - GL_RG32F, no packing
uniform sampler2D u_position; // last frame's finalized position - packed 8-bit encoding, same as before
uniform vec2 u_gravity;       // constant acceleration, applied the same to every particle
uniform float u_dt;
uniform float u_damping;      // 1.0 = no damping; <1.0 gradually bleeds off speed (e.g. drag)
uniform float u_edgeElasticity; // 1.0 = perfectly elastic screen-edge rebound; <1.0 loses rebound energy

void main() {
    // v_texCoord is this particle's row/column, same indexing as the position pass -
    // one fragment = one particle's velocity cell.
    vec2 velocity = texture(u_velocity, v_texCoord).rg;

    velocity += u_gravity * u_dt;
    velocity *= u_damping;

    // rebounds: decode the position this velocity is about to move, predict where it would land
    // this frame, and flip whichever axis would carry it past a screen edge. The rebound
    // keeps only a fraction of the wall-normal velocity so the impact loses energy.
    vec4 posColor = texture(u_position, v_texCoord);
    vec2 pos = vec2(
        posColor.r / 255.0 + posColor.b,
        posColor.g / 255.0 + posColor.a);

    vec2 predicted = pos + velocity * u_dt;
    if (predicted.x < 0.0 || predicted.x > 1.0) velocity.x = -velocity.x * u_edgeElasticity;
    if (predicted.y < 0.0 || predicted.y > 1.0) velocity.y = -velocity.y * u_edgeElasticity;

    // storage format is GL_RG - only .rg is actually written to the texture,
    // .b/.a here are unused placeholders
    outColor = vec4(velocity, 0.0, 1.0);
}
