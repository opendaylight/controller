/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;

@Beta
public final class DefaultDatastoreContextIntrospectorFactory extends AbstractDatastoreContextIntrospectorFactory {
    public DefaultDatastoreContextIntrospectorFactory(final BindingNormalizedNodeSerializer serializer) {
        super(serializer);
    }
}
