/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

/**
 * Factory for creating DatastoreContext instances.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextFactory {
    private final DatastoreContextIntrospector introspector;

    public DatastoreContextFactory(DatastoreContextIntrospector introspector) {
        this.introspector = introspector;
    }

    public DatastoreContext getShardDatastoreContext(String forShardName) {
        return introspector.getShardDatastoreContext(forShardName);
    }

    public DatastoreContext getBaseDatastoreContext() {
        return introspector.getContext();
    }
}
