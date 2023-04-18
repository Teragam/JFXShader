static const int numLights = 3;

/*
 * The output of the vertex shader is the input of the pixel shader.
 */
typedef struct PsInput {
    float4 projPos : position;
    float3 worldVecToEye                 : texcoord2;
    float3 worldVecsToLights[numLights]  : texcoord3;
    float3 worldNormLightDirs[numLights] : texcoord6;
    float2 texD : texcoord0;
} VsOutput;

float4 main(PsInput psInput) : color {
    return float4(1.0, 1.0, 0.0, 1.0);
}
