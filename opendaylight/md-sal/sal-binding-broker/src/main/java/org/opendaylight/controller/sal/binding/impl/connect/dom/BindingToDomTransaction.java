package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@Deprecated
class BindingToDomTransaction implements
    DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final DataModificationTransaction backing;
    private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;
    private final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions;

    public BindingToDomTransaction(final DataModificationTransaction backing,
        final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,
        ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions) {
        this.backing = backing;
        this.modification = modification;
        this.domOpenedTransactions = domOpenedTransactions;
        this.domOpenedTransactions.put(backing.getIdentifier(), this);
    }

    @Override
    public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
        return modification;
    }

    @Override
    public RpcResult<Void> finish() throws IllegalStateException {
        Future<RpcResult<TransactionStatus>> result = backing.commit();
        try {
            RpcResult<TransactionStatus> biResult = result.get();
            domOpenedTransactions.remove(backing.getIdentifier());
            return RpcResultBuilder.<Void> status(biResult.isSuccessful())
                                             .withRpcErrors(biResult.getErrors()).build();
        } catch (InterruptedException e) {
            throw new IllegalStateException("", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("", e);
        } finally {
            domOpenedTransactions.remove(backing.getIdentifier());
        }
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        domOpenedTransactions.remove(backing.getIdentifier());
        return RpcResultBuilder.<Void> success().build();
    }
}
