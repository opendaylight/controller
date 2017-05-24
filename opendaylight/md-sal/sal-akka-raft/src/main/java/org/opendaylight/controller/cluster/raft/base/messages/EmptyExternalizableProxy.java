/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Abstract base that implements Externalizable with no-op methods that is intended for classes that use the
 * externalizable proxy pattern but have no data to serialize and read-resolve to a static instance.
 *
 * @author Thomas Pantelis
 */
public abstract class EmptyExternalizableProxy implements Externalizable {
    private static final long serialVersionUID = 1L;

    private final Object readResolveTo;

    protected EmptyExternalizableProxy(Object readResolveTo) {
        this.readResolveTo = Preconditions.checkNotNull(readResolveTo);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }

    protected Object readResolve() {
        return readResolveTo;
    }
}
