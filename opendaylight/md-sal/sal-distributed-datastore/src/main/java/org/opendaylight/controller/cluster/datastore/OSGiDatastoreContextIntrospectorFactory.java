/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DatastoreContextIntrospectorFactory.class)
public final class OSGiDatastoreContextIntrospectorFactory extends AbstractDatastoreContextIntrospectorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiDatastoreContextIntrospectorFactory.class);

    private final AtomicReference<BindingNormalizedNodeSerializer> serializer = new AtomicReference<>();

    @Activate
    void activate() {
        serializer();
        LOG.info("Datastore Context Introspector activated");
    }

    @Deactivate
    @SuppressWarnings("static-method")
    void deactivate() {
        LOG.info("Datastore Context Introspector deactivated");
    }

    @Override
    BindingNormalizedNodeSerializer serializer() {
        return verifyNotNull(serializer.getAcquire());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    void bindSerializer(final BindingNormalizedNodeSerializer newSerializer) {
        serializer.setRelease(requireNonNull(newSerializer));
        LOG.debug("Using new serializer {}", newSerializer);
    }

    void unbindSerializer(final BindingNormalizedNodeSerializer oldSerializer) {
        if (serializer.compareAndExchangeRelease(oldSerializer, null) == oldSerializer) {
            LOG.debug("Relinquished final serializer {}", oldSerializer);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("serializer", serializer).toString();
    }
}
