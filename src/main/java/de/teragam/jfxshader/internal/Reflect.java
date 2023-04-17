package de.teragam.jfxshader.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import javafx.util.Pair;

public class Reflect<C> {

    private final Class<C> clazz;

    private Reflect(Class<C> clazz) {
        this.clazz = clazz;
    }

    public static <T> Reflect<T> on(Class<T> clazz) {
        return new Reflect<>(clazz);
    }

    public static <T> Reflect<T> on(String clazzName) {
        return (Reflect<T>) on(resolveClass(clazzName));
    }

    private static final Map<Pair<Class<?>, String>, Field> CLASS_FIELD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
    private static final Map<MethodCache, Method> CLASS_METHOD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public Field getField(String fieldName) {
        return CLASS_FIELD_CONCURRENT_HASH_MAP.computeIfAbsent(new Pair<>(this.clazz, fieldName), c -> {
            try {
                final Field field = this.clazz.getDeclaredField(fieldName);
                if (!field.trySetAccessible()) {
                    System.err.println("Could not set field " + fieldName + " of class " + this.clazz.getName() + " accessible"); //TODO remove
                }
                return field;
            } catch (NoSuchFieldException e) {
                throw new ShaderException(String.format("Could not get declared field %s of class %s", fieldName, this.clazz.getName()), e);
            }
        });
    }

    public Method getMethod(String methodName, Class<?>... parameterTypes) {
        return CLASS_METHOD_CONCURRENT_HASH_MAP.computeIfAbsent(new MethodCache(this.clazz, methodName, parameterTypes), c -> {
            try {
                final Method method = this.clazz.getDeclaredMethod(methodName, parameterTypes);
                if (!method.trySetAccessible()) {
                    System.err.println("Could not set method " + methodName + " of class " + this.clazz.getName() + " accessible"); //TODO remove
                }
                return method;
            } catch (NoSuchMethodException e) {
                throw new ShaderException(String.format("Could not get declared method %s of class %s", methodName, this.clazz.getName()), e);
            }
        });
    }

    public <T> T getFieldValue(String fieldName, Object instance) {
        try {
            return (T) this.getField(fieldName).get(instance);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException(String.format("Could not access field %s of class %s", fieldName, this.clazz.getName()), e);
        }
    }

    public void setFieldValue(String fieldName, Object instance, Object value) {
        try {
            this.getField(fieldName).set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException(String.format("Could not access field %s of class %s", fieldName, this.clazz.getName()), e);
        }
    }

    public <T> void processFieldValue(String fieldName, Object instance, UnaryOperator<T> processor) {
        try {
            final Field field = this.getField(fieldName);
            field.set(instance, processor.apply((T) field.get(instance)));
        } catch (ReflectiveOperationException e) {
            throw new ShaderException(String.format("Could not access field %s of class %s", fieldName, this.clazz.getName()), e);
        }
    }

