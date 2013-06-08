
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.util.List;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Container Manager interface
 *
 *
 */
public interface IContainerManager {

    /**
     * Returns a list of Containers that currently exist.
     *
     * @return	array of String Container names
     */
    public boolean hasNonDefaultContainer();

    /**
     * Returns a list of Containers that currently exist.
     *
     * @return	array of String Container names
     */
    public List<String> getContainerNames();

    /**
     * Save the current container configuration to disk.
     * TODO : REMOVE THIS FUNCTION and make Save as a service rather than the
     * current hack of calling individual save routines.
     *
     * @return	status code
     */
    public Status saveContainerConfig();
}
