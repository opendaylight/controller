package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

public interface AsyncDataTransactionFactory<P extends Path<P>, D> {

    AsyncReadTransaction<P, D> newReadOnlyTransaction();

    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    AsyncWriteTransaction<P,D> newWriteOnlyTransaction();

}
