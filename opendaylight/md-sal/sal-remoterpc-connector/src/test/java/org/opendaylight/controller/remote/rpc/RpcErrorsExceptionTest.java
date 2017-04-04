/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.common.RpcError;

public class RpcErrorsExceptionTest {

    private static RpcErrorsException object;

    private final String errorMessage = "Test error message.";
    private Collection<RpcError> rpcErrors;

    @Mock
    private RpcError error1;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        rpcErrors = new ArrayList<>();
        rpcErrors.add(error1);
        object = new RpcErrorsException(errorMessage, rpcErrors);
    }

    @Test
    public void checkMessage() {
        String message = object.getMessage();
        assertTrue(errorMessage.equals(message));
    }

    @Test
    public void testGetRpcErrors() throws Exception {
        RpcError expectedError = rpcErrors.iterator().next();
        RpcError actualError = object.getRpcErrors().iterator().next();
        Assert.assertEquals(expectedError.getCause(), actualError.getCause());
        Assert.assertEquals(expectedError.getMessage(), actualError.getMessage());
        Assert.assertEquals(expectedError.getApplicationTag(), actualError.getApplicationTag());
        Assert.assertEquals(expectedError.getErrorType(), actualError.getErrorType());
    }
}