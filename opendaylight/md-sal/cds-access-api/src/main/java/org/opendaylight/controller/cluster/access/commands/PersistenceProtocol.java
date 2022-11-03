/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.WritableObject;

/**
 * Enumeration of transaction persistence protocols. These govern which protocol is executed between the frontend
 * and backend to drive persistence of a particular transaction.
 */
public enum PersistenceProtocol implements WritableObject {
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
     * is the only entity which needs to persist its effects, hence a simple request/reply protocol is sufficient.
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
    },
    /**
     * Transaction is ready. This is not a really a persistence protocol, but an indication that frontend has
     * completed modifications on the transaction and considers it ready, without deciding the actual commit protocol.
     */
    READY {
        @Override
        byte byteValue() {
            return 4;
        }
    };

    @Override
    public final void writeTo(final DataOutput out) throws IOException {
        out.writeByte(byteValue());
    }

    public static PersistenceProtocol readFrom(final DataInput in) throws IOException {
        return valueOf(in.readByte());
    }

    abstract byte byteValue();

    static int byteValue(final PersistenceProtocol finish) {
        return finish == null ? 0 : finish.byteValue();
    }

    static @Nullable PersistenceProtocol valueOf(final byte value) {
        return switch (value) {
            case 0 -> null;
            case 1 -> ABORT;
            case 2 -> SIMPLE;
            case 3 -> THREE_PHASE;
            case 4 -> READY;
            default -> throw new IllegalArgumentException("Unhandled byte value " + value);
        };
    }
}
