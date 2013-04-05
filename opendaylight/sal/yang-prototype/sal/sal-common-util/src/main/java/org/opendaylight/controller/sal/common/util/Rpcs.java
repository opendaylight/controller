/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.yang.common.RpcError;
import org.opendaylight.controller.yang.common.RpcResult;

public class Rpcs {
    public static <T> RpcResult<T> getRpcResult(boolean successful, T result,
            Collection<RpcError> errors) {
        RpcResult<T> ret = new RpcResultTO<T>(successful, result, errors);
        return ret;
    }

    private static class RpcResultTO<T> implements RpcResult<T> {

        private final Collection<RpcError> errors;
        private final T result;
        private final boolean successful;

        public RpcResultTO(boolean successful, T result,
                Collection<RpcError> errors) {
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
