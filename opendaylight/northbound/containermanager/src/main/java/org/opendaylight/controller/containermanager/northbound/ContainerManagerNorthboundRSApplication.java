
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.northbound;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Instance of javax.ws.rs.core.Application used to return the classes
 * that will be instantiated for JAXRS processing, this is necessary
 * because the package scanning in jersey doesn't yet work in OSGi
 * environment.
 *
 */
public class ContainerManagerNorthboundRSApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ContainerManagerNorthbound.class);
        classes.add(JacksonJaxbJsonProvider.class);
        return classes;
    }
}
