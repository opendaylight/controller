package org.opendaylight.controller.md.sal.common.api.routing;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

public interface RouteChangePublisher<C,P> {

    <L extends RouteChangeListener<C,P>> ListenerRegistration<L> registerRouteChangeListener(L listener);
}
