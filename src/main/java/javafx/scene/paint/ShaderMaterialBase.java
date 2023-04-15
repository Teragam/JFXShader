package javafx.scene.paint;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.tk.Toolkit;

import de.teragam.jfxshader.material.internal.InternalNGPhongMaterial;


//TODO: Temporary class to test the shader material
public class ShaderMaterialBase extends Material {

    private final Map<String, Image> oldImageMap;
    private final Map<String, Boolean> dirtyImageMap;

    public ShaderMaterialBase() {
        this.oldImageMap = new HashMap<>();
        this.dirtyImageMap = new HashMap<>();
    }

    @Override
    void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) {
            this.dirtyImageMap.replaceAll((image, dirty) -> false);
        }
    }

    private NGPhongMaterial peer;

    @Override
    NGPhongMaterial getNGMaterial() {
        if (this.peer == null) {
            this.peer = new InternalNGPhongMaterial();
        }
        return this.peer;
    }

    @Override
    void updatePG() {
        if (!this.isDirty()) {
            return;
        }

        final NGPhongMaterial pMaterial = MaterialHelper.getNGMaterial(this);
        this.dirtyImageMap.forEach((image, dirty) -> {
            if (Boolean.TRUE.equals(dirty)) {
                final Image oldImage = this.oldImageMap.get(image);
                pMaterial.setDiffuseMap(oldImage == null ? null : Toolkit.getImageAccessor().getPlatformImage(oldImage)); // TODO set correct image
            }
        });
        pMaterial.setDiffuseColor(null);
        pMaterial.setSpecularColor(null);
        pMaterial.setSpecularPower(32);

        this.setDirty(false);
    }
}
