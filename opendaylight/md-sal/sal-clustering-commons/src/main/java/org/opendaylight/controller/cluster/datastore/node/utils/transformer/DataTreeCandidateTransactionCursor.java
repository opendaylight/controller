/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.Beta;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.BackendFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

/**
 * Helper interface for applying a {@link DataTreeCandidate} in streamed form onto a {@link DataTreeModificationCursor}.
 * This interface does not expose its ties to either interface, but semantically combines the walking part of a cursor
 * and receives data in the form of a {@link NormalizedNodeStream}.
 */
@Beta
public interface DataTreeCandidateTransactionCursor {
    @FunctionalInterface
    interface NormalizedNodeStream {

        void writeTo(@NonNull NormalizedNodeStreamWriter writer) throws IOException;
    }

    /**
     * Move the cursor to the specified child of the current position.
     *
     * @param child Child identifier
     * @throws BackendFailedException when an implementation-specific error occurs while servicing the request.
     * @throws IllegalArgumentException when specified identifier does not identify a valid child, or if that child is
     *                                  not an instance of {@link NormalizedNodeContainer}.
     * @throws NullPointerException if {@code child} is null
     */
    void enter(@NonNull PathArgument child);

    /**
     * Move the cursor up to the parent of current position.
     *
     * @throws IllegalStateException when exiting would violate containment, typically by attempting to exit more levels
     *                               than previously  entered.
     */
    void exit();

    /**
     * Delete the specified child.
     *
     * @param child Child identifier
     * @throws BackendFailedException when implementation-specific errors occurs while servicing the request.
     * @throws NullPointerException if {@code child} is null
     */
    void delete(@NonNull PathArgument child);

    /**
     * Merge the specified data with the currently-present data
     * at specified path.
     *
     * @param child Child identifier
     * @param data Data to be merged
     * @throws BackendFailedException when implementation-specific errors occurs while servicing the request.
     * @throws IOException if the dataStream throws it
     * @throws NullPointerException if any argument is null
     */
    void merge(@NonNull PathArgument child, @NonNull NormalizedNodeStream dataStream) throws IOException;

    /**
     * Replace the data at specified path with supplied data.
     *
     * @param child Child identifier
     * @param data New node data
     * @throws BackendFailedException when implementation-specific errors occurs while servicing the request.
     * @throws IOException if the dataStream throws it
     * @throws NullPointerException if any argument is null
     */
    void write(@NonNull PathArgument child, @NonNull NormalizedNodeStream dataStream) throws IOException;
}
