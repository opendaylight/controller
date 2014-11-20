/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * @deprecated Utility class for {@link DataCommitTransaction}, will be removed at the same time.
 */
@Deprecated
public class CommitHandlerTransactions {

    private static class AllwaysSuccessfulTransaction<P extends Path<P>,D> implements DataCommitTransaction<P, D> {

        private final  DataModification<P, D> modification;

        public AllwaysSuccessfulTransaction(DataModification<P, D> modification) {
            this.modification = modification;
        }
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            return RpcResultBuilder.<Void>success().build();
        }
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            return RpcResultBuilder.<Void>success().build();
        }

        @Override
        public DataModification<P, D> getModification() {
            return modification;
        }
    }

    public static final <P extends Path<P>,D> AllwaysSuccessfulTransaction<P, D> allwaysSuccessfulTransaction(DataModification<P, D> modification)  {
        return new AllwaysSuccessfulTransaction<>(modification);
    }
}
