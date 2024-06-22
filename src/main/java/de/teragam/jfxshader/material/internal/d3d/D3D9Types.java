package de.teragam.jfxshader.material.internal.d3d;

public final class D3D9Types {

    private D3D9Types() {}

    // D3DFORMAT constants
    public static final int D3DFMT_INDEX16 = 0x65;
    public static final int D3DFMT_INDEX32 = 0x66;
    public static final int D3DFMT_R32F = 0x72;
    public static final int D3DFMT_G32R32F = 0x73;
    public static final int D3DFMT_A32B32G32R32F = 0x74;

    // D3DFVF constants
    public static final int D3DFVF_XYZ = 0x2;
    public static final int D3DFVF_NORMAL = 0x10;
    public static final int D3DFVF_PSIZE = 0x20;
    public static final int D3DFVF_DIFFUSE = 0x40;
    public static final int D3DFVF_SPECULAR = 0x80;
    public static final int D3DFVF_TEXCOUNT_SHIFT = 0x8;
    public static final int D3DFVF_TEXTUREFORMAT1 = 0x3;
    public static final int D3DFVF_TEXTUREFORMAT2 = 0x0;
    public static final int D3DFVF_TEXTUREFORMAT3 = 0x1;
    public static final int D3DFVF_TEXTUREFORMAT4 = 0x2;

    public static int D3DFVF_TEXCOORDSIZE1(int coordIndex) {
        return D3DFVF_TEXTUREFORMAT1 << (coordIndex * 2 + 16);
    }

    public static int D3DFVF_TEXCOORDSIZE2(int coordIndex) {
        return D3DFVF_TEXTUREFORMAT2;
    }

    public static int D3DFVF_TEXCOORDSIZE3(int coordIndex) {
        return D3DFVF_TEXTUREFORMAT3 << (coordIndex * 2 + 16);
    }

    public static int D3DFVF_TEXCOORDSIZE4(int coordIndex) {
        return D3DFVF_TEXTUREFORMAT4 << (coordIndex * 2 + 16);
    }

    // D3DUSAGE constants
    public static final int D3DUSAGE_WRITEONLY = 0x8;

    // D3DFILLMODE constants
    public static final int D3DFILL_POINT = 0x1;
    public static final int D3DFILL_WIREFRAME = 0x2;
    public static final int D3DFILL_SOLID = 0x3;

    // D3DRENDERSTATETYPE constants
    public static final int D3DRS_FILLMODE = 0x8;
    public static final int D3DRS_CULLMODE = 0x16;

    // D3DPRIMITIVETYPE constants
    public static final int D3DPT_POINTLIST = 0x1;
    public static final int D3DPT_LINELIST = 0x2;
    public static final int D3DPT_LINESTRIP = 0x3;
    public static final int D3DPT_TRIANGLELIST = 0x4;
    public static final int D3DPT_TRIANGLESTRIP = 0x5;
    public static final int D3DPT_TRIANGLEFAN = 0x6;

    // D3DCULL constants
    public static final int D3DCULL_NONE = 0x1;
    public static final int D3DCULL_CW = 0x2;
    public static final int D3DCULL_CCW = 0x3;

    // D3DPOOL constants
    public static final int D3DPOOL_DEFAULT = 0x0;
    public static final int D3DPOOL_MANAGED = 0x1;
    public static final int D3DPOOL_SYSTEMMEM = 0x2;
    public static final int D3DPOOL_SCRATCH = 0x3;

    // D3DVERTEXTEXTURESAMPLER constants
    public static final int D3DDMAPSAMPLER = 0x100;
    public static final int D3DVERTEXTEXTURESAMPLER0 = D3DDMAPSAMPLER + 1;
    public static final int D3DVERTEXTEXTURESAMPLER1 = D3DDMAPSAMPLER + 2;
    public static final int D3DVERTEXTEXTURESAMPLER2 = D3DDMAPSAMPLER + 3;
    public static final int D3DVERTEXTEXTURESAMPLER3 = D3DDMAPSAMPLER + 4;


    // D3DSAMPLERSTATETYPE constants
    public static final int D3DSAMP_ADDRESSU = 0x1;
    public static final int D3DSAMP_ADDRESSV = 0x2;
    public static final int D3DSAMP_ADDRESSW = 0x3;
    public static final int D3DSAMP_BORDERCOLOR = 0x4;
    public static final int D3DSAMP_MAGFILTER = 0x5;
    public static final int D3DSAMP_MINFILTER = 0x6;
    public static final int D3DSAMP_MIPFILTER = 0x7;
    public static final int D3DSAMP_MIPMAPLODBIAS = 0x8;
    public static final int D3DSAMP_MAXMIPLEVEL = 0x9;
    public static final int D3DSAMP_MAXANISOTROPY = 0xA;
    public static final int D3DSAMP_SRGBTEXTURE = 0xB;
    public static final int D3DSAMP_ELEMENTINDEX = 0xC;
    public static final int D3DSAMP_DMAPOFFSET = 0xD;
    public static final int D3DSAMP_FORCE_DWORD = 0x7fffffff;

    // D3DTEXTUREADDRESS constants
    public static final int D3DTADDRESS_WRAP = 0x1;
    public static final int D3DTADDRESS_MIRROR = 0x2;
    public static final int D3DTADDRESS_CLAMP = 0x3;
    public static final int D3DTADDRESS_BORDER = 0x4;
    public static final int D3DTADDRESS_MIRRORONCE = 0x5;
    public static final int D3DTADDRESS_FORCE_DWORD = 0x7fffffff;

}
