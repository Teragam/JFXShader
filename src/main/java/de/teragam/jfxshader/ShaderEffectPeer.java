package de.teragam.jfxshader;

import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.InternalCoreEffectBase;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.ps.PPSMultiSamplerPeer;
import com.sun.scenario.effect.impl.state.RenderState;

public abstract class ShaderEffectPeer<T extends ShaderEffect> extends PPSMultiSamplerPeer<RenderState> {

    protected ShaderEffectPeer(FilterContext fctx) {
        super(fctx, Renderer.getRenderer(fctx), null);
    }

    @Override
    protected boolean isSamplerLinear(int i) {
        return false;
    }

    @Override
    protected Shader createShader() {
        return ShaderController.createShader(super.getFilterContext(), this.createShaderDeclaration());
    }

    @Override
    protected void updateShader(Shader shader) {
        this.updateShader(shader, (T) ((InternalCoreEffectBase) super.getEffect()).getEffect());
    }

    protected abstract ShaderDeclaration createShaderDeclaration();

    protected abstract void updateShader(Shader shader, T effect);

}
