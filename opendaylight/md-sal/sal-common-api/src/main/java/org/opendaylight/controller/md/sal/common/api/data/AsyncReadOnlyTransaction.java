package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

public interface AsyncReadOnlyTransaction<P extends Path<P>, D> extends AsyncTransaction<P,D> {

    /**
     *
     * Closes transaction and resources allocated with it.
     *
     */
    @Override
    public void close();
}
