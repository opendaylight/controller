/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.spi.AbstractEffectiveModelContextProvider;

/**
 * A {@link CursorAwareDataTreeModification} which does not really do anything and throws an
 * {@link FailedDataTreeModificationException} for most of its operations. Used in case we when
 * {@link DataTreeSnapshot#newModification()} fails, see {@link LocalReadWriteProxyTransaction} for details. Surrounding
 * code should guard against invocation of most of these methods.
 */
final class FailedDataTreeModification extends AbstractEffectiveModelContextProvider
        implements CursorAwareDataTreeModification {
    private final @NonNull Exception cause;

    FailedDataTreeModification(final EffectiveModelContext context, final Exception cause) {
        super(context);
        this.cause = requireNonNull(cause);
    }

    @NonNull Exception cause() {
        return cause;
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        throw ex();
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode data) {
        throw ex();
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode data) {
        throw ex();
    }

    @Override
    public void ready() {
        // No-op
    }

    @Override
    public void applyToCursor(final DataTreeModificationCursor cursor) {
        throw ex();
    }

    @Override
    public Optional<NormalizedNode> readNode(final YangInstanceIdentifier path) {
        throw ex();
    }

    @Override
    public CursorAwareDataTreeModification newModification() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<? extends DataTreeModificationCursor> openCursor(final YangInstanceIdentifier path) {
        throw ex();
    }

    private @NonNull FailedDataTreeModificationException ex() {
        return new FailedDataTreeModificationException(cause);
    }
}
