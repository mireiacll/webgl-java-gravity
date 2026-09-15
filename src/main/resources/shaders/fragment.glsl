#version 330 core
in vec4 v_color;
in vec2 v_texCoord;
in vec3 v_normal;
out vec4 outColor;

uniform sampler2D u_texture;
uniform float u_useTexture;
uniform vec3 u_lightDir;
uniform float u_ambientStrength;

void main() {
    vec4 texColor = texture(u_texture, v_texCoord);
    vec4 baseColor = mix(v_color, texColor, u_useTexture);

    vec3 normal = normalize(v_normal);
    if (!gl_FrontFacing) {
        normal = -normal;
    }
    vec3 lightDir = normalize(u_lightDir);
    float diffuse = max(dot(normal, -lightDir), 0.0);
    float lighting = u_ambientStrength + (1.0 - u_ambientStrength) * diffuse;

    outColor = vec4(baseColor.rgb * lighting, baseColor.a);
}