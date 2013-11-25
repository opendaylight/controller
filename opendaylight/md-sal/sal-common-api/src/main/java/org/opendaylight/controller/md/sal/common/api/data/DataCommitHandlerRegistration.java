package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;

public interface DataCommitHandlerRegistration<P extends Path<P>,D> extends Registration<DataCommitHandler<P, D>>{

    P getPath();
}
