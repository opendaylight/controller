/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true, service = DatastoreContextIntrospectorFactory.class)
public final class OSGiDatastoreContextIntrospectorFactory extends AbstractDatastoreContextIntrospectorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiDatastoreContextIntrospectorFactory.class);

    @Reference
    volatile BindingNormalizedNodeSerializer serializer = null;

    @Override
    BindingNormalizedNodeSerializer serializer() {
        return verifyNotNull(serializer);
    }

    @Activate
    @SuppressWarnings("static-method")
    void activate() {
        LOG.info("Datastore Context Introspector activated");
    }

    @Deactivate
    @SuppressWarnings("static-method")
    void deactivate() {
        LOG.info("Datastore Context Introspector deactivated");
    }
}
