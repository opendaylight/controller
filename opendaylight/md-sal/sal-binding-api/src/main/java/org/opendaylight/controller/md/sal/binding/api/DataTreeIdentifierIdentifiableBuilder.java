/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Builder of an {@link DataTreeIdentifier} (which is an InstanceIdentifier plus
 * operational/configuration story type) and {@link DataObject} Entry/Pair.
 *
 * @param <T> DataObject type
 *
 * @see IdentifierIdentifiableBuilder
 *
 * @author Michael Vorburger
 */
public interface DataTreeIdentifierIdentifiableBuilder<T extends DataObject>
        extends Builder<Map.Entry<DataTreeIdentifier<T>, T>> {

    LogicalDatastoreType type();

    InstanceIdentifier<T> identifier();

    T identifiable();

    @Override
    default Map.Entry<DataTreeIdentifier<T>, T> build() {
        return new SimpleImmutableEntry<>(new DataTreeIdentifier<>(type(), identifier()), identifiable());
    }
}
