#version 330 core
precision highp float;

in vec2 v_texCoord;
uniform sampler2D u_texture;
uniform float u_fade; // fraction of alpha kept each frame - 0.5 = halves every frame (1, 0.5, 0.25, 0.125...)

out vec4 outColor;

void main() {
    vec4 previous = texture(u_texture, v_texCoord);
    float newAlpha = previous.a * 0.99f;
    vec3 finalColor = previous.rgb;
    if (newAlpha < 0.2f) {
        newAlpha = 0.0f;
        //finalColor = vec3(1.0, 0.0, 0.0);
    } 
    /*
    else if(newAlpha >= 0.01f && newAlpha < 0.2f)
    {
        finalColor = vec3(0.0, 1.0, 0.0);
    }
    else if(newAlpha >= 0.2f && newAlpha < 0.4f)
    {
        finalColor = vec3(0.0, 0.0, 1.0);
    }
    else if(newAlpha >= 0.4f && newAlpha < 0.6f)
    {
        finalColor = vec3(1.0, 1.0, 0.0);
    }
    else if(newAlpha >= 0.6f && newAlpha < 0.8f)
    {
        finalColor = vec3(1.0, 0.0, 1.0);
    }
    else if(newAlpha >= 0.8f)
    {
        finalColor = vec3(0.0, 1.0, 1.0);
    } else {
        finalColor = vec3(0.5, 0.5, 0.5);
    }
    */
    outColor = vec4(finalColor, newAlpha);
}
