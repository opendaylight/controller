/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.opendaylight.controller.cluster.raft.spi.AbstractStateCommand;

public final class MockCommand extends AbstractStateCommand {
    @java.io.Serial
    private static final long serialVersionUID = 3121380393130864247L;

    private final String data;
    private final int size;

    public MockCommand(final String data) {
        this(data, data.length());
    }

    public MockCommand(final String data, final int size) {
        this.data = requireNonNull(data);
        this.size = size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int serializedSize() {
        return size;
    }

    @Override
    protected Object writeReplace() {
        return new MockCommandProxy(data, size);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof MockCommand other && size == other.size && data.equals(other.data);
    }

    @Override
    public String toString() {
        return data;
    }
}
