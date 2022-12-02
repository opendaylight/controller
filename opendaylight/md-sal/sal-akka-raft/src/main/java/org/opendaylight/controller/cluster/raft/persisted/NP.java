/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;

/**
 * Serialization proxy for {@link NoopPayload}.
 */
// There is no need for Externalizable
final class NP implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @java.io.Serial
    private Object readResolve() {
        return NoopPayload.INSTANCE;
    }
}

