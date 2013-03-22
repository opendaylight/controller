
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IContainerListener.java
 *
 * @brief  Set of methods needed to listen to changes in the Container
 * configuration
 *
 * Set of methods needed to listen to changes in the Container
 * configuration
 *
 *
 */
package org.opendaylight.controller.sal.core;

/**
 *
 * Interface used to retrieve the status of a given Container
 */
public interface IContainerListener {
    /**
     * Called to notify a change in the tag assigned to a switch
     *
     * @param containerName container for which the update has been raised
     * @param n Node of the tag under notification
     * @param oldTag previous version of the tag, this differ from the
     * newTag only if the UpdateType is a modify
     * @param newTag new value for the tag, different from oldTag only
     * in case of modify operation
     * @param t type of update
     */
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t);

    /**
     * Notification raised when the container flow layout changes
     *
     * @param containerName container for which the update has been raised
     * @param previousFlow previous value of the container flow under
     * update, differs from the currentFlow only and only if it's an
     * update operation
     * @param currentFlow current version of the container flow differs from
     * the previousFlow only in case of update
     * @param t type of update
     */
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t);

    /**
     * Notification raised when a NodeConnector is added or removed in
     * the container.
     *
     * @param containerName container for which the update has been raised
     * @param p NodeConnector being updated
     * @param t type of modification, but among the types the modify
     * operation is not expected to be raised because the
     * nodeConnectors are anyway immutable so this is only used to
     * add/delete
     */
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType t);

    /**
     * Notification raised when the container mode has changed
     * This notification is needed for some bundle in the default container
     * to cleanup some HW state when switching from non-slicing to
     * slicing case and vice-versa
     *
     * @param t  ADDED when first container is created, REMOVED when last container is removed
     */
    public void containerModeUpdated(UpdateType t);
}
