
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration;

/**
 * Container configuration service
 */
public interface IConfigurationContainerService extends IConfigurationServiceCommon {

    /**
     * Bundle will call this function to ask ContainerConfigurationService to provide the
     * directory location of container
     *
     * @return The path to active container directory
     */
    public String getConfigurationRoot();

    /**
     * Function checks whether there exists a saved configuration for this container
     * (This is essentially checking whether the container root config directory exists)
     *
     * @return True iff container config has been saved at least once
     */
    public boolean hasBeenSaved();

}
