/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;

/**
 * Abstract base class for a versioned Externalizable message.
 *
 * @author Thomas Pantelis
 */
public abstract class VersionedExternalizableMessage implements Externalizable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private short version = DataStoreVersions.CURRENT_VERSION;

    public VersionedExternalizableMessage() {
    }

    public VersionedExternalizableMessage(final short version) {
        this.version = version <= DataStoreVersions.CURRENT_VERSION ? version : DataStoreVersions.CURRENT_VERSION;
    }

    public short getVersion() {
        return version;
    }

    protected final @NonNull NormalizedNodeStreamVersion getStreamVersion() {
        if (version >= DataStoreVersions.MAGNESIUM_VERSION) {
            return NormalizedNodeStreamVersion.MAGNESIUM;
        } else if (version == DataStoreVersions.SODIUM_SR1_VERSION) {
            return NormalizedNodeStreamVersion.SODIUM_SR1;
        } else if (version == DataStoreVersions.NEON_SR2_VERSION) {
            return NormalizedNodeStreamVersion.NEON_SR2;
        } else {
            return NormalizedNodeStreamVersion.LITHIUM;
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(version);
    }

    @Override
    public final Object toSerializable() {
        final short ver = getVersion();
        if (ver < DataStoreVersions.BORON_VERSION) {
            throw new UnsupportedOperationException("Version " + ver
                + " is older than the oldest version supported version " + DataStoreVersions.BORON_VERSION);
        }

        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [version=" + getVersion() + "]";
    }
}
