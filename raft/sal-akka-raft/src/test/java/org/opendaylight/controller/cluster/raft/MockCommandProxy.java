/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

record MockCommandProxy(String value, int size) implements Serializable {
    MockCommandProxy {
        requireNonNull(value);
    }

    @java.io.Serial
    private Object readResolve() {
        return new MockCommand(value, size);
    }
}