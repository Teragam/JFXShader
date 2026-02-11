sampler2D botImg : register(s0);
sampler2D topImg : register(s1);
int count : register(c0);
float4 rects[8] : register(c1);
float4 ops[8] : register(c9);
float scale : register(c17);
int invertMask : register(c18);
float2 pixCoordOffset : register(c19);


float roundRect(float2 p, float4 box, float radius, float feather) {
    float2 size = box.zw - box.xy;
    radius = min(radius, min(size.x, size.y) / 2.0);
    float2 q = abs(p - box.xy - size / 2.0) - size / 2.0 + radius;
    float distance = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    return 1.0 - smoothstep(min(feather, 0.0), max(feather, 0.0), distance);
}

void main(in float2 pos0 : TEXCOORD0, in float2 pos1 : TEXCOORD1, in float2 pixcoord : VPOS, in float4 jsl_vertexColor : COLOR0, out float4 color : COLOR0) {
    pixcoord += pixCoordOffset;
    float4 bot = tex2D(botImg, pos0);
    float4 top = tex2D(topImg, pos1);
    float factor = 0.0;
    for (int i = 0; i < 8; i++){
        if (i >= count) {
            break;
        }
         // The scale is used to compensate for dpi scaling
        factor += roundRect(pixcoord / scale, rects[i], ops[i].x, ops[i].y) * ops[i].z;
    }
    factor = clamp(factor, 0.0, 1.0);
    if (invertMask == 1) {
        factor = 1.0 - factor;
    }
    color = bot * factor + top * (1.0 - factor);
}
