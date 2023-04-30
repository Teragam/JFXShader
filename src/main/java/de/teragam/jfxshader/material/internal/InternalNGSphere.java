package de.teragam.jfxshader.material.internal;

import com.sun.javafx.sg.prism.NGSphere;
import com.sun.prism.Graphics;

public class InternalNGSphere extends NGSphere {
    @Override
    protected void renderContent(Graphics g) {
        super.renderContent(MeshProxyHelper.createGraphicsProxy(g, this));
    }
}
