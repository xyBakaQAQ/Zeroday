#version 330
#extension GL_ARB_separate_shader_objects : require

// Must match the engine's DynamicTransforms bind-group layout (26.3 field order).
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec4 vColor;
layout(location = 1) in vec2 vLocal;
layout(location = 2) in vec2 vHalf;
layout(location = 3) in float vRadius;

layout(location = 0) out vec4 fragColor;

// Signed distance to a rounded box centred at the origin.
float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    float dist = sdRoundBox(vLocal, vHalf, vRadius);
    // Adaptive anti-aliasing: ~1px feather based on the screen-space derivative.
    float aa = fwidth(dist) + 0.0001;
    float alpha = 1.0 - smoothstep(-aa, aa, dist);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha) * ColorModulator;
}
