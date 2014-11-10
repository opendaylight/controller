
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration;

import org.opendaylight.controller.sal.utils.Status;


/**
 * Listener Interface for receiving Configuration events.
 */
public interface IConfigurationAwareCommon {
    /**
     * Trigger from Configuration Service or Container Configuration Service to
     * persist the configuration state for this component on the local cluster
     * node
     */
    Status saveConfiguration();
}
