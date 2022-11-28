package de.teragam.jfxshader.internal;

public class ShaderException extends RuntimeException {

    public ShaderException(String message) {
        super(message);
    }

    public ShaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
