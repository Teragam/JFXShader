typedef struct PsInput {
    float4 projPos : position;
    float2 texD : texcoord0;
    float3 normal : texcoord1;
} VsOutput;

float4 main(PsInput psInput) : color {
    float3 normalVec = normalize(psInput.normal);
    float3 coloredNormal = normalVec * 0.5 + 0.5;
    return float4(coloredNormal.r, coloredNormal.g, coloredNormal.b, 1.0);
}
