package de.teragam.jfxshader.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import javafx.util.Pair;

import de.teragam.jfxshader.exception.ShaderException;

@SuppressWarnings("unchecked")
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
    private static final Map<ReflectMethodCache, Method> CLASS_METHOD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public Field getField(String fieldName) {
        return CLASS_FIELD_CONCURRENT_HASH_MAP.computeIfAbsent(new Pair<>(this.clazz, fieldName), c -> {
            try {
                final Field field = this.clazz.getDeclaredField(fieldName);
                field.trySetAccessible();
                return field;
            } catch (NoSuchFieldException e) {
                throw new ShaderException(String.format("Could not get declared field %s of class %s", fieldName, this.clazz.getName()), e);
            }
        });
    }

    public Method getMethod(String methodName, Class<?>... parameterTypes) {
        return CLASS_METHOD_CONCURRENT_HASH_MAP.computeIfAbsent(new ReflectMethodCache(this.clazz, methodName, parameterTypes), c -> {
            try {
                final Method method = this.clazz.getDeclaredMethod(methodName, parameterTypes);
                method.trySetAccessible();
                return method;
            } catch (NoSuchMethodException e) {
                final Optional<Method> methodOpt = Arrays.stream(this.clazz.getDeclaredMethods()).filter(m -> m.getName().equals(methodName)).findFirst();
                final Method method = methodOpt.orElseThrow(
                        () -> new ShaderException(String.format("Could not get declared method %s of class %s", methodName, this.clazz.getName()), e));
                method.trySetAccessible();
                return method;
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

    public <T> MethodInvocationWrapper<T> method(String methodName, Class<?>... parameterTypes) {
        return (instance, args) -> {
            try {
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
        return ((MethodInvocationWrapper<C>) unsafeReflect.method("allocateInstance", Class.class)).invoke(unsafe, this.clazz);
    }

    public boolean hasConstructor(Class<?>... parameterTypes) {
        return Arrays.stream(this.clazz.getDeclaredConstructors()).anyMatch(c -> Arrays.equals(c.getParameterTypes(), parameterTypes));
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

    public ConstructorInvocationWrapper<C> constructor(Class<?>... parameterTypes) {
        return args -> {
            try {
                if (parameterTypes.length == 0 && args.length != 0) {
                    final Class<?>[] runtimeParameterTypes = Arrays.stream(args)
                            .map(obj -> Objects.requireNonNull(obj, "Argument cannot be null"))
                            .map(Object::getClass)
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
            final Optional<Module> moduleOpt = ModuleLayer.boot().findModule(module);
            if (moduleOpt.isEmpty()) {
                throw new IllegalStateException("Could not find module " + module);
            }
            final Method addOpensMethodImpl = Reflect.on(Module.class).getMethod(open ? "implAddOpens" : "implAddExports", String.class, Module.class);
            class OffsetProvider {
                int first;
            }
            final Object unsafe = Reflect.on("sun.misc.Unsafe").getFieldValue("theUnsafe", null);
            final long firstFieldOffset = (long) Reflect.on(unsafe.getClass()).method("objectFieldOffset", Field.class)
                    .invoke(unsafe, OffsetProvider.class.getDeclaredField("first"));
            Reflect.on(unsafe.getClass()).method("putBooleanVolatile", Object.class, long.class, boolean.class)
                    .invoke(unsafe, addOpensMethodImpl, firstFieldOffset, true);
            addOpensMethodImpl.invoke(moduleOpt.get(), fullyQualifiedPackageName, currentModule);
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

    public static <P extends ReflectProxy> P createProxy(Object object, Class<P> proxyInterface) {
        final Reflect<?> reflect = Reflect.on(object.getClass());
        return createProxy(object, proxyInterface, (proxy, method, args) -> reflect.method(method.getName(), method.getParameterTypes()).invoke(object, args));
    }

    public static <P extends ReflectProxy> P createProxy(Object object, Class<P> proxyInterface, InvocationHandler invocationHandler) {
        final List<Class<?>> interfaces = new ArrayList<>(Arrays.asList(proxyInterface.getInterfaces()));
        if (proxyInterface.isInterface()) {
            interfaces.add(proxyInterface);
        }
        return (P) Proxy.newProxyInstance(proxyInterface.getClassLoader(), interfaces.toArray(new Class<?>[0]), (proxy, method, args) -> {
            if ("getObject".equals(method.getName())) {
                return object;
            } else {
                return invocationHandler.invoke(proxy, method, args);
            }
        });
    }

}
