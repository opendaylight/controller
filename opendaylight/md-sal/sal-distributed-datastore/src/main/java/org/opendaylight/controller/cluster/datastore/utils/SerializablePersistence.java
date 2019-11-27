/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.PersistenceBuilder;

public class SerializablePersistence implements Serializable {
    private static final long serialVersionUID = 1L;
    private final DatastoreType datastoreType;
    private final Boolean persistent;

    public SerializablePersistence(final DatastoreType datastoreType, final Boolean persistent) {
        this.datastoreType = requireNonNull(datastoreType, "Datastore type can't be null");
        this.persistent = requireNonNull(persistent, "Persistent flag can't be null");
    }

    public static SerializablePersistence from(final Persistence persistence) {
        if (persistence != null) {
            return new SerializablePersistence(persistence.getDatastore(), persistence.isPersistent());
        }
        return null;
    }

    public static Persistence toPersistence(SerializablePersistence serializablePersistence) {
        if (serializablePersistence != null) {
            return serializablePersistence.toPersistence();
        }
        return null;
    }

    public Persistence toPersistence() {
        return new PersistenceBuilder().setDatastore(this.datastoreType).setPersistent(this.persistent).build();
    }

    public DatastoreType getDatastoreType() {
        return datastoreType;
    }

    public Boolean getPersistent() {
        return persistent;
    }
}
