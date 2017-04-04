/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class RpcErrorsExceptionTest {

    private static final String ERROR_MESSAGE = "Test error message.";
    private static RpcErrorsException exception;
    private List<RpcError> rpcErrors;
    private RpcError rpcError;
    private RpcError rpcWarning;

    @Before
    public void setUp() throws Exception {

        rpcError = RpcResultBuilder.newError(RpcError.ErrorType.RPC, "test", "test message");
        rpcWarning = RpcResultBuilder.newWarning(RpcError.ErrorType.RPC, "test", "test message");

        rpcErrors = new ArrayList<>();
        rpcErrors.add(rpcError);
        rpcErrors.add(rpcWarning);
        exception = new RpcErrorsException(ERROR_MESSAGE, rpcErrors);
    }

    @Test
    public void checkMessage() {
        Assert.assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    public void testGetRpcErrors() throws Exception {
        Assert.assertEquals(rpcErrors.size(), exception.getRpcErrors().size());
        final List<RpcError> actualErrors = (List) exception.getRpcErrors();
        for (int i = 0; i < actualErrors.size(); i++) {
            Assert.assertEquals(rpcErrors.get(i).getApplicationTag(), actualErrors.get(i).getApplicationTag());
            Assert.assertEquals(rpcErrors.get(i).getSeverity(), actualErrors.get(i).getSeverity());
            Assert.assertEquals(rpcErrors.get(i).getMessage(), actualErrors.get(i).getMessage());
            Assert.assertEquals(rpcErrors.get(i).getErrorType(), actualErrors.get(i).getErrorType());
            Assert.assertEquals(rpcErrors.get(i).getCause(), actualErrors.get(i).getCause());
            Assert.assertEquals(rpcErrors.get(i).getInfo(), actualErrors.get(i).getInfo());
            Assert.assertEquals(rpcErrors.get(i).getTag(), actualErrors.get(i).getTag());
        }
    }
}