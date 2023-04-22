package de.teragam.jfxshader.material.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.sg.prism.NGShape3D;
import com.sun.javafx.sg.prism.NGTriangleMesh;
import com.sun.prism.Graphics;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.ResourceFactory;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.impl.BaseMesh;

import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.material.ShaderMaterial;
import de.teragam.jfxshader.material.internal.es2.ES2ShaderMeshView;
import de.teragam.jfxshader.material.internal.es2.InternalES2BasePhongMaterial;
import de.teragam.jfxshader.util.Reflect;

public final class MeshProxyHelper {

    private MeshProxyHelper() {}

    public static Graphics createGraphicsProxy(Graphics g, NGShape3D shape) {
        final NGPhongMaterial material = Reflect.on(NGShape3D.class).getFieldValue("material", shape);
        final MeshView meshView = Reflect.on(NGShape3D.class).getFieldValue("meshView", shape);
        if (!(material instanceof InternalNGPhongMaterial)) {
            if (meshView instanceof ShaderMeshView) {
                MeshProxyHelper.resetShape(shape);
            }
            return g;
        } else {
            if (!(meshView instanceof ShaderMeshView) && meshView != null) {
                MeshProxyHelper.resetShape(shape);
            }
            final Object proxyFactory = MeshProxyHelper.createResourceFactoryProxy(g.getResourceFactory(), ((InternalNGPhongMaterial) material).getMaterial());
            final Object proxyGraphics = Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[]{Graphics.class, GraphicsHelper.class},
                    (proxy, method, args) -> {
                        if ("getResourceFactory".equals(method.getName())) {
                            return proxyFactory;
                        }
                        if ("getRawGraphics".equals(method.getName())) {
                            return g;
                        }
                        return method.invoke(g, args);
                    });

            return (Graphics) proxyGraphics;
        }
    }

    private static void resetShape(NGShape3D shape) {
        final MeshView meshView = Reflect.on(NGShape3D.class).getFieldValue("meshView", shape);
        if (meshView != null) {
            meshView.dispose();
            Reflect.on(NGShape3D.class).setFieldValue("meshView", shape, null);
            final NGTriangleMesh mesh = Reflect.on(NGShape3D.class).getFieldValue("mesh", shape);
            if (mesh != null) {
                final Mesh internalMesh = Reflect.on(NGTriangleMesh.class).getFieldValue("mesh", mesh);
                if (internalMesh != null) {
                    internalMesh.dispose();
                    Reflect.on(NGTriangleMesh.class).setFieldValue("mesh", mesh, null);
                }
            }
        }
        final NGPhongMaterial material = Reflect.on(NGShape3D.class).getFieldValue("material", shape);
        if (material != null && Reflect.on(NGPhongMaterial.class).getFieldValue("material", material) != null) {
            Reflect.on(NGPhongMaterial.class).method("disposeMaterial").invoke(material);
        }
    }

    private static ResourceFactory createResourceFactoryProxy(ResourceFactory rf, ShaderMaterial material) {
        final InvocationHandler handler;
        if (ShaderController.isHLSLSupported()) {
            handler = (proxy, method, args) -> {
                if ("createMesh".equals(method.getName())) {
                    return ShaderBaseMesh.create(rf);
                }
                if ("createMeshView".equals(method.getName())) {
                    return ShaderMeshView.create((ShaderBaseMesh) args[0]);
                }
                if ("createPhongMaterial".equals(method.getName())) {
                    return InternalBasePhongMaterial.create(material);
                }
                return method.invoke(rf, args);
            };
        } else {
            handler = (proxy, method, args) -> {
                if ("createMeshView".equals(method.getName())) {
                    return ES2ShaderMeshView.create((ES2ResourceFactory) rf, (BaseMesh) args[0]);
                }
                if ("createPhongMaterial".equals(method.getName())) {
                    return InternalES2BasePhongMaterial.create((ES2ResourceFactory) rf, material);
                }
                return method.invoke(rf, args);
            };
        }
        return (ResourceFactory) Proxy.newProxyInstance(ResourceFactory.class.getClassLoader(), new Class<?>[]{ResourceFactory.class}, handler);
    }

    public interface GraphicsHelper {
        Graphics getRawGraphics();
    }

}
