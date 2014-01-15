package org.opendaylight.controller.sal.binding.impl.forward;

import com.google.common.base.Preconditions;

public class DomForwardingUtils {

    public static boolean isDomForwardedBroker(Object obj) {
        return obj instanceof DomForwardedBroker;
    }

    public static void reuseForwardingFrom(Object target,Object source) {
        Preconditions.checkArgument(isDomForwardedBroker(source));
        Preconditions.checkArgument(isDomForwardedBroker(target));
        DomForwardedBroker forwardedSource = (DomForwardedBroker) source;
        DomForwardedBroker forwardedTarget = (DomForwardedBroker) target;
        reuseForwardingFrom(forwardedTarget, forwardedSource);
        
    }

    private static void reuseForwardingFrom(DomForwardedBroker target, DomForwardedBroker source) {
        target.setConnector(source.getConnector());
        target.setDomProviderContext(source.getDomProviderContext());
    }

}
