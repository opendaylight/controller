package org.opendaylight.controller.md.sal.common.api.routing;

import org.opendaylight.yangtools.concepts.Immutable;

public interface Route<C,P> extends Immutable {

    C getType();
    
    P getPath();
}
