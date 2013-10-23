package org.opendaylight.controller.sal.binding.spi.remote;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

public interface RemoteRpcRouter {


    
    
    
    
    ListenerRegistration<RouteChangeListener> registerRouteChangeListener(RouteChangeListener listener);

    
    

}
