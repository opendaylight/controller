
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.Set;

import org.opendaylight.controller.sal.core.Property;

/**
 *
 * Interface for Inventory Service visible inside the protocol plugin only
 *
 *
 *
 */
public interface IOFInventoryService {
    /**
     * Tell Inventory Service a property has been updated
     * for the specified switch with the specified value
     *
     * @param switchId
     */
    public void updateSwitchProperty(Long switchId, Set<Property> propertySet);
}
