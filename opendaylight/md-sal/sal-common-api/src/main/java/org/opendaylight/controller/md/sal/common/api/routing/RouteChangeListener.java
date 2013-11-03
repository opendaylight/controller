package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.EventListener;

public interface RouteChangeListener<C,P> extends EventListener {

    void onRouteChange(RouteChange<C, P> change);
}
