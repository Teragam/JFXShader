varying vec2 texCoord0;

uniform sampler2D baseImg;
uniform vec2 center;
uniform int blurSteps;
uniform float strength;

void main() {
    vec2 focus = texCoord0 - center;
    vec4 outColor = vec4(0.0, 0.0, 0.0, 0.0);
    for (int i = 0; i < blurSteps; i++) {
        float power = 1.0 - strength * float(i) / float(blurSteps);
        vec2 texCoord = focus * power + center;
        outColor += texture2D(baseImg, texCoord);
    }
    outColor *= 1.0 / float(blurSteps);
    gl_FragColor = outColor;
}
