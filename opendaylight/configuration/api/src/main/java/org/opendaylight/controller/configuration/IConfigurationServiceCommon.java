
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
 * Common configuration interface for Configuration Service and Container
 * Configuration Service
 */
public interface IConfigurationServiceCommon {
    /**
     * Represent the trigger to save the controller configuration cluster wide.
     * When called on IConfigurationService, it will trigger a cluster wide save
     * configuration event for all the global instance components and all
     * components in all containers. When called on
     * IContainerConfigurationService, it will trigger a cluster wide save
     * configuration event for all components in the current container.
     *
     * @return the Status object representing the result of the saving request
     */
    Status saveConfigurations();
}
