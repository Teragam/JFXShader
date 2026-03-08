sampler2D baseImg : register(s0);

void main(float2 texCoord : TEXCOORD0, out float4 color : SV_Target) {
    color = tex2D(baseImg, texCoord);
}
