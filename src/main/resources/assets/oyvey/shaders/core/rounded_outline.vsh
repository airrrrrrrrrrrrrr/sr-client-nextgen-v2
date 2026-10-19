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
flat out float vThickness;
out vec4 vColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    vUv = UV0;
    vHalfSize = max(abs(UV0) - Position.z, 0.0);
    vColor = Color;
    float halfShort = min(vHalfSize.x, vHalfSize.y);
    vRadius = float(UV2.x) / 32767.0 * halfShort;
    vThickness = float(UV2.y) / 32767.0 * halfShort;
}
