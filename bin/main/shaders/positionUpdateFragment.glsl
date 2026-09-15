#version 330 core

in vec2 v_texCoord;
out vec4 outColor;

uniform sampler2D u_position; // current positions, packed 8-bit encoding (same as before)
uniform sampler2D u_velocity; // this frame's updated velocity, raw (vx, vy) floats - GL_RG32F
uniform float u_dt;

void main() {
    // same decode as updateFragment.glsl: two 8-bit channels reassembled into
    // one float per axis (coarse integer part + fine fractional remainder)
    vec4 posColor = texture(u_position, v_texCoord);
    vec2 pos = vec2(
        posColor.r / 255.0 + posColor.b,
        posColor.g / 255.0 + posColor.a);

    // no decode needed here - GL_RG32F means this is already a real (vx, vy)
    vec2 velocity = texture(u_velocity, v_texCoord).rg;

    // velocity was already flipped away from any screen edge in the velocity, safety net
    vec2 newPos = clamp(pos + velocity * u_dt, 0.0, 1.0);

    // re-encode: same packing pointVertex.glsl expects
    outColor = vec4(
        fract(newPos.x * 255.0),
        fract(newPos.y * 255.0),
        floor(newPos.x * 255.0) / 255.0,
        floor(newPos.y * 255.0) / 255.0);
}
