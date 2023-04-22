varying vec2 oTexCoords;
varying vec3 eyePos;

void main() {
    vec3 n = vec3(0, 0, -1);
    vec3 d = 1.0 - vec3(clamp(dot(n, normalize(eyePos)), 0.0, 1.0));
    gl_FragColor = vec4(d * d, 1.0);
}
