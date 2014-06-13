package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * TransactionProxy acts as a proxy for one or more transactions that were created on a remote shard
 *
 * Creating a transaction on the consumer side will create one instance of a transaction proxy. If during
 * the transaction reads and writes are done on data that belongs to different shards then a separate transaction will
 * be created on each of those shards by the TransactionProxy
 *
 * The TransactionProxy does not make any guarantees about atomicity or order in which the transactions on the various
 * shards will be executed.
 *
 */
public class TransactionProxy implements DOMStoreReadWriteTransaction {
    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(InstanceIdentifier path) {
        throw new UnsupportedOperationException("read");
    }

    @Override
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void merge(InstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("merge");
    }

    @Override
    public void delete(InstanceIdentifier path) {
        throw new UnsupportedOperationException("delete");
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        throw new UnsupportedOperationException("ready");
    }

    @Override
    public Object getIdentifier() {
        throw new UnsupportedOperationException("getIdentifier");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close");
    }
}
