
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.internal;

import org.opendaylight.controller.containermanager.ContainerData;

/**
 * @file   IContainerInternal.java
 *
 * @brief  Interface to export internal container manager data to friend classes
 *
 * Interface to export internal container manager data to friend classes
 */

interface IContainerInternal {
    /**
     * Return a reference to containerData if available so a friend class
     * can extract all the data and cook them up.
     *
     * @param containerName ContainerName for which we want to export the data
     *
     * @return null if containerName doesn't exist or a reference to
     * ContainerData if exists
     */
    ContainerData getContainerData(String containerName);
}
