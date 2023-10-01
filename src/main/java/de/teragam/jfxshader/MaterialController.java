package de.teragam.jfxshader;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.scene.shape.BoxHelper;
import com.sun.javafx.scene.shape.CylinderHelper;
import com.sun.javafx.scene.shape.MeshViewHelper;
import com.sun.javafx.scene.shape.Shape3DHelper;
import com.sun.javafx.scene.shape.SphereHelper;
import com.sun.javafx.sg.prism.NGShape3D;
import com.sun.javafx.util.Utils;
import com.sun.prism.ResourceFactory;

import de.teragam.jfxshader.effect.internal.d3d.D3DRTTextureHelper;
import de.teragam.jfxshader.material.MaterialDependency;
import de.teragam.jfxshader.material.ShaderMaterial;
import de.teragam.jfxshader.material.ShaderMaterialPeer;
import de.teragam.jfxshader.material.internal.InternalNGBox;
import de.teragam.jfxshader.material.internal.InternalNGCylinder;
import de.teragam.jfxshader.material.internal.InternalNGMeshView;
import de.teragam.jfxshader.material.internal.InternalNGSphere;
import de.teragam.jfxshader.material.internal.ShaderMaterialBase;
import de.teragam.jfxshader.material.internal.d3d.Direct3DDevice9;
import de.teragam.jfxshader.util.Reflect;

public final class MaterialController {

    private static final HashMap<Class<? extends ShaderMaterial>, ShaderMaterialPeer<?>> MATERIAL_PEER_MAP = new HashMap<>();
    private static final Map<Long, Direct3DDevice9> D3D_DEVICE_MAP = new HashMap<>();

    private MaterialController() {}

    public static Direct3DDevice9 getD3DDevice(ResourceFactory resourceFactory) {
        return D3D_DEVICE_MAP.computeIfAbsent(D3DRTTextureHelper.getDevice(resourceFactory), Direct3DDevice9::new);
    }

    public static ShaderMaterialPeer<?> getPeer(ShaderMaterial material) {
        return MATERIAL_PEER_MAP.computeIfAbsent(material.getClass(), MaterialController::createPeer);
    }

    private static ShaderMaterialPeer<?> createPeer(Class<? extends ShaderMaterial> material) {
        if (material.isAnnotationPresent(MaterialDependency.class)) {
            final Class<? extends ShaderMaterialPeer<?>> peerClass = material.getAnnotation(MaterialDependency.class).value();
            return Reflect.on(peerClass).constructor().create();
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated with %s", material, MaterialDependency.class));
        }
    }

    /**
     * Replaces the internal {@link Shape3DHelper} accessors with custom accessors that support custom 3D shaders.
     * To allow for custom shaders on default {@link MeshView} instances, this injection must be performed before any {@link Shape3D} is rendered.
     * Otherwise, only {@link Shape3D} instances created after this injection will support custom shaders.
     */
    public static void setup3D() {
        if (PlatformUtil.isWindows()) {
            NativeLibLoader.loadLibrary("jfxshader_" + JFXShaderModule.VERSION);
        }
        MaterialController.injectMaterialAccessor();
        MaterialController.injectShape3DAccessor(MeshViewHelper.class, "meshViewAccessor", InternalNGMeshView::new);
        MaterialController.injectShape3DAccessor(SphereHelper.class, "sphereAccessor", InternalNGSphere::new);
        MaterialController.injectShape3DAccessor(BoxHelper.class, "boxAccessor", InternalNGBox::new);
        MaterialController.injectShape3DAccessor(CylinderHelper.class, "cylinderAccessor", InternalNGCylinder::new);
    }

    private static void injectMaterialAccessor() {
        Utils.forceInit(MaterialHelper.class);
        final MaterialHelper.MaterialAccessor accessor = Reflect.on(MaterialHelper.class).getFieldValue("materialAccessor", null);
        if (Proxy.isProxyClass(accessor.getClass())) {
            return;
        }
        final Object proxy = Proxy.newProxyInstance(accessor.getClass().getClassLoader(), accessor.getClass().getInterfaces(), (p, method, args) -> {
            if ("getNGMaterial".equals(method.getName())) {
                if (Reflect.on(PhongMaterial.class).getFieldValue("peer", args[0]) == null && args[0] instanceof ShaderMaterialBase) {
                    Reflect.on(PhongMaterial.class).setFieldValue("peer", args[0], ((ShaderMaterialBase) args[0]).getInternalNGMaterial());
                }
            } else if ("updatePG".equals(method.getName()) && (args[0] instanceof ShaderMaterialBase)) {
                ((ShaderMaterialBase) args[0]).updateMaterial();
            }
            return method.invoke(accessor, args);
        });
        Reflect.on(MaterialHelper.class).setFieldValue("materialAccessor", null, proxy);
    }

    private static void injectShape3DAccessor(Class<? extends Shape3DHelper> helper, String accessorField, Supplier<NGShape3D> shapeClassSupplier) {
        Utils.forceInit(helper);
        final Object accessor = Reflect.on(helper).getFieldValue(accessorField, null);
        if (Proxy.isProxyClass(accessor.getClass())) {
            return;
        }
        final Object proxy = Proxy.newProxyInstance(accessor.getClass().getClassLoader(), accessor.getClass().getInterfaces(), (p, method, args) -> {
            if ("doCreatePeer".equals(method.getName())) {
                return shapeClassSupplier.get();
            } else {
                return method.invoke(accessor, args);
            }
        });
        Reflect.on(helper).setFieldValue(accessorField, null, proxy);
    }
}
