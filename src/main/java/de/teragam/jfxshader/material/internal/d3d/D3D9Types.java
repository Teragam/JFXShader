package de.teragam.jfxshader.material.internal.d3d;

public final class D3D9Types {

    private D3D9Types() {}

    // D3DFORMAT constants
    public static final int D3DFMT_INDEX16 = 0x65;
    public static final int D3DFMT_INDEX32 = 0x66;

    // D3DFVF constants
    public static final int D3DFVF_XYZ = 0x2;
    public static final int D3DFVF_TEXCOUNT_SHIFT = 0x8;
    public static final int D3DFVF_TEXTUREFORMAT4 = 0x2;

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

}
