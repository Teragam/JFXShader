sampler2D baseImg : register(s0);

float2 center : register(c0);
int blurSteps : register(c1);
float strength : register(c2);

void main(float2 pos0 : TEXCOORD0, float2 pos1 : TEXCOORD1, float2 pixcoord : VPOS, float4 jsl_vertexColor : COLOR0, out float4 color : SV_Target) {
    float2 focus = pos0 - center;
    float4 outColor = float4(0.0, 0.0, 0.0, 0.0);
    [unroll]
    for (int i = 0; i < 64; i++) {
        if (i >= blurSteps) {
            break;
        }
        float power = 1.0 - strength * float(i) / float(blurSteps);
        float2 texCoord = focus * power + center;
        outColor += tex2D(baseImg, texCoord);
    }
    outColor *= 1.0 / float(blurSteps);
    color = outColor;
}
