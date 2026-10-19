#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in ivec2 UV2;
in vec4 Color;

out vec2 vUv;
flat out vec2 vHalfSize;
flat out float vRadius;
flat out int vCornerMask;
out vec4 vColor;

void main() {
    // Z carries the local AA fringe, never GUI depth.
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    vUv = UV0;
    vColor = Color;
    vHalfSize = max(abs(UV0) - Position.z, 0.0);
    vRadius = float(UV2.x) / 32767.0 * min(vHalfSize.x, vHalfSize.y);
    vCornerMask = UV2.y < 0 ? 1 : 0;
}
