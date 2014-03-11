package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

public interface AsyncReadWriteTransaction<P extends Path<P>, D> extends AsyncReadTransaction<P, D>,
        AsyncWriteTransaction<P, D> {

}
