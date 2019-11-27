/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.opendaylight.controller.cluster.datastore.utils.SerializablePersistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;

/**
 * Local or remote message sent in reply to FindPrimaryShard to indicate the primary shard is remote to the caller.
 */
public class RemotePrimaryShardFound implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String primaryPath;
    private final short primaryVersion;
    private final SerializablePersistence persistence;

    public RemotePrimaryShardFound(final String primaryPath, short primaryVersion, final Persistence persistence) {
        this.primaryPath = primaryPath;
        this.primaryVersion = primaryVersion;
        this.persistence = SerializablePersistence.from(persistence);
    }

    public String getPrimaryPath() {
        return primaryPath;
    }

    public short getPrimaryVersion() {
        return primaryVersion;
    }

    public Persistence getPersistence() {
        return SerializablePersistence.toPersistence(persistence);
    }

    @Override
    public String toString() {
        return "RemotePrimaryShardFound [primaryPath=" + primaryPath
                + ", primaryVersion=" + primaryVersion + "]";
    }
}
