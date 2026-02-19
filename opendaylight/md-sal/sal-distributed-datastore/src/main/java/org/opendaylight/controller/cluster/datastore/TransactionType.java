/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

public enum TransactionType {
    READ_ONLY,
    WRITE_ONLY,
    READ_WRITE;

    public static TransactionType fromInt(final int type) {
        return switch (type) {
            case 0 -> READ_ONLY;
            case 1 -> WRITE_ONLY;
            case 2 -> READ_WRITE;
            default -> throw new IllegalArgumentException("In TransactionType enum value " + type);
        };
    }
}
