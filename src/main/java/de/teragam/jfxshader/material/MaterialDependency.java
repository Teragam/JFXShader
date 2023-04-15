package de.teragam.jfxshader.material;

public @interface MaterialDependency {

    Class<? extends ShaderMaterialPeer<?>> value();

}
