package org.opendaylight.controller.sal.binding.spi.remote;

import java.util.EventListener;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface RouteChangeListener extends EventListener {

    void onRouteChange(RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change);

}
