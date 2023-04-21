varying vec2 oTexCoords;

void main() {
    gl_FragColor = vec4(oTexCoords.x, oTexCoords.y, 0.0, 1.0);
}
