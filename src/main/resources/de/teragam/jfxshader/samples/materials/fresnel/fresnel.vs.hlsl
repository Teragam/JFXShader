float4x4 viewProjectionMatrix : register(c0);
float4   camPos               : register(c4);
float4x3 worldMatrix          : register(c35);

struct VertexInput {
    float4  pos               : POSITION;
    float2  texCoords         : TEXCOORD0;
    float4  modelVertexNormal : TEXCOORD1;
};

struct VertexOutput {
    float4 pos : SV_POSITION;
    float2 oTexCoords : TEXCOORD0;
    float3 eyePos : TEXCOORD1;
};

void quatToMatrix(float4 q, out float3 N[3]) {
    float3 t1 = q.xyz * q.yzx * 2;
    float3 t2 = q.zxy * q.www * 2;
    float3 t3 = q.xyz * q.xyz * 2;
    float3 t4 = 1 - (t3 + t3.yzx);

    float3 r1 = t1 + t2;
    float3 r2 = t1 - t2;

    N[0] = float3(t4.y, r1.x, r2.z);
    N[1] = float3(r2.x, t4.z, r1.y);
    N[2] = float3(r1.z, r2.y, t4.x);
    N[2] *= (q.w >= 0) ? 1 : -1;
}

VertexOutput main(VertexInput input) {
    VertexOutput output;

    float3 n[3];
    quatToMatrix(input.modelVertexNormal, n);
    for (int i = 0; i != 3; ++i) {
        n[i] = mul(n[i], (float3x3) worldMatrix);
    }
    output.eyePos = n[0];

    float3 worldVertexPos = mul(input.pos, worldMatrix);

    output.oTexCoords = input.texCoords;
    output.pos = mul(float4(worldVertexPos, 1.0), viewProjectionMatrix);

    return output;
}
