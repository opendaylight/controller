package org.opendaylight.controller.sal.binding.codegen;

public class CodeGenerationException extends RuntimeException{

    public CodeGenerationException() {
        super();
    }

    public CodeGenerationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodeGenerationException(String message) {
        super(message);
    }

    public CodeGenerationException(Throwable cause) {
        super(cause);
    }
}
