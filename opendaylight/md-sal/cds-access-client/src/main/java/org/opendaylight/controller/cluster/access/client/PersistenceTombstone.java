/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Persistence marker indicating that generation tracking has been moved to state file.
 *
 * @param clientId the ClientIdentifier which performed the migration
 */
@NonNullByDefault
record PersistenceTombstone(ClientIdentifier clientId) implements Serializable {
    PersistenceTombstone {
        requireNonNull(clientId);
    }
}
