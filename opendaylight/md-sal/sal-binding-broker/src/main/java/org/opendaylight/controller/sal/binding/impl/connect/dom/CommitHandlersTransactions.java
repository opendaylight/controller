package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Collections;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class CommitHandlersTransactions {

    private static class AllwaysSuccessfulTransaction<P,D> implements DataCommitTransaction<P, D> {
        
        private final  DataModification<P, D> modification;

        public AllwaysSuccessfulTransaction(DataModification<P, D> modification) {
            this.modification = modification;
        }
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            return Rpcs.<Void>getRpcResult(true, null, Collections.<RpcError>emptyList());
        }
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            return Rpcs.<Void>getRpcResult(true, null, Collections.<RpcError>emptyList());
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
