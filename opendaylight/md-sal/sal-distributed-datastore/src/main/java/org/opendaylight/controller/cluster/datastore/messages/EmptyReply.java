/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

/**
 * A reply with no data.
 *
 * @author Thomas Pantelis
 */
public abstract class EmptyReply extends VersionedExternalizableMessage {
    protected EmptyReply() {
    }

    protected EmptyReply(short version) {
        super(version);
    }

    protected abstract Object newLegacySerializedInstance();

    @Override
    public Object toSerializable() {
        return getVersion() >= DataStoreVersions.BORON_VERSION ? this : newLegacySerializedInstance();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [version=" + getVersion() + "]";
    }
}
