
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ICoordinatorChangeAware.java
 *
 *
 * @brief  Interface that needs to be implemented by who wants to be
 * notified of coordinator role change
 *
 */
package org.opendaylight.controller.clustering.services;

/**
 * Interface that needs to be implemented by who wants to be
 * notified of coordinator role change
 *
 */
public interface ICoordinatorChangeAware {

    /**
     * Function that will be called when there is the event of
     * coordinator change in the cluster.
     */
    public void coordinatorChanged();
}
