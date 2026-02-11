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
uniform vec2 pixCoordOffset;


float roundRect(vec2 p, vec4 box, float radius, float feather) {
    vec2 size = box.zw - box.xy;
    radius = min(radius, min(size.x, size.y) / 2.0);
    vec2 q = abs(p - box.xy - size / 2.0) - size / 2.0 + radius;
    float distance = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    return 1.0 - smoothstep(min(feather, 0.0), max(feather, 0.0), distance);
}

void main() {
    vec2 pixcoord = vec2(gl_FragCoord.x - jsl_pixCoordOffset.x, ((jsl_pixCoordOffset.z - gl_FragCoord.y) * jsl_pixCoordOffset.w) - jsl_pixCoordOffset.y);
    pixcoord += pixCoordOffset;
    vec4 bot = texture2D(botImg, texCoord0);
    vec4 top = texture2D(topImg, texCoord1);
    float factor = 0.0;
    for (int i = 0; i < count; i++) {
        // The scale is used to compensate for dpi scaling
        factor += roundRect(pixcoord / scale, rects[i], ops[i].x, ops[i].y) * ops[i].z;
    }
    factor = clamp(factor, 0.0, 1.0);
    if (invertMask == 1) {
        factor = 1.0 - factor;
    }
    gl_FragColor = bot * factor + top * (1.0 - factor);
}
