varying vec2 texCoord0;

uniform sampler2D baseImg;

void main() {
    gl_FragColor = texture2D(baseImg, texCoord0);
}
