/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiRpcException extends KimiException {
    public KimiRpcException(String message) {
        super(KimiErrorCategory.RPC, message);
    }
    public KimiRpcException(String message, Throwable cause) {
        super(KimiErrorCategory.RPC, message, cause);
    }
}
