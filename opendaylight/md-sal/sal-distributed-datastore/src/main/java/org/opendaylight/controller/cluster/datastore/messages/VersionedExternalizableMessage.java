/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Abstract base class for a versioned Externalizable message.
 *
 * @author Thomas Pantelis
 */
public abstract class VersionedExternalizableMessage implements Externalizable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private ABIVersion version;

    public VersionedExternalizableMessage() {
        // For Externalizable
    }

    protected VersionedExternalizableMessage(final ABIVersion version) {
        if (ABIVersion.current().compareTo(version) > 0) {
            this.version = ABIVersion.current();
        } else {
            this.version = Preconditions.checkNotNull(version);
        }
    }

    public final ABIVersion getVersion() {
        return version;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = ABIVersion.read(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version.shortValue());
    }

    @Override
    public final Object toSerializable() {
        if (ABIVersion.BORON.compareTo(version) > 0) {
            throw new UnsupportedOperationException("Versions prior to " + ABIVersion.BORON + " are not supported");
        }

        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [version=" + getVersion() + "]";
    }
}
