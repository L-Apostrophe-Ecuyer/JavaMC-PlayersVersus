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

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    // Adaptive Brightness
    float adaptiveBrightnessMultiplier = 1.0 / (vertexDistance + 2 * vertexColor[1]);
    for(int i = 0; i < 3; i++){
        color[i] += (0.05 + color[i]) * adaptiveBrightnessMultiplier;
    }


    // Atmosphere
    float atmosphereAmount = (vertexDistance * vertexDistance) / ((color[0] + color[1] + color[2] + 1.0) * (FogEnd * FogEnd));
    color[0] += (0.8 * FogColor[0] - color[0]) * atmosphereAmount;
    color[1] += (1.0 * FogColor[1] - color[1]) * atmosphereAmount;
    color[2] += (1.3 * FogColor[2] - color[2]) * atmosphereAmount;
    fragColor = linear_fog(color, vertexDistance, (FogStart + FogEnd) / 2.0, FogEnd, FogColor);
}
