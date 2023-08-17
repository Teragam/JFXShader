uniform mat4 viewProjectionMatrix;
uniform mat4 worldMatrix;
uniform vec3 camPos;
uniform vec3 ambientColor;

attribute vec3 pos;
attribute vec2 texCoords;
attribute vec4 tangent;

varying vec2 oTexCoords;
varying vec3 eyePos;

void main() {
    vec3 t1 = tangent.xyz * tangent.yzx;
    t1 *= 2.0;
    vec3 t2 = tangent.zxy * tangent.www;
    t2 *= 2.0;
    vec3 t3 = tangent.xyz * tangent.xyz;
    t3 *= 2.0;
    vec3 t4 = 1.0-(t3+t3.yzx);

    vec3 r1 = t1 + t2;
    vec3 r2 = t1 - t2;

    mat3 sWorldMatrix = mat3(worldMatrix[0].xyz, worldMatrix[1].xyz, worldMatrix[2].xyz);
    eyePos = sWorldMatrix * vec3(t4.y, r1.x, r2.z);

    mat4 mvpMatrix = viewProjectionMatrix * worldMatrix;

    oTexCoords = texCoords;
    gl_Position = mvpMatrix * vec4(pos,1.0);
}
