uniform mat4 viewProjectionMatrix;
uniform mat4 worldMatrix;
uniform vec3 camPos;

attribute vec3 pos;
attribute vec2 texCoords;
attribute vec4 tangent;

varying vec2 oTexCoords;

void main() {
    vec4 worldPos = worldMatrix * vec4(pos, 1.0);
    mat3 sWorldMatrix = mat3(worldMatrix[0].xyz, worldMatrix[1].xyz, worldMatrix[2].xyz);
    mat4 mvpMatrix = viewProjectionMatrix * worldMatrix;

    oTexCoords = texCoords;
    gl_Position = mvpMatrix * vec4(pos, 1.0);
}
