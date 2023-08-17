#version 330 core

varying vec2 texCoord0;

uniform sampler2D baseImg;
uniform vec2 pixelSize;
uniform vec2 offset;
uniform vec2 resolution;
uniform vec4 texCoords;
uniform vec4 viewport;

out vec4 fragColor;

void main() {
    vec2 scaledPixelSize = (pixelSize / resolution) * viewport.zw;
    vec2 scaledOffset = (offset / resolution) * viewport.zw;
    vec2 pixelCoord = floor((texCoord0 + scaledOffset) / scaledPixelSize) * scaledPixelSize;
    vec2 clampedTexCoord = clamp(pixelCoord + scaledPixelSize * 0.5 + texCoords.xy, 1.0 / resolution, texCoords.zw - 1.0 / resolution);
    fragColor = texture(baseImg, clampedTexCoord);
}
