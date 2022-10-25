package de.teragam.jfxshader.samples.blendshapes;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.ObjectProperty;

import com.sun.javafx.geom.Rectangle;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.BufferUtil;

import de.teragam.jfxshader.EffectPeer;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.ShaderEffectPeer;

@EffectPeer("BlendShapes")
class BlendShapesEffectPeer extends ShaderEffectPeer<BlendShapes> {

    private FloatBuffer rects;
    private FloatBuffer ops;

    protected BlendShapesEffectPeer(FilterContext fctx) {
        super(fctx);
    }

    @Override
    protected ShaderDeclaration createShaderDeclaration() {
        final HashMap<String, Integer> samplers = new HashMap<>();
        samplers.put("botImg", 0);
        samplers.put("topImg", 1);
        final HashMap<String, Integer> params = new HashMap<>();
        params.put("count", 0);
        params.put("rects", 1);
        params.put("ops", 5);
        return new ShaderDeclaration(samplers, params,
                BlendShapes.class.getResourceAsStream("/samples/blendshapes/blendshapes.frag"),
                BlendShapes.class.getResourceAsStream("/samples/blendshapes/blendshapes.obj"));
    }

    @Override
    protected void updateShader(Shader shader, BlendShapes effect) {
        if (this.rects == null) {
            this.rects = BufferUtil.newFloatBuffer(4 * 4);
            this.ops = BufferUtil.newFloatBuffer(4 * 4);
        }
        this.rects.clear();
        this.ops.clear();
        final List<ObjectProperty<BlendShape>> effectShapes = effect.getBlendShapes();
        for (int i = 0; i < 4; i++) {
            if (i < effectShapes.size()) {
                final BlendShape shape = effectShapes.get(i).get();
                if (shape != null) {
                    final Rectangle bounds = shape.getBounds();
                    this.rects.put(new float[]{bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height});
                    this.ops.put(new float[]{(float) shape.getWidth(), (float) shape.getFeather(), (float) shape.getOpacity(), 0F});
                    continue;
                }
            }
            this.rects.put(new float[]{0, 0, 0, 0});
            this.ops.put(new float[]{0, 0, 0, 0});
        }
        this.rects.rewind();
        this.ops.rewind();
        shader.setConstant("count", effect.getBlendShapes().size());
        shader.setConstants("rects", this.rects, 0, 4);
        shader.setConstants("ops", this.ops, 0, 4);
    }
}
