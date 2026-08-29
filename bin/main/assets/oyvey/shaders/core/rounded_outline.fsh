#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 vUv;
flat in vec2 vHalfSize;
flat in float vRadius;
flat in float vThickness;
in vec4 vColor;

out vec4 fragColor;

float roundedBoxDistance(vec2 p, vec2 halfSize, float radius) {
    vec2 inner = max(halfSize - radius, 0.0);
    return length(max(abs(p) - inner, 0.0)) - radius;
}

void main() {
    float dist = roundedBoxDistance(vUv, vHalfSize, vRadius);
    float aa = max(fwidth(dist), 1e-5);
    float outer = clamp(0.5 - dist / aa, 0.0, 1.0);
    float inner = clamp(0.5 - (dist + vThickness) / aa, 0.0, 1.0);
    float alpha = outer - inner;
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha) * ColorModulator;
}
