package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

public interface DataTransactionFactory<P extends Path<P>, D> {

    AsyncReadOnlyTransaction<P, D> newReadOnlyTransaction();

    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

}
