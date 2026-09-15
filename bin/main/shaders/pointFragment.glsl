#version 330 core

//uniform sampler2D u_field;
//uniform vec2 u_field_min;
//uniform vec2 u_field_max;
//uniform sampler2D u_color_ramp;

//in vec2 v_particle_pos;
out vec4 outColor;

void main() {
    //vec2 value = mix(u_field_min, u_field_max, texture(u_field, v_particle_pos).rg);
    //float magnitude_t = length(value) / length(u_field_max);

    //vec2 ramp_pos = vec2(
    //    fract(16.0 * magnitude_t),
     //   floor(16.0 * magnitude_t) / 16.0);

    //outColor = texture(u_color_ramp, ramp_pos);

    // circle shape point: (0.5, 0.5) is the center, dist > 0.5 is outside the
    // fade alpha out over a ~1-pixel band around the edge so the
    // boundary blends smoothly instead of aliasing into a jagged circle.
    vec2 coord = gl_PointCoord - vec2(0.5);
    float dist = length(coord);
    float edge = fwidth(dist);
    float alpha = 1.0 - smoothstep(0.5 - edge, 0.5 + edge, dist);

    if (alpha <= 0.0) {
        discard;
    }

    outColor = vec4(1.0, 1.0, 1.0, alpha);
}
