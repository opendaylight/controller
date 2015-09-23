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
    OPERATIONAL(org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL),
    /**
     * Logical Datastore representing configuration state of the system
     * and it's components.
     *
     * This datastore is used to describe intended state of
     * the system and intended operation mode.
     *
     */
    CONFIGURATION(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION);

    private final org.opendaylight.mdsal.common.api.LogicalDatastoreType newValue;

    private LogicalDatastoreType(org.opendaylight.mdsal.common.api.LogicalDatastoreType newValue) {
        this.newValue = newValue;
    }

    public org.opendaylight.mdsal.common.api.LogicalDatastoreType asNew() {
        return this.newValue;
    }

    public LogicalDatastoreType fromNew(org.opendaylight.mdsal.common.api.LogicalDatastoreType newValue) {
        switch (newValue) {
            case CONFIGURATION:
                return CONFIGURATION;
            case OPERATIONAL:
                return OPERATIONAL;
            default:
                throw new IllegalArgumentException("Not supported datastore type " + newValue);
        }
    }

}
