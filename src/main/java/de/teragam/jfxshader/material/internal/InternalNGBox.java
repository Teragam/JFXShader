package de.teragam.jfxshader.material.internal;

import com.sun.javafx.sg.prism.NGBox;
import com.sun.prism.Graphics;

public class InternalNGBox extends NGBox {
    @Override
    protected void renderContent(Graphics g) {
        super.renderContent(MeshProxyHelper.createGraphicsProxy(g, this));
    }
}
