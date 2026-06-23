#version 330

// Standard MC GUI uniform blocks (must match the engine's bind-group layout exactly).
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

// Vertex attributes (names match our custom VertexFormat element names).
in vec3 Position;   // screen-space xy
in vec4 Color;      // fill colour (per-vertex -> allows a gradient)
in vec2 UV0;        // local pixel position relative to the rect centre
in ivec2 UV1;       // (halfWidth, halfHeight) in pixels
in ivec2 UV2;       // (cornerRadius, unused) in pixels

out vec4 vColor;
out vec2 vLocal;
out vec2 vHalf;
out float vRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vLocal = UV0;
    vHalf = vec2(UV1);
    vRadius = float(UV2.x);
}
