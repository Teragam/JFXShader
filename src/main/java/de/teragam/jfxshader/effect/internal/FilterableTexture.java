package de.teragam.jfxshader.effect.internal;

import com.sun.prism.Texture;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.impl.prism.PrTexture;

public class FilterableTexture extends PrTexture<Texture> implements Filterable {

    private boolean disposed;

    public FilterableTexture(Texture tex) {
        super(tex);
    }

    @Override
    public Object getData() {
        return this;
    }

    @Override
    public float getPixelScale() {
        return 1.0f;
    }

    @Override
    public int getContentWidth() {
        return this.getTextureObject().getContentWidth();
    }

    @Override
    public int getContentHeight() {
        return this.getTextureObject().getContentHeight();
    }

    @Override
    public int getMaxContentWidth() {
        return this.getTextureObject().getMaxContentWidth();
    }

    @Override
    public int getMaxContentHeight() {
        return this.getTextureObject().getMaxContentHeight();
    }

    @Override
    public void setContentWidth(int contentW) {
        this.getTextureObject().setContentWidth(contentW);
    }

    @Override
    public void setContentHeight(int contentH) {
        this.getTextureObject().setContentHeight(contentH);
    }

    @Override
    public int getPhysicalWidth() {
        return this.getTextureObject().getPhysicalWidth();
    }

    @Override
    public int getPhysicalHeight() {
        return this.getTextureObject().getPhysicalHeight();
    }

    @Override
    public void flush() {
        if (!this.disposed && this.getTextureObject() != null) {
            this.getTextureObject().dispose();
            this.disposed = true;
        }
    }
}
