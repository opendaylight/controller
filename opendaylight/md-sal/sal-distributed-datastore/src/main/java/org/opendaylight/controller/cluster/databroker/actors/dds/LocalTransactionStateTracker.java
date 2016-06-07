package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalTransactionStateTracker extends AbstractTransactionStateTracker {
    private static final Logger LOG = LoggerFactory.getLogger(LocalTransactionStateTracker.class);
    private static final Consumer<Response<TransactionIdentifier, ?>> ABORT_COMPLETER = response -> {
        LOG.debug("Abort completed with {}", response);
    };

    private final TransactionIdentifier identifier;
    private DataTreeModification modification;

    LocalTransactionStateTracker(final DistributedDataStoreClientBehavior client,
        final TransactionIdentifier identifier, final DataTree dataTree) {
        super(client);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.modification = dataTree.takeSnapshot().newModification();
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    void delete(final YangInstanceIdentifier path) {
        modification.delete(path);
    }

    @Override
    void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.merge(path, data);
    }

    @Override
    void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.write(path, data);
    }

    @Override
    CompletionStage<Boolean> exists(final YangInstanceIdentifier path) {
        return CompletableFuture.completedFuture(modification.readNode(path).isPresent());
    }

    @Override
    CompletionStage<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path) {
        return CompletableFuture.completedFuture(modification.readNode(path));
    }

    @Override
    void abort() {
        client().sendRequest(new AbortLocalTransactionRequest(identifier, 0, client().self()), ABORT_COMPLETER);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been aborted"));
    }

    @SuppressWarnings("unchecked")
    @Override
    CommitLocalTransactionRequest doCommit(final boolean coordinated) {
        modification.ready();

        final CommitLocalTransactionRequest ret = new CommitLocalTransactionRequest(identifier, 0, client().self(),
            modification, coordinated);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been submitted"));
        return ret;
    }
}
