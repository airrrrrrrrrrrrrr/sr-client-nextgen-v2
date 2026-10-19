#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 vUv;
flat in vec2 vHalfSize;
flat in float vRadius;
flat in int vCornerMask;
in vec4 vColor;

out vec4 fragColor;

float distanceToShape(vec2 p) {
    if (vCornerMask != 0) return 1.0 - length(p);
    vec2 q = abs(p) - (vHalfSize - vRadius);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - vRadius;
}

void main() {
    float dist = distanceToShape(vUv);
    // Screen derivatives and four subpixel samples smooth small curves,
    // fractional borders and moving edges at every GUI scale.
    float aa = max(length(vec2(dFdx(dist), dFdy(dist))) * 0.5, 1e-5);
    vec2 dx = dFdx(vUv) * 0.25, dy = dFdy(vUv) * 0.25;
    float alpha = (
        clamp(0.5 - distanceToShape(vUv - dx - dy) / aa, 0.0, 1.0) +
        clamp(0.5 - distanceToShape(vUv + dx - dy) / aa, 0.0, 1.0) +
        clamp(0.5 - distanceToShape(vUv - dx + dy) / aa, 0.0, 1.0) +
        clamp(0.5 - distanceToShape(vUv + dx + dy) / aa, 0.0, 1.0)
    ) * 0.25;
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha) * ColorModulator;
}
