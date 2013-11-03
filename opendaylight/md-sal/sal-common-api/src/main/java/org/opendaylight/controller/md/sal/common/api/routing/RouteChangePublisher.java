package org.opendaylight.controller.md.sal.common.api.routing;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

public interface RouteChangePublisher<C,P> {

    ListenerRegistration<RouteChangeListener<C,P>> registerRouteChangeListener(RouteChangeListener<C,P> listener);
}
