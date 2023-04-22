struct PixelInput {
    float4 pos : SV_POSITION;
    float2 oTexCoords : TEXCOORD0;
    float3 eyePos : TEXCOORD1;
};

float4 main(PixelInput input) : SV_Target {
    float3 n = float3(0, 0, -1);
    float d = 1.0 - clamp(dot(n, normalize(input.eyePos)), 0.0, 1.0);
    d = d * d;
    return float4(d, d, d, 1.0);
}
