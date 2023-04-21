package de.teragam.jfxshader.util;

@FunctionalInterface
public interface MethodInvocationWrapper<T> {
    T invoke(Object instance, Object... args);
}
