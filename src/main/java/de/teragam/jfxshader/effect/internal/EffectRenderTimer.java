package de.teragam.jfxshader.effect.internal;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javafx.animation.AnimationTimer;

public final class EffectRenderTimer {

    private static final EffectRenderTimer INSTANCE = new EffectRenderTimer();

    private final Map<UUID, WeakReference<ShaderEffectBase>> effects;

    private EffectRenderTimer() {
        this.effects = new HashMap<>();
        final AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (final Iterator<WeakReference<ShaderEffectBase>> it = EffectRenderTimer.this.effects.values().iterator(); it.hasNext(); ) {
                    final ShaderEffectBase e = it.next().get();
                    if (e != null) {
                        if (e.isContinuousRendering()) {
                            e.markDirty();
                        } else {
                            it.remove();
                        }
                    } else {
                        it.remove();
                    }
                }
            }
        };
        timer.start();
    }

    public void register(ShaderEffectBase effect) {
        this.effects.put(effect.getEffectID(), new WeakReference<>(effect));
    }

    public static EffectRenderTimer getInstance() {
        return INSTANCE;
    }

}
