/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Factory for creating {@link DatastoreContextIntrospector} instances.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public interface DatastoreContextIntrospectorFactory {
    /**
     * Create a new {@link DatastoreContextIntrospector} initialized with specified properties.
     *
     * @param datastoreType Datastore type
     * @param properties optional initial properties
     * @return A new DatastoreContextIntrospector
     */
    DatastoreContextIntrospector newInstance(LogicalDatastoreType datastoreType,
        @Nullable Map<String, Object> properties);
}
