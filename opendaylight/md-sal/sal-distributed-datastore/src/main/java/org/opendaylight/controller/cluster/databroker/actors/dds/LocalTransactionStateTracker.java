package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class LocalTransactionStateTracker extends AbstractTransactionStateTracker {
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
        // FIXME: send to backend?

        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been aborted"));
    }
}
