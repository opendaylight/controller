package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.util.concurrent.ListenableFuture;

public interface DOMStoreWriteTransaction extends DOMStoreTransaction {

    void write(InstanceIdentifier path,NormalizedNode<?, ?> data);

    void delete(InstanceIdentifier path,NormalizedNode<?, ?> data);

    ListenableFuture<DOMStoreTransactionCommitCoordination> requestCommit();

}
