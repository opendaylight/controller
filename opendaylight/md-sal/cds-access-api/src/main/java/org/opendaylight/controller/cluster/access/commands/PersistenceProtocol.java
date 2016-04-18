/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;

@Beta
public enum PersistenceProtocol {
    ABORT {
        @Override
        byte byteValue() {
            return 1;
        }
    },
    SIMPLE {
        @Override
        byte byteValue() {
            return 2;
        }
    },
    THREE_PHASE {
        @Override
        byte byteValue() {
            return 3;
        }
    };

    abstract byte byteValue();

    static int byteValue(final PersistenceProtocol finish) {
        return finish == null ? 0 : finish.byteValue();
    }

    static PersistenceProtocol valueOf(final byte b) {
        switch (b) {
            case 0:
                return null;
            case 1:
                return ABORT;
            case 2:
                return SIMPLE;
            case 3:
                return THREE_PHASE;
            default:
                throw new IllegalArgumentException("Unhandled byte value " + b);
        }
    }
}
