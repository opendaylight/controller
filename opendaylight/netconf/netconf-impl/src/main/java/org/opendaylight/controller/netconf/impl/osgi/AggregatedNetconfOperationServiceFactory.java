/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.util.CloseableUtil;

/**
 * NetconfOperationService aggregator. Makes a collection of operation services accessible as one.
 */
public class AggregatedNetconfOperationServiceFactory implements NetconfOperationServiceFactory, NetconfOperationServiceFactoryListener, AutoCloseable {

    private final Set<NetconfOperationServiceFactory> factories = new HashSet<>();
    private final Multimap<NetconfOperationServiceFactory, AutoCloseable> registrations = HashMultimap.create();
    private final Set<CapabilityListener> listeners = Sets.newHashSet();

    @Override
    public synchronized void onAddNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        factories.add(service);

        for (final CapabilityListener listener : listeners) {
            AutoCloseable reg = service.registerCapabilityListener(listener);
            registrations.put(service, reg);
        }
    }

    @Override
    public synchronized void onRemoveNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        factories.remove(service);

        for (final AutoCloseable autoCloseable : registrations.get(service)) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                // FIXME Issue warning
            }
        }

        registrations.removeAll(service);
    }

    @Override
    public synchronized Set<Capability> getCapabilities() {
        final HashSet<Capability> capabilities = Sets.newHashSet();
        for (final NetconfOperationServiceFactory factory : factories) {
            capabilities.addAll(factory.getCapabilities());
        }
        return capabilities;
    }

    @Override
    public synchronized AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        final Map<NetconfOperationServiceFactory, AutoCloseable> regs = Maps.newHashMap();

        for (final NetconfOperationServiceFactory factory : factories) {
            final AutoCloseable reg = factory.registerCapabilityListener(listener);
            regs.put(factory, reg);
        }
        listeners.add(listener);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                synchronized (AggregatedNetconfOperationServiceFactory.this) {
                    listeners.remove(listener);
                    CloseableUtil.closeAll(regs.values());
                    for (final Map.Entry<NetconfOperationServiceFactory, AutoCloseable> reg : regs.entrySet()) {
                        registrations.remove(reg.getKey(), reg.getValue());
                    }
                }
            }
        };
    }

    @Override
    public synchronized NetconfOperationService createService(final String netconfSessionIdForReporting) {
        return new AggregatedNetconfOperation(factories, netconfSessionIdForReporting);
    }

    @Override
    public synchronized void close() throws Exception {
        factories.clear();
        for (AutoCloseable reg : registrations.values()) {
            reg.close();
        }
        registrations.clear();
        listeners.clear();
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