    public <T> MethodInvocationWrapper<T> invokeMethod(String methodName, Class<?>... parameterTypes) {
        return (instance, args) -> {
            try {
                if (parameterTypes.length == 0 && args.length != 0) {
                    final Class<?>[] runtimeParameterTypes = Arrays.stream(args).map(Objects::requireNonNull).map(Object::getClass)
                            .map(Reflect::convertToPrimitiveClass).toArray(Class<?>[]::new);
                    return (T) this.getMethod(methodName, runtimeParameterTypes).invoke(instance, args);
                }
                return (T) this.getMethod(methodName, parameterTypes).invoke(instance, args);
            } catch (ReflectiveOperationException e) {
                throw new ShaderException(String.format("Could not invoke method %s of class %s", methodName, this.clazz.getName()), e);
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

    public C allocateInstance() {
        final Reflect<?> unsafeReflect = Reflect.on("sun.misc.Unsafe");
        final Object unsafe = unsafeReflect.getFieldValue("theUnsafe", null);
        return ((MethodInvocationWrapper<C>) unsafeReflect.invokeMethod("allocateInstance", Class.class)).invoke(unsafe, this.clazz);
    }

    public Constructor<C> getConstructor(Class<?>... parameterTypes) {
        try {
            final Constructor<C> constructor = this.clazz.getDeclaredConstructor(parameterTypes);
            constructor.trySetAccessible();
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new ShaderException(String.format("Could not get constructor of class %s", this.clazz.getName()), e);
        }
    }

    public ConstructorInvocationWrapper<C> createInstance(Class<?>... parameterTypes) {
        return args -> {
            try {
                if (parameterTypes.length == 0 && args.length != 0) {
                    final Class<?>[] runtimeParameterTypes = Arrays.stream(args).map(Objects::requireNonNull).map(Object::getClass)
                            .map(Reflect::convertToPrimitiveClass).toArray(Class<?>[]::new);
                    return this.getConstructor(runtimeParameterTypes).newInstance(args);
                }
                return this.getConstructor(parameterTypes).newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new ShaderException(String.format("Could not create instance of class %s", this.clazz.getName()), e);
            }
        };
    }

    public static void addOpens(String fullyQualifiedPackageName, String module, Module currentModule) {
        addOpensOrExports(fullyQualifiedPackageName, module, currentModule, true);
    }

    public static void addExports(String fullyQualifiedPackageName, String module, Module currentModule) {
        addOpensOrExports(fullyQualifiedPackageName, module, currentModule, false);
    }

    private static void addOpensOrExports(String fullyQualifiedPackageName, String module, Module currentModule, boolean open) {
        try {
            final Object unsafe = Reflect.on("sun.misc.Unsafe").getFieldValue("theUnsafe", null);
            final Class<?> bootModuleLayerClass = Reflect.resolveClass("java.lang.ModuleLayer");

            final Object bootModuleLayer = Reflect.on(bootModuleLayerClass).invokeMethod("boot").invoke(null);
            final Object moduleOpt = Reflect.on(bootModuleLayerClass).invokeMethod("findModule", String.class).invoke(bootModuleLayer, module);
            if (Reflect.on(moduleOpt.getClass()).invokeMethod("isPresent").invoke(moduleOpt).equals(Boolean.FALSE)) {
                throw new IllegalStateException("Could not find module " + module);
            }
            final Object fMod = Reflect.on(moduleOpt.getClass()).invokeMethod("get").invoke(moduleOpt);
            final Class<?> moduleImpl = Reflect.resolveClass("java.lang.Module");
            final String methodName = open ? "implAddOpens" : "implAddExports";
            final Method addOpensMethodImpl = Reflect.on(moduleImpl).getMethod(methodName, String.class, moduleImpl);
            class OffsetProvider {
                int first;
            }
            final long firstFieldOffset = (long) Reflect.on(unsafe.getClass()).invokeMethod("objectFieldOffset", Field.class)
                    .invoke(unsafe, OffsetProvider.class.getDeclaredField("first"));
            Reflect.on(unsafe.getClass()).invokeMethod("putBooleanVolatile", Object.class, long.class, boolean.class)
                    .invoke(unsafe, addOpensMethodImpl, firstFieldOffset, true);
            addOpensMethodImpl.invoke(fMod, fullyQualifiedPackageName, currentModule);
        } catch (ReflectiveOperationException ex) {
            throw new ShaderException("Could not add opens or exports", ex);
        }
    }

    private static Class<?> convertToPrimitiveClass(Class<?> clazz) {
        if (clazz == Integer.class) {
            return int.class;
        } else if (clazz == Long.class) {
            return long.class;
        } else if (clazz == Float.class) {
            return float.class;
        } else if (clazz == Double.class) {
            return double.class;
        } else if (clazz == Boolean.class) {
            return boolean.class;
        } else if (clazz == Byte.class) {
            return byte.class;
        } else if (clazz == Short.class) {
            return short.class;
        } else if (clazz == Character.class) {
            return char.class;
        }
        return clazz;
    }

    @FunctionalInterface
    public interface MethodInvocationWrapper<T> {
        T invoke(Object instance, Object... args);
    }

    @FunctionalInterface
    public interface ConstructorInvocationWrapper<T> {
        T create(Object... args);
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

    public interface ReflectProxy {

        /**
         * @return the object that is proxied by this proxy
         */
        Object getObject();

    }

    public static <P extends ReflectProxy> P createProxy(Object object, Class<P> proxyInterface) {
        final Reflect<?> reflect = Reflect.on(object.getClass());
        return (P) Proxy.newProxyInstance(proxyInterface.getClassLoader(), proxyInterface.getInterfaces(), (proxy, method, args) -> {
            if ("getObject".equals(method.getName())) {
                return object;
            } else {
                return reflect.invokeMethod(method.getName(), method.getParameterTypes()).invoke(object, args);
            }
        });
    }

}
