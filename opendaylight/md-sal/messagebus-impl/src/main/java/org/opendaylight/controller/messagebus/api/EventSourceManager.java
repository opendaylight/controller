/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.api;
import java.util.List;

import org.opendaylight.controller.messagebus.registry.EventSourceRegistration;

/**
 * @author madamjak
 *
 */
public interface EventSourceManager {

    /**
     * Get registration objects for all events sources 
     * that have been registered by EventSourceRegistry.registerEventSource
     * @return
     */
    List<EventSourceRegistration> getEventSourceRegistrations();

}