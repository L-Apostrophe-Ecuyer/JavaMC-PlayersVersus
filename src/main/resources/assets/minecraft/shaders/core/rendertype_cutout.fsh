#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    // Adaptive Brightness
    float adaptiveBrightnessMultiplier = (vertexDistance + 10.0) / (vertexDistance + 2.0 + vertexColor[1] * 6.0) + (vertexColor[1] - 0.6)/2.0;
    color *= vertexColor * adaptiveBrightnessMultiplier;

    // Atmosphere
    float atmosphereAmount = (vertexDistance * vertexDistance) / ((color[0] + color[1] + color[2] + 2.0) * (FogEnd * FogEnd));
    color[0] += (0.9 * FogColor[0] - color[0] + (vertexColor[1] - 0.4)/3.0) * atmosphereAmount;
    color[1] += (1.0 * FogColor[1] - color[1] + (vertexColor[2] - 0.4)/3.0) * atmosphereAmount;
    color[2] += (1.1 * FogColor[2] - color[2] + (vertexColor[3] - 0.4)/3.0) * atmosphereAmount;
    fragColor = linear_fog(color, vertexDistance, (FogStart + FogEnd) / 2.0, FogEnd, FogColor);
}
