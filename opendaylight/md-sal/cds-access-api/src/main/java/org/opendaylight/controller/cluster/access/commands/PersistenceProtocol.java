/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;

/**
 * Enumeration of transaction persistence protocols. These govern which protocol is executed between the frontend
 * and backend to drive persistence of a particular transaction.
 */
@Beta
public enum PersistenceProtocol {
    /**
     * Abort protocol. The transaction has been aborted on the frontend and its effects should not be visible
     * in the global history. This is a simple request/reply protocol.
     */
    ABORT {
        @Override
        byte byteValue() {
            return 1;
        }
    },
    /**
     * Simple commit protocol. The transaction should be committed to the global history. The receiving backend
     * it the only entity which needs to persist its effects, hence a simple request/reply protocol is sufficient.
     */
    SIMPLE {
        @Override
        byte byteValue() {
            return 2;
        }
    },
    /**
     * Three-phase commit protocol (3PC). The transaction should be committed to the global history, but it is a part
     * of a transaction spanning multiple entities and coordination is needed to drive persistence.
     */
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
