package de.teragam.jfxshader.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javafx.util.Pair;

public class ReflectionHelper {

    private ReflectionHelper() {}

    private static final Map<Pair<Class<?>, String>, Field> CLASS_FIELD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
    private static final Map<MethodCache, Method> CLASS_METHOD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public static Field getField(Class<?> clazz, String fieldName) {
        return CLASS_FIELD_CONCURRENT_HASH_MAP.computeIfAbsent(new Pair<>(clazz, fieldName), c -> {
            try {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new ShaderException(String.format("Could not get declared field %s of class %s", fieldName, clazz.getName()), e);
            }
        });
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return CLASS_METHOD_CONCURRENT_HASH_MAP.computeIfAbsent(new MethodCache(clazz, methodName, parameterTypes), c -> {
            try {
                final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                throw new ShaderException(String.format("Could not get declared method %s of class %s", methodName, clazz.getName()), e);
            }
        });
    }

    public static <T> T getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        try {
            return (T) getField(clazz, fieldName).get(instance);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException(String.format("Could not access field %s of class %s", fieldName, clazz.getName()), e);
        }
    }

    public static void setFieldValue(Class<?> clazz, String fieldName, Object instance, Object value) {
        try {
            getField(clazz, fieldName).set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException(String.format("Could not access field %s of class %s", fieldName, clazz.getName()), e);
        }
    }

    public static <T> MethodInvocationWrapper<T> invokeMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        final Method method = getMethod(clazz, methodName, parameterTypes);
        return (instance, args) -> {
            try {
                return (T) method.invoke(instance, args);
            } catch (ReflectiveOperationException e) {
                throw new ShaderException(String.format("Could not invoke method %s of class %s", methodName, clazz.getName()), e);
            }
        };
    }

    public static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ShaderException(String.format("Could not resolve class %s", className), e);
        }
    }

    public static <T> T allocateInstance(Class<T> clazz) {
        final Class<?> unsafeClass = ReflectionHelper.resolveClass("sun.misc.Unsafe");
        final Object unsafe = ReflectionHelper.getFieldValue(unsafeClass, "theUnsafe", null);
        return ((MethodInvocationWrapper<T>) ReflectionHelper.invokeMethod(unsafeClass, "allocateInstance", Class.class)).invoke(unsafe, clazz);
    }

    public static void addOpens(String fullyQualifiedPackageName, String module, Module currentModule) {
        addOpensOrExports(fullyQualifiedPackageName, module, currentModule, true);
    }

    public static void addExports(String fullyQualifiedPackageName, String module, Module currentModule) {
        addOpensOrExports(fullyQualifiedPackageName, module, currentModule, false);
    }

    private static void addOpensOrExports(String fullyQualifiedPackageName, String module, Module currentModule, boolean open) {
        try {
            final Class<?> unsafeClass = ReflectionHelper.resolveClass("sun.misc.Unsafe");
            final Object unsafe = ReflectionHelper.getFieldValue(unsafeClass, "theUnsafe", null);
            final Class<?> bootModuleLayerClass = ReflectionHelper.resolveClass("java.lang.ModuleLayer");

            final Object bootModuleLayer = ReflectionHelper.invokeMethod(bootModuleLayerClass, "boot").invoke(null);
            final Object moduleOpt = ReflectionHelper.invokeMethod(bootModuleLayerClass, "findModule", String.class).invoke(bootModuleLayer, module);
            if (ReflectionHelper.invokeMethod(moduleOpt.getClass(), "isPresent").invoke(moduleOpt).equals(Boolean.FALSE)) {
                throw new IllegalStateException("Could not find module " + module);
            }
            final Object fMod = ReflectionHelper.invokeMethod(moduleOpt.getClass(), "get").invoke(moduleOpt);
            final Class<?> moduleImpl = ReflectionHelper.resolveClass("java.lang.Module");
            final String methodName = open ? "implAddOpens" : "implAddExports";
            final Method addOpensMethodImpl = ReflectionHelper.getMethod(moduleImpl, methodName, String.class, moduleImpl);
            class OffsetProvider {
                int first;
            }
            final long firstFieldOffset = (long) ReflectionHelper.invokeMethod(unsafe.getClass(), "objectFieldOffset", Field.class)
                    .invoke(unsafe, OffsetProvider.class.getDeclaredField("first"));
            ReflectionHelper.invokeMethod(unsafe.getClass(), "putBooleanVolatile", Object.class, long.class, boolean.class)
                    .invoke(unsafe, addOpensMethodImpl, firstFieldOffset, true);
            addOpensMethodImpl.invoke(fMod, fullyQualifiedPackageName, currentModule);
        } catch (ReflectiveOperationException ex) {
            throw new ShaderException("Could not add opens or exports", ex);
        }
    }

    public interface MethodInvocationWrapper<T> {
        T invoke(Object instance, Object... args);
    }

    private static final class MethodCache {
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] parameterTypes;

        public MethodCache(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }

            final MethodCache that = (MethodCache) o;
            if (!Objects.equals(this.clazz, that.clazz) || !Objects.equals(this.methodName, that.methodName)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(this.parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = this.clazz != null ? this.clazz.hashCode() : 0;
            result = 31 * result + (this.methodName != null ? this.methodName.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(this.parameterTypes);
            return result;
        }
    }

}
