/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.common.RpcError;

public class RpcErrorsExceptionTest {

    private static final String ERROR_MESSAGE = "Test error message.";
    private static RpcErrorsException exception;
    private Collection<RpcError> rpcErrors;

    @Mock
    private RpcError error1;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        rpcErrors = new ArrayList<>();
        rpcErrors.add(error1);
        exception = new RpcErrorsException(ERROR_MESSAGE, rpcErrors);
    }

    @Test
    public void checkMessage() {
        Assert.assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    public void testGetRpcErrors() throws Exception {
        final RpcError expectedError = rpcErrors.iterator().next();
        final RpcError actualError = exception.getRpcErrors().iterator().next();
        Assert.assertEquals(expectedError.getCause(), actualError.getCause());
        Assert.assertEquals(expectedError.getMessage(), actualError.getMessage());
        Assert.assertEquals(expectedError.getApplicationTag(), actualError.getApplicationTag());
        Assert.assertEquals(expectedError.getErrorType(), actualError.getErrorType());
    }
}