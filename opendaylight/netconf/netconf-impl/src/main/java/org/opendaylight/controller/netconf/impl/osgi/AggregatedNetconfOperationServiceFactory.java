/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.CloseableUtil;

/**
 * NetconfOperationService aggregator. Makes a collection of operation services accessible as one.
 */
public class AggregatedNetconfOperationServiceFactory implements NetconfOperationServiceFactory, NetconfOperationServiceFactoryListener {

    private final Set<NetconfOperationServiceFactory> factories = new HashSet<>();

    @Override
    public synchronized void onAddNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        factories.add(service);
    }

    @Override
    public synchronized void onRemoveNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        factories.remove(service);
    }

    @Override
    public Set<Capability> getCapabilities() {
        final HashSet<Capability> capabilities = Sets.newHashSet();
        for (final NetconfOperationServiceFactory factory : factories) {
            capabilities.addAll(factory.getCapabilities());
        }
        return capabilities;
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        final List<AutoCloseable> regs = Lists.newArrayList();
        for (final NetconfOperationServiceFactory factory : factories) {
            regs.add(factory.registerCapabilityListener(listener));
        }

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                CloseableUtil.closeAll(regs);
            }
        };
    }

    @Override
    public NetconfOperationService createService(final String netconfSessionIdForReporting) {
        return new AggregatedNetconfOperation(factories, netconfSessionIdForReporting);
    }

    private static final class AggregatedNetconfOperation implements NetconfOperationService {

        private final Set<NetconfOperationService> services;

        public AggregatedNetconfOperation(final Set<NetconfOperationServiceFactory> factories, final String netconfSessionIdForReporting) {
            final Builder<NetconfOperationService> b = ImmutableSet.builder();
            for (final NetconfOperationServiceFactory factory : factories) {
                b.add(factory.createService(netconfSessionIdForReporting));
            }
            this.services = b.build();
        }

        @Override
        public Set<NetconfOperation> getNetconfOperations() {
            final HashSet<NetconfOperation> operations = Sets.newHashSet();
            for (final NetconfOperationService service : services) {
                operations.addAll(service.getNetconfOperations());
            }
            return operations;
        }

        @Override
        public void close() {
            try {
                CloseableUtil.closeAll(services);
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to properly close all aggregated services", e);
            }
        }
    }
}
