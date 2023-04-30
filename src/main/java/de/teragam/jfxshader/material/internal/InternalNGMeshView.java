package de.teragam.jfxshader.material.internal;

import com.sun.javafx.sg.prism.NGMeshView;
import com.sun.prism.Graphics;

public class InternalNGMeshView extends NGMeshView {
    @Override
    protected void renderContent(Graphics g) {
        super.renderContent(MeshProxyHelper.createGraphicsProxy(g, this));
    }
}
