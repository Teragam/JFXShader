sampler2D baseImg : register(s0);

float2 pixelSize : register(c0);
float2 offset : register(c1);
float2 resolution : register(c2);
float4 texCoords : register(c3);
float4 viewport : register(c4);

void main(float2 texCoord : TEXCOORD0, out float4 color : SV_Target) {
    float2 scaledPixelSize = (pixelSize / resolution) * viewport.zw;
    float2 scaledOffset = (offset / resolution) * viewport.zw;
    float2 pixelCoord = floor((texCoord + scaledOffset) / scaledPixelSize) * scaledPixelSize;
    color = tex2D(baseImg, clamp(pixelCoord + scaledPixelSize * 0.5 + texCoords.xy, 1.0 / resolution, texCoords.zw - 1.0 / resolution));
}
