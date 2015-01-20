package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.createEditConfigStructure;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class AbstractWriteTx implements DOMDataWriteTransaction {
    protected final RemoteDeviceId id;
    protected final NetconfBaseOps netOps;
    protected final DataNormalizer normalizer;
    protected final NetconfSessionPreferences netconfSessionPreferences;
    // Allow commit to be called only once
    protected boolean finished = false;

    public AbstractWriteTx(final NetconfBaseOps netOps, final RemoteDeviceId id, final DataNormalizer normalizer, final NetconfSessionPreferences netconfSessionPreferences) {
        this.netOps = netOps;
        this.id = id;
        this.normalizer = normalizer;
        this.netconfSessionPreferences = netconfSessionPreferences;
        init();
    }

    protected void checkNotFinished() {
        Preconditions.checkState(!isFinished(), "%s: Transaction %s already finished", id, getIdentifier());
    }

    protected boolean isFinished() {
        return finished;
    }

    protected void invokeBlocking(final String msg, final Function<NetconfBaseOps, ListenableFuture<RpcResult<CompositeNode>>> op) throws NetconfDocumentedException {
        try {
            final RpcResult<CompositeNode> compositeNodeRpcResult = op.apply(netOps).get(1L, TimeUnit.MINUTES);
            if(compositeNodeRpcResult.isSuccessful() == false) {
                throw new NetconfDocumentedException(id + ": " + msg + " failed: " + compositeNodeRpcResult.getErrors(), NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorSeverity.warning);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (final ExecutionException | TimeoutException e) {
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

        try {
            final YangInstanceIdentifier legacyPath = ReadOnlyTx.toLegacyPath(normalizer, path, id);
            final CompositeNode legacyData = normalizer.toLegacy(path, data);
            editConfig(
                    createEditConfigStructure(legacyPath, Optional.of(ModifyAction.REPLACE), Optional.fromNullable(legacyData)), Optional.of(ModifyAction.NONE));
        } catch (final NetconfDocumentedException e) {
            handleEditException(path, data, e, "putting");
        }
    }

    protected abstract void handleEditException(YangInstanceIdentifier path, NormalizedNode<?, ?> data, NetconfDocumentedException e, String editType);
    protected abstract void handleDeleteException(YangInstanceIdentifier path, NetconfDocumentedException e);

    @Override
    public synchronized void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkEditable(store);

        try {
            final YangInstanceIdentifier legacyPath = ReadOnlyTx.toLegacyPath(normalizer, path, id);
            final CompositeNode legacyData = normalizer.toLegacy(path, data);
            editConfig(
                    createEditConfigStructure(legacyPath, Optional.<ModifyAction>absent(), Optional.fromNullable(legacyData)), Optional.<ModifyAction>absent());
        } catch (final NetconfDocumentedException e) {
            handleEditException(path, data, e, "merge");
        }
    }

    @Override
    public synchronized void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkEditable(store);

        try {
            editConfig(createEditConfigStructure(
                    ReadOnlyTx.toLegacyPath(normalizer, path, id), Optional.of(ModifyAction.DELETE),
                    Optional.<CompositeNode>absent()), Optional.of(ModifyAction.NONE));
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

    protected abstract void editConfig(CompositeNode editStructure, Optional<ModifyAction> defaultOperation) throws NetconfDocumentedException;
}
