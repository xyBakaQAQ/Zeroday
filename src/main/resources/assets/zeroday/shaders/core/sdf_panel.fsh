#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vColor;
in vec2 vLocal;
in vec2 vHalf;
in float vRadius;

out vec4 fragColor;

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
