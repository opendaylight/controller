/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStorePropertiesContainer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

@NonNullByDefault
abstract sealed class AbstractDatastoreContextIntrospectorFactory implements DatastoreContextIntrospectorFactory
        permits DefaultDatastoreContextIntrospectorFactory, OSGiDatastoreContextIntrospectorFactory {
    @Override
    public DatastoreContextIntrospector newInstance(final LogicalDatastoreType datastoreType,
            final Map<String, Object> properties) {
        final DatastoreContextIntrospector inst = newInstance(datastoreType);
        inst.update(properties);
        return inst;
    }

    @VisibleForTesting
    final DatastoreContextIntrospector newInstance(final LogicalDatastoreType datastoreType) {
        return newInstance(DatastoreContext.newBuilder()
                .logicalStoreType(datastoreType)
                .tempFileDirectory("./data")
                .build());
    }

    @VisibleForTesting
    final DatastoreContextIntrospector newInstance(final DatastoreContext context) {
        return new DatastoreContextIntrospector(context, (DataStorePropertiesContainer) serializer()
            .fromNormalizedNode(YangInstanceIdentifier.of(DataStorePropertiesContainer.QNAME),
                ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(DataStorePropertiesContainer.QNAME))
                .build())
            .getValue());
    }

    abstract BindingNormalizedNodeSerializer serializer();
}
