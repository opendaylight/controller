/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.connectionmanager;

/*
 *
 *  This enum enables the options which could be used for querying
 *  the cluster-members based on the state-change permissions.
 *
 *  SB Plugin or any other means could be used to store this metadata
 *  against each connection w.r.t. Node
 *
 *  TODO: When connection-manager is enhanced later to store connection
 *  metadata with permissions, this could be used to query the connections
 *  based on permission-level of the connection
 *
 */

public enum NodeAccessPermission {

    /*
     * This option could be used to retrieve the cluster-members who have rights
     * only to read the Node state but are not eligible for modifying the state
     * of Node
     */
    READONLY_ACCESS,

    /*
     * This option could be used to retrieve the cluster-members who have rights
     * to read and modify the state of Node
     */
    READWRITE_ACCESS

}
