#version 410
#ifdef GL_ES
#extension GL_OES_standard_derivatives: enable
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
precision highp int;
#else
precision mediump float;
precision mediump int;
#endif
#else
#define highp
#define mediump
#define lowp
#endif
varying vec2 texCoord0;
varying vec2 texCoord1;
uniform vec4 jsl_pixCoordOffset;
uniform sampler2D botImg;
uniform sampler2D topImg;
uniform int count;
uniform vec4 rects[8];
uniform vec4 ops[8];
uniform float scale;
uniform int invertMask;

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

float insideRect(vec2 p, vec4 rect, vec4 ops) {
    float dx = max(rect.x - p.x, max(p.x - rect.z, 0.0));
    float dy = max(rect.y - p.y, max(p.y - rect.w, 0.0));
    float d = sqrt(dx * dx + dy * dy);
    if (ops.x == 0.0) {
        return 1.0 - clamp(ceil(d), 0.0, 1.0);
    } else {
        return map(clamp(d / ops.x, 0.0, 1.0), ops.y, 1.0, 1.0, 0.0);
    }
}

void main() {
    vec2 pixcoord = vec2(gl_FragCoord.x - jsl_pixCoordOffset.x, ((jsl_pixCoordOffset.z - gl_FragCoord.y) * jsl_pixCoordOffset.w) - jsl_pixCoordOffset.y);

    vec4 bot = texture2D(botImg, texCoord0);
    vec4 top = texture2D(topImg, texCoord1);
    float factor = 0.0;
    for (int i = 0; i < count; i++) {
        // The scale is used to compensate for dpi scaling
        factor += insideRect(pixcoord * (1.0 / scale), rects[i], ops[i]) * ops[i].z;
    }
    factor = clamp(factor, 0.0, 1.0);
    if (invertMask == 1) {
        factor = 1.0 - factor;
    }
    gl_FragColor = bot * factor + top * (1.0 - factor);
}
