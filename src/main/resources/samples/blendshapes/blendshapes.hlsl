sampler2D botImg : register(s0);
sampler2D topImg : register(s1);
int count : register(c0);
float4 rects[4] : register(c1);
float4 ops[4] : register(c5);

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

float insideRect(float2 p, float4 rect, float4 ops) {
    float dx = max(rect.x - p.x, max(p.x - rect.z, 0.0));
    float dy = max(rect.y - p.y, max(p.y - rect.w, 0.0));
    float d = sqrt(dx * dx + dy * dy);
    if (ops.x == 0.0) {
        return 1.0 - clamp(ceil(d), 0.0, 1.0);
    } else {
        return map(clamp(d / ops.x, 0.0, 1.0), ops.y, 1.0, 1.0, 0.0);
    }
}

void main(in float2 pos0 : TEXCOORD0, in float2 pos1 : TEXCOORD1, in float2 pixcoord : VPOS, in float4 jsl_vertexColor : COLOR0, out float4 color : COLOR0) {
    float4 bot = tex2D(botImg, pos0);
    float4 top = tex2D(topImg, pos1);
    float factor = 0.0;
    for (int i = 0; i < count; i++){
        factor += insideRect(pixcoord, rects[i], ops[i]) * ops[i].z;
    }
    factor = clamp(factor, 0.0, 1.0);
    color = top * factor + bot * (1.0 - factor);
}
