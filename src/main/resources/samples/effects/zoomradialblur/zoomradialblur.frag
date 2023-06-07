#version 330 core

varying vec2 texCoord0;

uniform sampler2D baseImg;
uniform vec2 resolution;
uniform vec2 center;
uniform vec4 viewport;
uniform vec4 texCoords;
uniform int blurSteps;
uniform float strength;

out vec4 fragColor;

void main() {
    vec2 scaledCenter = ((center / resolution + texCoords.xy) * texCoords.zw) * viewport.zw;
    vec2 focus = texCoord0 - scaledCenter;
    float calcStrength = strength * (1.0 / max(resolution.x, resolution.y)) * viewport.z;

    vec4 outColor = vec4(0.0, 0.0, 0.0, 0.0);

    for (int i = 0; i < blurSteps; i++) {
        float power = 1.0 - calcStrength * float(i) / float(blurSteps);
        vec2 texCoord = focus * power + scaledCenter;
        if (texCoord.x < texCoords.x || texCoord.y < texCoords.y || texCoord.x > texCoords.z || texCoord.y > texCoords.w) {
            break;
        }
        outColor += texture(baseImg, texCoord);
    }

    outColor *= 1.0 / float(blurSteps);
    fragColor = outColor;
}
