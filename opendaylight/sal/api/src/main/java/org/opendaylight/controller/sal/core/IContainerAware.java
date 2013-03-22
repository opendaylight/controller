
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

/**
 * @file   IContainerAware.java
 *
 * @brief  Define the interface to be called when the Container is being
 * created/destroyed
 *
 *
 */

public interface IContainerAware {
    /**
     * Method invoked to signal that a container is being created
     *
     * @param containerName Container being created
     */
    public void containerCreate(String containerName);

    /**
     * Method invoked to signal that a container is being destroyed
     *
     * @param containerName Container being destroyed
     */
    public void containerDestroy(String containerName);
}
