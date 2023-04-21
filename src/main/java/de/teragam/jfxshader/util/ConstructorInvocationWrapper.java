package de.teragam.jfxshader.util;

@FunctionalInterface
public interface ConstructorInvocationWrapper<T> {
    T create(Object... args);
}
