sampler2D baseImg : register(s0);

float2 resolution : register(c0);
float2 center : register(c1);
float4 viewport : register(c2);
float4 texCoords : register(c3);
int blurSteps : register(c4);
float strength : register(c5);

void main(float2 pos0 : TEXCOORD0, float2 pos1 : TEXCOORD1, float2 pixcoord : VPOS, float4 jsl_vertexColor : COLOR0, out float4 color : SV_Target) {

    float2 scaledCenter = ((center / resolution + texCoords.xy) * texCoords.zw) * viewport.zw;
    float2 focus = pos0 - scaledCenter;
    float calcStrength = strength * (1.0 / max(resolution.x, resolution.y)) * viewport.z;

    float4 outColor = float4(0.0, 0.0, 0.0, 0.0);

    [unroll]
    for (int i = 0; i < 64; i++) {
        if (i >= blurSteps) {
            break;
        }
        float power = 1.0 - calcStrength * float(i) / float(blurSteps);
        outColor += tex2D(baseImg, focus * power + scaledCenter);
    }

    outColor *= 1.0 / float(blurSteps);
    color = outColor;
}
