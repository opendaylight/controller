package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWriteTx implements DOMDataWriteTransaction {

    private static final Logger LOG  = LoggerFactory.getLogger(AbstractWriteTx.class);

    protected final RemoteDeviceId id;
    protected final NetconfBaseOps netOps;
    protected final boolean rollbackSupport;
    // Allow commit to be called only once
    protected boolean finished = false;

    public AbstractWriteTx(final NetconfBaseOps netOps, final RemoteDeviceId id, final boolean rollbackSupport) {
        this.netOps = netOps;
        this.id = id;
        this.rollbackSupport = rollbackSupport;
        init();
    }

    static boolean isSuccess(final DOMRpcResult result) {
        return result.getErrors().isEmpty();
    }

    protected void checkNotFinished() {
        Preconditions.checkState(!isFinished(), "%s: Transaction %s already finished", id, getIdentifier());
    }

    protected boolean isFinished() {
        return finished;
    }

    protected void invokeBlocking(final String msg, final Function<NetconfBaseOps, ListenableFuture<DOMRpcResult>> op) throws NetconfDocumentedException {
        try {
            final DOMRpcResult compositeNodeRpcResult = op.apply(netOps).get();
            if(isSuccess(compositeNodeRpcResult) == false) {
                throw new NetconfDocumentedException(id + ": " + msg + " failed: " + compositeNodeRpcResult.getErrors(), NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorSeverity.warning);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new NetconfDocumentedException(id + ": " + msg + " failed: " + e.getMessage(), e, NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorSeverity.warning);
        }
    }

    @Override
    public synchronized boolean cancel() {
        if(isFinished()) {
            return false;
        }

        finished = true;
        cleanup();
        return true;
    }

    protected abstract void init();

    protected abstract void cleanup();

    @Override
    public Object getIdentifier() {
        return this;
    }

    @Override
    public synchronized void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkEditable(store);

        // trying to write only mixin nodes (not visible when serialized). Ignoring. Some devices cannot handle empty edit-config rpc
        if(containsOnlyNonVisibleData(path, data)) {
            LOG.debug("Ignoring put for {} and data {}. Resulting data structure is empty.", path, data);
            return;
        }

        try {
            editConfig(
                    netOps.createEditConfigStrcture(Optional.<NormalizedNode<?, ?>>fromNullable(data), Optional.of(ModifyAction.REPLACE), path), Optional.of(ModifyAction.NONE));
        } catch (final NetconfDocumentedException e) {
            handleEditException(path, data, e, "putting");
        }
    }

    protected abstract void handleEditException(YangInstanceIdentifier path, NormalizedNode<?, ?> data, NetconfDocumentedException e, String editType);
    protected abstract void handleDeleteException(YangInstanceIdentifier path, NetconfDocumentedException e);

    @Override
    public synchronized void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkEditable(store);

        // trying to write only mixin nodes (not visible when serialized). Ignoring. Some devices cannot handle empty edit-config rpc
        if (containsOnlyNonVisibleData(path, data)) {
            LOG.debug("Ignoring merge for {} and data {}. Resulting data structure is empty.", path, data);
            return;
        }

        try {
            editConfig(
                    netOps.createEditConfigStrcture(Optional.<NormalizedNode<?, ?>>fromNullable(data), Optional.<ModifyAction>absent(), path), Optional.<ModifyAction>absent());
        } catch (final NetconfDocumentedException e) {
            handleEditException(path, data, e, "merge");
        }
    }

    /**
     * Check whether the data to be written consists only from mixins
     */
    private static boolean containsOnlyNonVisibleData(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        // There's only one such case:top level list (pathArguments == 1 && data is Mixin)
        // any other mixin nodes are contained by a "regular" node thus visible when serialized
        return path.getPathArguments().size() == 1 && data instanceof MixinNode;
    }

    @Override
    public synchronized void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkEditable(store);

        try {
            editConfig(
                    netOps.createEditConfigStrcture(Optional.<NormalizedNode<?, ?>>absent(), Optional.of(ModifyAction.DELETE), path), Optional.of(ModifyAction.NONE));
        } catch (final NetconfDocumentedException e) {
            handleDeleteException(path, e);
        }
    }

    @Override
    public final ListenableFuture<RpcResult<TransactionStatus>> commit() {
        checkNotFinished();
        finished = true;

        return performCommit();
    }

    protected abstract ListenableFuture<RpcResult<TransactionStatus>> performCommit();

    private void checkEditable(final LogicalDatastoreType store) {
        checkNotFinished();
        Preconditions.checkArgument(store == LogicalDatastoreType.CONFIGURATION, "Can edit only configuration data, not %s", store);
    }

    protected abstract void editConfig(DataContainerChild<?, ?> editStructure, Optional<ModifyAction> defaultOperation) throws NetconfDocumentedException;
}
