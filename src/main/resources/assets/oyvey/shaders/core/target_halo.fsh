#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    // Blur the actual reticle silhouette, retaining its crisp core. The input
    // has a transparent margin, so the halo fits without clipping the quad.
    vec2 stepUv = 3.0 / vec2(textureSize(Sampler0, 0));
    float halo = 0.0;
    float weightSum = 0.0;
    for (int y = -3; y <= 3; ++y) {
        for (int x = -3; x <= 3; ++x) {
            float weight = exp(-float(x*x + y*y) / 4.5);
            halo += texture(Sampler0, texCoord0 + vec2(x,y) * stepUv).a * weight;
            weightSum += weight;
        }
    }
    float core = texture(Sampler0, texCoord0).a;
    float alpha = min(1.0, core + 2.4 * halo / weightSum);
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
