/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.loadbalancer.northbound;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * This class is an instance of javax.ws.rs.core.Application and is used to return the classes
 * that will be instantiated for JAXRS processing. This is necessary
 * because package scanning in jersey doesn't yet work in OSGi environment.
 *
 */
public class LoadBalancerNorthboundRSApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(LoadBalancerNorthbound.class);
        return classes;
    }
}
