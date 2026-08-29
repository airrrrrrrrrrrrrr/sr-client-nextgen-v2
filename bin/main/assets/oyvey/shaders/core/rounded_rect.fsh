#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 vUv;
flat in vec2 vHalfExtent;
in vec4 vColor;

out vec4 fragColor;

void main() {
    float dist = length(max(abs(vUv) - vHalfExtent, 0.0)) - 1.0;
    float aa = max(fwidth(dist), 1e-5);
    float alpha = clamp(0.5 - dist / aa, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor.rgb, vColor.a * alpha) * ColorModulator;
}
