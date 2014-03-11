/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

public enum LogicalDatastoreType {

    /**
     * Logical atastore representing operational state of the system
     * and it's components
     *
     * This datastore is used to describe operational state of
     * the system and it's operation related data.
     *
     */
    OPERATIONAL,
    /**
     * Logical Datastore representing configuration state of the system
     * and it's components.
     *
     * This datastore is used to describe intended state of
     * the system and intended operation mode.
     *
     */
    CONFIGURATION

}
