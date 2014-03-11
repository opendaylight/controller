package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.util.concurrent.ListenableFuture;


public interface DOMStoreTransactionCommitCoordination {

    ListenableFuture<Void> abort();

    ListenableFuture<Void> commit();
}
