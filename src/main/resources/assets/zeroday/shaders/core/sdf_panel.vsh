#version 330
#extension GL_ARB_separate_shader_objects : require

// Standard MC GUI uniform blocks (must match the engine's bind-group layout exactly).
// 26.3 reordered DynamicTransforms: TextureMat now precedes ColorModulator/ModelOffset.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

// Vertex attributes (locations follow our custom VertexFormat's element order).
layout(location = 0) in vec3 Position;  // screen-space xy
layout(location = 1) in vec4 Color;     // fill colour (per-vertex -> allows a gradient)
layout(location = 2) in vec2 UV0;       // local pixel position relative to the rect centre
layout(location = 3) in ivec2 UV1;      // (halfWidth, halfHeight) in pixels
layout(location = 4) in ivec2 UV2;      // (cornerRadius, unused) in pixels

layout(location = 0) out vec4 vColor;
layout(location = 1) out vec2 vLocal;
layout(location = 2) out vec2 vHalf;
layout(location = 3) out float vRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vLocal = UV0;
    vHalf = vec2(UV1);
    vRadius = float(UV2.x);
}
