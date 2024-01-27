sampler diffuseImage : register(s0);
float4 color : register(c0);
float strength : register(c1);

struct PixelInput {
    float4 pos : SV_POSITION;
    float2 oTexCoords : TEXCOORD0;
    float3 eyePos : TEXCOORD1;
};

float4 main(PixelInput input) : SV_Target {
    float3 n = float3(0, 0, -1);
    float d = 1.0 - clamp(dot(n, normalize(input.eyePos)), 0.0, 1.0);
    d = pow(d, strength);
    float4 tex = tex2D(diffuseImage, input.oTexCoords);
    return tex * (1.0 - d) + color * d;
}
