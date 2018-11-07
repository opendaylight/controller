/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public enum LogicalDatastoreType {
    /**
     * Logical atastore representing operational state of the system
     * and it's components
     *
     * <p>
     * This datastore is used to describe operational state of
     * the system and it's operation related data.
     *
     */
    OPERATIONAL {
        @Override
        public org.opendaylight.mdsal.common.api.LogicalDatastoreType toMdsal() {
            return org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;
        }
    },
    /**
     * Logical Datastore representing configuration state of the system
     * and it's components.
     *
     * <p>
     * This datastore is used to describe intended state of
     * the system and intended operation mode.
     *
     */
    CONFIGURATION {
        @Override
        public org.opendaylight.mdsal.common.api.LogicalDatastoreType toMdsal() {
            return org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
        }
    };

    /**
     * Convert this logical datastore type to its MD-SAL counterpart.
     *
     * @return MD-SAL counterpart of this type.
     */
    public abstract org.opendaylight.mdsal.common.api.LogicalDatastoreType toMdsal();

    /**
     * Convert MD-SAL logical datastore type to this counterpart.
     *
     * @param type MD-SAL counterpart of this type.
     * @return Corresponding value in this type.
     */
    public static LogicalDatastoreType fromMdsal(final org.opendaylight.mdsal.common.api.LogicalDatastoreType type) {
        switch (type) {
            case CONFIGURATION:
                return CONFIGURATION;
            case OPERATIONAL:
                return OPERATIONAL;
            default:
                throw new IllegalArgumentException("Unhandled type " + type);
        }
    }
}
