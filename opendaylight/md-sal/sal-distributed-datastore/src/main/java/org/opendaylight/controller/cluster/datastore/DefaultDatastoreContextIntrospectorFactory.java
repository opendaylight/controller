/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;

@Beta
public final class DefaultDatastoreContextIntrospectorFactory extends AbstractDatastoreContextIntrospectorFactory {
    private final BindingNormalizedNodeSerializer serializer;

    public DefaultDatastoreContextIntrospectorFactory(final BindingNormalizedNodeSerializer serializer) {
        this.serializer = requireNonNull(serializer);
    }

    @Override
    BindingNormalizedNodeSerializer serializer() {
        return serializer;
    }
}
