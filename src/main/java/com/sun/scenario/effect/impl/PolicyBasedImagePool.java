package com.sun.scenario.effect.impl;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import com.sun.prism.Texture;
import com.sun.scenario.effect.impl.prism.PrTexture;

import de.teragam.jfxshader.ImagePoolPolicy;
import de.teragam.jfxshader.internal.ShaderException;

public class PolicyBasedImagePool extends ImagePool {

    private final List<SoftReference<PoolFilterable>> unlocked;
    private final List<SoftReference<PoolFilterable>> locked;

    private final Field texField;
    private final Method pruneCacheMethod;

    public PolicyBasedImagePool() {
        try {
            this.texField = PrTexture.class.getDeclaredField("tex");
            this.texField.setAccessible(true);
            this.pruneCacheMethod = ImagePool.class.getDeclaredMethod("pruneCache");
            this.pruneCacheMethod.setAccessible(true);
            final Field unlockedField = ImagePool.class.getDeclaredField("unlocked");
            unlockedField.setAccessible(true);
            this.unlocked = (List<SoftReference<PoolFilterable>>) unlockedField.get(this);
            final Field lockedField = ImagePool.class.getDeclaredField("locked");
            lockedField.setAccessible(true);
            this.locked = (List<SoftReference<PoolFilterable>>) lockedField.get(this);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Failed to initialize PixelFormatImagePool", e);
        }
    }

    @Override
    public synchronized PoolFilterable checkOut(Renderer renderer, int w, int h) {
        return this.checkOut(renderer, w, h, false, renderer::createCompatibleImage, ImagePoolPolicy.LENIENT);
    }

    public synchronized PoolFilterable checkOut(Renderer renderer, int w, int h, boolean targetMipmaps,
                                                BiFunction<Integer, Integer, PoolFilterable> drawableSupplier, ImagePoolPolicy policy) {
        if (w <= 0 || h <= 0) {
            w = h = 1;
        }
        w = renderer.getCompatibleWidth(((w + QUANT - 1) / QUANT) * QUANT);
        h = renderer.getCompatibleHeight(((h + QUANT - 1) / QUANT) * QUANT);

        ImagePool.numAccessed++;
        ImagePool.pixelsAccessed += ((long) w) * h;
        SoftReference<PoolFilterable> chosenEntry = null;
        PoolFilterable chosenImage = null;
        int minDiff = Integer.MAX_VALUE;
        final Iterator<SoftReference<PoolFilterable>> entries = this.unlocked.iterator();
        while (entries.hasNext()) {
            final SoftReference<PoolFilterable> entry = entries.next();
            final PoolFilterable filterable = entry.get();
            if (filterable == null) {
                entries.remove();
            } else {
                final int ew = filterable.getMaxContentWidth();
                final int eh = filterable.getMaxContentHeight();
                final int diff = (ew - w) * (eh - h);
                final boolean lenient = ew >= w && eh >= h && ew * eh / 2 <= w * h && (chosenEntry == null || diff < minDiff);
                final boolean exact = ew == w && eh == h;
                if ((policy == ImagePoolPolicy.LENIENT && lenient) || (policy == ImagePoolPolicy.EXACT && exact)) {
                    final Texture tex = this.getTexture(filterable);
                    if (tex.getUseMipmap() == targetMipmaps) {
                        filterable.lock();
                        if (filterable.isLost()) {
                            entries.remove();
                            continue;
                        }
                        if (chosenImage != null) {
                            chosenImage.unlock();
                        }
                        chosenEntry = entry;
                        chosenImage = filterable;
                        minDiff = diff;
                        if (exact) {
                            break;
                        }
                    }
                }
            }
        }

        if (chosenEntry != null) {
            this.unlocked.remove(chosenEntry);
            this.locked.add(chosenEntry);
            renderer.clearImage(chosenImage);
            return chosenImage;
        }

        this.locked.removeIf(entry -> entry.get() == null);

        PoolFilterable img = null;
        try {
            img = drawableSupplier.apply(w, h);
        } catch (OutOfMemoryError ignored) {
            this.prune();
            try {
                img = drawableSupplier.apply(w, h);
            } catch (OutOfMemoryError ignoredEx) {
            }
        }

        if (img != null) {
            img.setImagePool(this);
            this.locked.add(new SoftReference<>(img));
            ImagePool.numCreated++;
            ImagePool.pixelsCreated += ((long) w) * h;
        }
        return img;
    }

    private void prune() {
        try {
            this.pruneCacheMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Failed to prune cache", e);
        }
    }

    private Texture getTexture(PoolFilterable img) {
        try {
            return ((Texture) this.texField.get(img));
        } catch (IllegalAccessException e) {
            throw new ShaderException("Could not get pixel format of image", e);
        }
    }
}
