/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.mapping.osgi;

import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.Module;


/**
 * Listener for base netconf notifications defined in https://tools.ietf.org/html/rfc6470.
 * This listener uses generated classes from yang model defined in RFC6470.
 * It alleviates the provisioning of base netconf notifications from the code.
 */
public interface BaseNetconfNotificationListener {

    /**
     * Callback used to notify about a change in used capabilities
     */
    void onCapabilityChanged(Set<Module> added, Set<Module> removed);

    // TODO add other base notifications

}
