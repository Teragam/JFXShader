package de.teragam.jfxshader.util;

import java.util.Arrays;
import java.util.Objects;

final class ReflectMethodCache {
    private final Class<?> clazz;
    private final String methodName;
    private final Class<?>[] parameterTypes;

    public ReflectMethodCache(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
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

        final ReflectMethodCache that = (ReflectMethodCache) o;
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
