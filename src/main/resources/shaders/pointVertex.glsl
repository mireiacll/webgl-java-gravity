#version 330 core

in float a_index;

uniform sampler2D u_particles;
uniform float u_particles_res;
uniform float u_pointSize;

void main() {
    vec4 color = texture(u_particles, vec2(
        fract(a_index / u_particles_res),
        floor(a_index / u_particles_res) / u_particles_res));

    vec2 pos = vec2(
        color.r / 255.0 + color.b,
        color.g / 255.0 + color.a);

    gl_PointSize = u_pointSize;
    gl_Position = vec4(2.0 * pos.x - 1.0, 1.0 - 2.0 * pos.y, 0.0, 1.0);
}
