package org.opendaylight.controller.md.sal.common.impl;

import org.opendaylight.controller.md.sal.common.api.routing.RoutedRegistration;
import org.opendaylight.yangtools.concepts.Path;

public abstract class AbstractRoutedRegistration<C, P extends Path<P>, S> extends AbstractRegistration<S> implements
        RoutedRegistration<C, P, S> {

    public AbstractRoutedRegistration(S instance) {
        super(instance);
    }
}
