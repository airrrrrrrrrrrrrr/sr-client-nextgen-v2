#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 vUv;
flat in vec2 vHalfSize;
flat in float vRadius;
flat in float vThickness;
in vec4 vColor;

out vec4 fragColor;

float roundedBoxDistance(vec2 p) {
    vec2 q = abs(p) - (vHalfSize - vRadius);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - vRadius;
}

float coverage(vec2 p, float aa) {
    float dist = roundedBoxDistance(p);
    float outer = clamp(0.5 - dist / aa, 0.0, 1.0);
    float inner = clamp(0.5 - (dist + vThickness) / aa, 0.0, 1.0);
    return max(outer - inner, 0.0);
}

void main() {
    float dist = roundedBoxDistance(vUv);
    float aa = max(length(vec2(dFdx(dist), dFdy(dist))) * 0.5, 1e-5);
    vec2 dx = dFdx(vUv) * 0.25, dy = dFdy(vUv) * 0.25;
    float alpha = (coverage(vUv - dx - dy, aa) + coverage(vUv + dx - dy, aa)
        + coverage(vUv - dx + dy, aa) + coverage(vUv + dx + dy, aa)) * 0.25;
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha) * ColorModulator;
}
