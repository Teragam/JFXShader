static const int numLights = 3;

/*
 * The output of the vertex shader is the input of the pixel shader.
 */
typedef struct PsInput {
    float4 projPos : position; // must be outputed even if unused
    float2 texD : texcoord0;
    float4 normal : texcoord1;
} VsOutput;

// camera
float4x4 mViewProj   : register(c0);
float4   gCameraPos  : register(c4);

float4x3 mWorld            : register(c35);

struct VsInput {
    // model space = local space = object space
    float4  modelVertexPos    : position;
    float2  texD              : texcoord0;
    float4  modelVertexNormal : texcoord1;
};

VsOutput main(VertexType vsInput) {
    VsOutput vsOutput;

    vsOutput.texD = vsInput.texD;
    vsOutput.normal = vsInput.modelVertexNormal;
    float3 worldVertexPos = mul(vsInput.modelVertexPos, mWorld);
    vsOutput.projPos = mul(float4(worldVertexPos, 1), mViewProj);

    return vsOutput;
}
