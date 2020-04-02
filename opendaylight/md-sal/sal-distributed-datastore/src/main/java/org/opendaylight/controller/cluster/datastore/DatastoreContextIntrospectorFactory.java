/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Factory for creating DatastoreContextIntrospector instances.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospectorFactory {
    private final BindingRuntimeContext runtimeContext;

    public DatastoreContextIntrospectorFactory(final BindingRuntimeContext runtimeContext) {
        this.runtimeContext = requireNonNull(runtimeContext);
    }

    public DatastoreContextIntrospector newInstance(final LogicalDatastoreType datastoreType) {
        return new DatastoreContextIntrospector(DatastoreContext.newBuilder()
                .logicalStoreType(datastoreType).tempFileDirectory("./data").build(), newBindingSerializer());
    }

    @VisibleForTesting
    DatastoreContextIntrospector newInstance(final DatastoreContext context) {
        return new DatastoreContextIntrospector(context, newBindingSerializer());
    }

    private BindingNormalizedNodeSerializer newBindingSerializer() {
        return new BindingNormalizedNodeCodecRegistry(runtimeContext);
    }
}
