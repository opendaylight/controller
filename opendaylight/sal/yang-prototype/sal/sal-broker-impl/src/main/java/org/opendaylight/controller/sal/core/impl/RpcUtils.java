/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcError;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;


public class RpcUtils {

    Callable<RpcResult<CompositeNode>> callableFor(
            final RpcImplementation implemenation, final QName rpc,
            final CompositeNode input) {

        return new Callable<RpcResult<CompositeNode>>() {

            @Override
            public RpcResult<CompositeNode> call() throws Exception {
                return implemenation.invokeRpc(rpc, input);
            }
        };
    }

    public static <T> RpcResult<T> getRpcResult(boolean successful, T result,
            List<RpcError> errors) {
        RpcResult<T> ret = new RpcResultTO<T>(successful, result, errors);
        return ret;
    }

    private static class RpcResultTO<T> implements RpcResult<T> {

        private final Collection<RpcError> errors;
        private final T result;
        private final boolean successful;

        public RpcResultTO(boolean successful, T result, List<RpcError> errors) {
            this.successful = successful;
            this.result = result;
            this.errors = Collections.unmodifiableList(new ArrayList<RpcError>(
                    errors));
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }

        @Override
        public T getResult() {
            return result;
        }

        @Override
        public Collection<RpcError> getErrors() {
            return errors;
        }

    }
}

