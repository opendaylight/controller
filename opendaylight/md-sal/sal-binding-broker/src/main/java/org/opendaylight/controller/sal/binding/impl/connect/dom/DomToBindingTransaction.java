package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Deprecated
class DomToBindingTransaction implements
    DataCommitHandler.DataCommitTransaction<YangInstanceIdentifier, CompositeNode> {

    private final org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing;
    private final DataModification<YangInstanceIdentifier, CompositeNode> modification;
    private final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions;

    public DomToBindingTransaction(
    final org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing,
    final DataModification<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, CompositeNode> modification,
        ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions) {
        super();
        this.backing = backing;
        this.modification = modification;
        this.bindingOpenedTransactions = bindingOpenedTransactions;
        this.bindingOpenedTransactions.put(backing.getIdentifier(), this);
    }

    @Override
    public DataModification<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, CompositeNode> getModification() {
        return modification;
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        bindingOpenedTransactions.remove(backing.getIdentifier());
        return RpcResultBuilder.<Void> success().build();
    }

    @Override
    public RpcResult<Void> finish() throws IllegalStateException {
        Future<RpcResult<TransactionStatus>> result = backing.commit();
        try {
            RpcResult<TransactionStatus> baResult = result.get();
            bindingOpenedTransactions.remove(backing.getIdentifier());
            return RpcResultBuilder.<Void> status(baResult.isSuccessful())
                                          .withRpcErrors(baResult.getErrors()).build();
        } catch (InterruptedException e) {
            throw new IllegalStateException("", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("", e);
        } finally {
            bindingOpenedTransactions.remove(backing.getIdentifier());
        }
    }
}
