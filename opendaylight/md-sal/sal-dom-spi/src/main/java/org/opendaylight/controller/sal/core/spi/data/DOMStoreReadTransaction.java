package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public interface DOMStoreReadTransaction extends DOMStoreTransaction {

    ListenableFuture<Optional<NormalizedNode<?,?>>> read(InstanceIdentifier path);

}
