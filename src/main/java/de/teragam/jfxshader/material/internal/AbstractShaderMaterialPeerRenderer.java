package de.teragam.jfxshader.material.internal;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.Graphics;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.util.Reflect;

public abstract class AbstractShaderMaterialPeerRenderer {

    private float[] worldMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];

    private final GeneralTransform3D worldTx = new GeneralTransform3D();
    private final GeneralTransform3D scratchTx = new GeneralTransform3D();
    private final Affine3D scratchAffine3DTx = new Affine3D();

    public void updateMatrices(Graphics g, BaseShaderContext context) {
        final Reflect<? extends BaseShaderContext> contextReflect = Reflect.on(context.getClass());
        final GeneralTransform3D projViewTx = contextReflect.getFieldValue("projViewTx", context);
        this.scratchTx.set(projViewTx);
        final float pixelScaleFactorX = g.getPixelScaleFactorX();
        final float pixelScaleFactorY = g.getPixelScaleFactorY();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            this.scratchTx.scale(pixelScaleFactorX, pixelScaleFactorY, 1.0);
        }
        this.viewProjectionMatrix = this.convertMatrix(this.scratchTx);

        final BaseTransform xform = g.getTransformNoClone();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            this.scratchAffine3DTx.setToIdentity();
            this.scratchAffine3DTx.scale(1.0 / pixelScaleFactorX, 1.0 / pixelScaleFactorY);
            this.scratchAffine3DTx.concatenate(xform);
            this.updateWorldTransform(this.scratchAffine3DTx);
        } else {
            this.updateWorldTransform(xform);
        }
        this.worldMatrix = this.convertMatrix(this.worldTx);
    }

    private void updateWorldTransform(BaseTransform xform) {
        this.worldTx.setIdentity();
        if ((xform != null) && (!xform.isIdentity())) {
            this.worldTx.mul(xform);
        }
    }

    protected abstract float[] convertMatrix(GeneralTransform3D src);

    public float[] getWorldMatrix() {
        return this.worldMatrix;
    }

    public float[] getViewProjectionMatrix() {
        return this.viewProjectionMatrix;
    }

}
