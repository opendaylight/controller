/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
