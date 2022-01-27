/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class RpcErrorsExceptionTest {
    private static final String ERROR_MESSAGE = "Test error message.";

    private List<RpcError> rpcErrors;
    private RpcErrorsException exception;

    @Before
    public void setUp() {
        final RpcError rpcError = RpcResultBuilder.newError(ErrorType.RPC, new ErrorTag("error"), "error message");
        final RpcError rpcWarning = RpcResultBuilder.newWarning(ErrorType.RPC, new ErrorTag("warning"),
            "warning message");

        rpcErrors = new ArrayList<>();
        rpcErrors.add(rpcError);
        rpcErrors.add(rpcWarning);

        exception = new RpcErrorsException(ERROR_MESSAGE, rpcErrors);
    }

    @Test
    public void testGetMessage() {
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    public void testGetRpcErrors() {
        final List<RpcError> actualErrors = (List<RpcError>) exception.getRpcErrors();
        assertEquals(rpcErrors.size(), actualErrors.size());

        for (int i = 0; i < actualErrors.size(); i++) {
            final RpcError expected = rpcErrors.get(i);
            final RpcError actual = actualErrors.get(i);

            assertEquals(expected.getApplicationTag(), actual.getApplicationTag());
            assertEquals(expected.getSeverity(), actual.getSeverity());
            assertEquals(expected.getMessage(), actual.getMessage());
            assertEquals(expected.getErrorType(), actual.getErrorType());
            assertEquals(expected.getCause(), actual.getCause());
            assertEquals(expected.getInfo(), actual.getInfo());
            assertEquals(expected.getTag(), actual.getTag());
        }
    }
}
