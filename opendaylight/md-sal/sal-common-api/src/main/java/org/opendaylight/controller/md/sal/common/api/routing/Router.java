package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.Map;
import java.util.Set;

public interface Router<C,P,D> extends //
        RouteChangePublisher<C, P> {

    Map<C, Set<P>> getAnnouncedPaths();
}
