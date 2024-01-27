varying vec2 oTexCoords;
varying vec3 eyePos;

uniform sampler2D diffuseImage;
uniform vec4 color;
uniform float strength;

void main() {
    vec3 n = vec3(0, 0, -1);
    float d = 1.0 - clamp(dot(n, normalize(eyePos)), 0.0, 1.0);
    d = pow(d, strength);
    vec4 tex = texture2D(diffuseImage, oTexCoords);
    gl_FragColor = tex * (1.0 - d) + color * d;
}
