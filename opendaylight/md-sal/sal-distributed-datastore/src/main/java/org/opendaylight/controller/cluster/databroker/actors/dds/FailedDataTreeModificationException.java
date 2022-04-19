/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

/**
 * A box {@link RuntimeException} thrown by {@link FailedDataTreeModification} from its user-facing methods.
 */
final class FailedDataTreeModificationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    FailedDataTreeModificationException(final Exception cause) {
        super(null, requireNonNull(cause), false, false);
    }
}
