package de.teragam.jfxshader.effect.internal.es2;

import de.teragam.jfxshader.util.ReflectProxy;

public interface GLContext extends ReflectProxy {

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
