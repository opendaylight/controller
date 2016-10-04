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
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Builder of an {@link InstanceIdentifier} and {@link DataObject} Entry/Pair.
 *
 * @param <T> DataObject type
 *
 * @see DataTreeIdentifierIdentifiableBuilder
 *
 * @author Michael Vorburger
 */
public interface IdentifierIdentifiableBuilder<T extends DataObject>
        extends Builder<Map.Entry<InstanceIdentifier<T>, T>> {

    InstanceIdentifier<T> identifier();

    T identifiable();

    @Override
    default Map.Entry<InstanceIdentifier<T>, T> build() {
        return new SimpleImmutableEntry<>(identifier(), identifiable());
    }
}
