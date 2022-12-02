/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Abstract base that implements Externalizable with no-op methods that is intended for classes that use the
 * externalizable proxy pattern but have no data to serialize and read-resolve to a static instance.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "7.0.0", forRemoval = true)
public abstract class EmptyExternalizableProxy implements Externalizable {
    private static final long serialVersionUID = 1L;

    private final Object readResolveTo;

    protected EmptyExternalizableProxy(final Object readResolveTo) {
        this.readResolveTo = requireNonNull(readResolveTo);
    }

    @Override
    public void writeExternal(final ObjectOutput out) {
    }

    @Override
    public void readExternal(final ObjectInput in) {
    }

    protected Object readResolve() {
        return readResolveTo;
    }
}
