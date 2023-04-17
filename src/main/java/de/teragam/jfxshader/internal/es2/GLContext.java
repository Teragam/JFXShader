package de.teragam.jfxshader.internal.es2;

import de.teragam.jfxshader.internal.Reflect;

public interface GLContext extends Reflect.ReflectProxy {

    boolean canClampToZero();

    int getMaxTextureSize();

    boolean canCreateNonPowTwoTextures();

    void setActiveTextureUnit(int unit);

    int getBoundFBO();

    int getBoundTexture();

    int genAndBindTexture();

    void texParamsMinMax(int pname, boolean useMipmap);

    int createFBO(int texID);

    void deleteTexture(int tID);

    void bindFBO(int nativeFBOID);

    void setBoundTexture(int texid);

}
