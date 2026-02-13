sampler2D baseImg : register(s0);

float2 pixelSize : register(c0);
float2 resolution : register(c1);
float4 viewport : register(c2);

void main(float2 texCoord : TEXCOORD0, out float4 color : SV_Target) {
    float2 normalizedTexCoord = (texCoord + viewport.xy) / viewport.zw;
    normalizedTexCoord = floor(normalizedTexCoord / pixelSize) * pixelSize;
    normalizedTexCoord += pixelSize * 0.5; // Center the pixel
    color = tex2D(baseImg, normalizedTexCoord * viewport.zw - viewport.xy);
}
