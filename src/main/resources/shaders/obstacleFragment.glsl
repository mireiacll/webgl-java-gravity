#version 330 core

in vec2 v_texCoord;
uniform sampler2D u_texture;
uniform vec3 u_color; // tint for obstacle pixels

out vec4 outColor;

void main() {
    // flip V: the obstacle texture is stored top-down (flip=false, for the
    // physics math), but this full-screen quad expects bottom-up like trailFBO 
    vec2 uv = vec2(v_texCoord.x, 1.0 - v_texCoord.y);
    vec4 mask = texture(u_texture, uv);

    // black in the drawing = obstacle = visible; white = open = fully transparent
    float luminance = (mask.r + mask.g + mask.b) / 3.0;
    outColor = vec4(u_color, 1.0 - luminance);
}