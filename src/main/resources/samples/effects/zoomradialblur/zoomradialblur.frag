#version 330 core

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
    vec2 focus = gl_FragCoord.xy - scaledCenter;
    float calcStrength = strength * (1.0 / max(resolution.x, resolution.y)) * viewport.z;

    vec4 outColor = vec4(0.0, 0.0, 0.0, 0.0);

    for (int i = 0; i < blurSteps; i++) {
        float power = 1.0 - calcStrength * float(i) / float(blurSteps);
        outColor += texture(baseImg, focus * power + scaledCenter);
    }

    outColor *= 1.0 / float(blurSteps);
    fragColor = outColor;
}
