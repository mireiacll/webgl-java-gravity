#version 330 core

in vec2 v_texCoord;

// MRT: attachment 0 = velocity (GL_RG32F, raw floats), attachment 1 = position
// (GL_RGBA8, packed 8-bit encoding) - see ParticleFBO. One draw call writes both.
layout(location = 0) out vec4 outVelocity;
layout(location = 1) out vec4 outPosition;

uniform sampler2D u_velocity; // meters/second - GL_RG32F, no packing
uniform sampler2D u_position; // packed 8-bit encoding of a normalized [0,1] screen position
uniform sampler2D u_obstacles;      // black = solid, white = open - drawn in Paint
uniform vec2 u_obstacleTexel;       // 1/width, 1/height of the obstacle image
uniform vec4 u_obstacleBoundsUV;    // xMin, yMin, xMax, yMax - bounding box of the whole drawing, in UV space
uniform vec2 u_gravity;       // meters/second^2
uniform vec2 u_worldSize;     // scene size in meters: (worldWidth, worldHeight)
uniform float u_dt;
uniform float u_damping;            // 1.0 = no damping; <1.0 gradually bleeds off speed (e.g. drag)
uniform float u_edgeElasticity;     // 1.0 = perfectly elastic screen-edge rebound; <1.0 loses rebound energy
uniform float u_obstacleElasticity; // same idea as edge elasticity, own knob for wall bounciness

// samples taken along pos->predicted when checking for an obstacle hit - raise
// this if fast particles still tunnel through thin drawn lines
const int OBSTACLE_STEPS = 8;

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

    // edge check may have just changed velocity - recompute predicted so the
    // obstacle march below checks the path we're actually about to take
    predicted = pos + velocity * u_dt;

    vec2 newPos = clamp(predicted, vec2(0.0), u_worldSize);

    // cheap reject: does this particle's movement box (pos->predicted)
    vec2 predictedUV = predicted / u_worldSize;
    bool mayHitObstacle =
        !(max(posUV.x, predictedUV.x) < u_obstacleBoundsUV.x ||
          min(posUV.x, predictedUV.x) > u_obstacleBoundsUV.z ||
          max(posUV.y, predictedUV.y) < u_obstacleBoundsUV.y ||
          min(posUV.y, predictedUV.y) > u_obstacleBoundsUV.w);

    if (mayHitObstacle) {
        // walk the line from pos to predicted in small hops instead of only
        // checking the endpoint - a fast particle can otherwise jump clean
        // over a thin drawn line without ever landing on it
        for (int i = 1; i <= OBSTACLE_STEPS; i++) {
            float t = float(i) / float(OBSTACLE_STEPS);
            vec2 sampleUV = clamp(mix(pos, predicted, t) / u_worldSize, vec2(0.0), vec2(1.0));
            float hit = texture(u_obstacles, sampleUV).r;

            if (hit < 0.5) {
                float tl = texture(u_obstacles, sampleUV + vec2(-u_obstacleTexel.x,  u_obstacleTexel.y)).r;
                float t  = texture(u_obstacles, sampleUV + vec2( 0.0,                u_obstacleTexel.y)).r;
                float tr = texture(u_obstacles, sampleUV + vec2( u_obstacleTexel.x,  u_obstacleTexel.y)).r;
                float l  = texture(u_obstacles, sampleUV + vec2(-u_obstacleTexel.x,  0.0)).r;
                float r  = texture(u_obstacles, sampleUV + vec2( u_obstacleTexel.x,  0.0)).r;
                float bl = texture(u_obstacles, sampleUV + vec2(-u_obstacleTexel.x, -u_obstacleTexel.y)).r;
                float b  = texture(u_obstacles, sampleUV + vec2( 0.0,               -u_obstacleTexel.y)).r;
                float br = texture(u_obstacles, sampleUV + vec2( u_obstacleTexel.x, -u_obstacleTexel.y)).r;

                float gx = (tr + 2.0*r + br) - (tl + 2.0*l + bl);
                float gy = (tl + 2.0*t + tr) - (bl + 2.0*b + br);
                vec2 normal = vec2(gx, gy);
                normal = length(normal) > 0.0001 ? normalize(normal) : -normalize(velocity);
                
                velocity = reflect(velocity, normal) * u_obstacleElasticity;
                //float vn = dot(velocity, normal);
                //vec2 velocityNormal = vn * normal;
                //vec2 velocityTangent = velocity - velocityNormal;

                //const float obstacleFriction = 0.85; // tune: lower = stops sliding faster
                //velocity = velocityTangent * obstacleFriction - velocityNormal * u_obstacleElasticity;

                // kill remaining jitter once basically at rest
                //if (length(velocity) < 0.01) velocity = vec2(0.0);


                // stop at the last step that WASN'T inside the obstacle, not at predicted 
                newPos = mix(pos, predicted, float(i - 1) / float(OBSTACLE_STEPS));
                //newPos += normal * 0.5; // small epsilon push in world units
                vec2 obstacleTexelWorld = u_obstacleTexel * u_worldSize; // texel size in world units
                newPos += normal * max(u_worldSize.x, u_worldSize.y) * 0.0005;
                break;
            }
        }
    }

    // position update
    newPos = clamp(newPos, vec2(0.0), u_worldSize);
    vec2 newPosUV = newPos / u_worldSize;

    outVelocity = vec4(velocity, 0.0, 1.0);

    // re-encode: same packing pointVertex.glsl expects
    outPosition = vec4(
        fract(newPosUV.x * 255.0),
        fract(newPosUV.y * 255.0),
        floor(newPosUV.x * 255.0) / 255.0,
        floor(newPosUV.y * 255.0) / 255.0);
}