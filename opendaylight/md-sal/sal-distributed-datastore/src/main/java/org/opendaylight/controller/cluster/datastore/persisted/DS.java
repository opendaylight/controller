/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Serialization proxy for {@link DatastoreSnapshot}.
 */
final class DS implements DatastoreSnapshot.SerialForm {
    private static final long serialVersionUID = 1L;

    private DatastoreSnapshot datastoreSnapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public DS() {
        // For Externalizable
    }

    DS(final DatastoreSnapshot datastoreSnapshot) {
        this.datastoreSnapshot = requireNonNull(datastoreSnapshot);
    }

    @Override
    public DatastoreSnapshot datastoreSnapshot() {
        return datastoreSnapshot;
    }

    @Override
    public void resolveTo(final DatastoreSnapshot newDatastoreSnapshot) {
        datastoreSnapshot = requireNonNull(newDatastoreSnapshot);
    }

    @Override
    public Object readResolve() {
        return verifyNotNull(datastoreSnapshot);
    }
}
