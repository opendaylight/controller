/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * A command for the RAFT Finite State Machine. It can be either a {@link RaftCommand} or a {@link StateCommand}.
 */
@NonNullByDefault
public sealed interface StateMachineCommand extends Immutable permits RaftCommand, StateCommand, Payload {
    /**
     * Read a {@link StateMachineCommand} from a {@link DataInput}.
     *
     * @param <T> the {@link StateMachineCommand} type
     */
    @FunctionalInterface
    interface Reader<T extends StateMachineCommand> {
        /**
         * Read a {@link StateMachineCommand} from a {@link DataInput}.
         *
         * @param in the {@link DataInput}
         * @return the {@link StateMachineCommand}
         * @throws IOException if an I/O error occurs
         */
        T readCommand(DataInput in) throws IOException;
    }

    /**
     * Write a {@link StateMachineCommand} to a {@link DataOutput}.
     *
     * @param <T> the {@link StateMachineCommand} type
     */
    @FunctionalInterface
    interface Writer<T extends StateMachineCommand> {
        /**
         * Write a {@link StateMachineCommand} from a {@link DataOutput}.
         *
         * @param command the {@link StateMachineCommand}
         * @param out the {@link DataOutput}
         * @throws IOException if an I/O error occurs
         */
        void writeCommand(T command, DataOutput out) throws IOException;
    }

    /**
     * A combination of a {@link Reader} and a {@link Writer} for a concrete {@link StateMachineCommand} type, as
     * indicated by {@link #commandType()}.
     *
     * @param <T> the {@link StateMachineCommand} type
     */
    interface Support<T extends StateMachineCommand> {
        /**
         * Returns the {@link StateMachineCommand} type supported by this {@link Support}.
         *
         * @return the {@link StateMachineCommand} type supported by this {@link Support}
         */
        Class<T> commandType();

        /**
         * Returns the {@link Reader}.
         *
         * @return the reader
         */
        Reader<T> reader();

        /**
         * Returns the {@link Writer}.
         *
         * @return the writer
         */
        Writer<T> writer();
    }

    /**
     * Returns the {@link Serializable} form. Returned object must {@code readResolve()} into an equivalent object.
     *
     * @return the {@link Serializable} form
     */
    @Beta
    // FIXME: CONTROLLER-2044: this should be handled by separate serialization support/protocol
    Serializable toSerialForm();
}
