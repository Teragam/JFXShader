varying vec2 texCoord0;

uniform sampler2D baseImg;
uniform vec2 pixelSize;
uniform vec2 resolution;
uniform vec4 viewport;

void main() {
    vec2 normalizedTexCoord = (texCoord0 + viewport.xy) / viewport.zw;
    normalizedTexCoord = floor(normalizedTexCoord / pixelSize) * pixelSize;
    normalizedTexCoord += pixelSize * 0.5; // Center the pixel
    gl_FragColor = texture2D(baseImg, normalizedTexCoord * viewport.zw - viewport.xy);
}
