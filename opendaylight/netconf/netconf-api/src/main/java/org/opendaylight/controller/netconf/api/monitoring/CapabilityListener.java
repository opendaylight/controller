/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api.monitoring;

import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;

/**
 * Created by mmarsale on 13.2.2015.
 */
public interface CapabilityListener {
    void onCapabilitiesAdded(Set<Capability> addedCaps);

    void onCapabilitiesRemoved(Set<Capability> addedCaps);
}
