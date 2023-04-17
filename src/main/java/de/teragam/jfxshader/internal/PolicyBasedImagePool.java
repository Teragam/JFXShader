package de.teragam.jfxshader.internal;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import com.sun.prism.Texture;
import com.sun.scenario.effect.impl.ImagePool;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.PrTexture;

import de.teragam.jfxshader.ImagePoolPolicy;

public class PolicyBasedImagePool {

    private final ImagePool imagePool;
    private final List<SoftReference<PoolFilterable>> unlocked;
    private final List<SoftReference<PoolFilterable>> locked;


    public PolicyBasedImagePool() {
        this.imagePool = Reflect.on(ImagePool.class).createInstance().create();
        try {
            this.unlocked = Reflect.on(ImagePool.class).getFieldValue("unlocked", this.imagePool);
            this.locked = Reflect.on(ImagePool.class).getFieldValue("locked", this.imagePool);
        } catch (ShaderException e) {
            throw new ShaderException("Failed to initialize PixelFormatImagePool", e);
        }
    }

    public synchronized PoolFilterable checkOut(Renderer renderer, int w, int h) {
        return this.checkOut(renderer, w, h, false, renderer::createCompatibleImage, ImagePoolPolicy.LENIENT);
    }

    public synchronized PoolFilterable checkOut(Renderer renderer, int w, int h, boolean targetMipmaps,
                                                BiFunction<Integer, Integer, PoolFilterable> drawableSupplier, ImagePoolPolicy policy) {
        if (w <= 0 || h <= 0) {
            w = h = 1;
        }
        final int quant = policy.getQuantization();
        w = renderer.getCompatibleWidth(((w + quant - 1) / quant) * quant);
        h = renderer.getCompatibleHeight(((h + quant - 1) / quant) * quant);

        final int finalW = w;
        final int finalH = h;

        Reflect.on(ImagePool.class).processFieldValue("numAccessed", null, numCheckedOut -> (long) numCheckedOut + 1);
        Reflect.on(ImagePool.class).processFieldValue("pixelsAccessed", null, pixelsAccessed -> (long) pixelsAccessed + ((long) finalW) * finalH);

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
                if ((policy.isApproximateMatch() && lenient) || (!policy.isApproximateMatch() && exact)) {
                    final Texture tex = Reflect.on(PrTexture.class).getFieldValue("tex", filterable);
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
            Reflect.on(ImagePool.class).invokeMethod("pruneCache").invoke(null);
            try {
                img = drawableSupplier.apply(w, h);
            } catch (OutOfMemoryError ignoredEx) {
                // ignored
            }
        }

        if (img != null) {
            img.setImagePool(this.imagePool);
            this.locked.add(new SoftReference<>(img));
            Reflect.on(ImagePool.class).processFieldValue("numCreated", null, numCreated -> (long) numCreated + 1);
            Reflect.on(ImagePool.class).processFieldValue("pixelsCreated", null, pixelsCreated -> (long) pixelsCreated + ((long) finalW) * finalH);
        }
        return img;
    }

}
