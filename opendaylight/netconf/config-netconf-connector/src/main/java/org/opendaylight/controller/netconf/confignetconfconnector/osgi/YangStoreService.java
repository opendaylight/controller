/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.notifications.BaseNetconfNotificationListener;
import org.opendaylight.controller.netconf.notifications.BaseNotificationPublisherRegistration;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangStoreService implements YangStoreContext {

    private static final Logger LOG = LoggerFactory.getLogger(YangStoreService.class);

    /**
     * This is a rather interesting locking model. We need to guard against both the
     * cache expiring from GC and being invalidated by schema context change. The
     * context can change while we are doing processing, so we do not want to block
     * it, so no synchronization can happen on the methods.
     *
     * So what we are doing is the following:
     *
     * We synchronize with GC as usual, using a SoftReference.
     *
     * The atomic reference is used to synchronize with {@link #refresh()}, e.g. when
     * refresh happens, it will push a SoftReference(null), e.g. simulate the GC. Now
     * that may happen while the getter is already busy acting on the old schema context,
     * so it needs to understand that a refresh has happened and retry. To do that, it
     * attempts a CAS operation -- if it fails, in knows that the SoftReference has
     * been replaced and thus it needs to retry.
     *
     * Note that {@link #getYangStoreSnapshot()} will still use synchronize() internally
     * to stop multiple threads doing the same work.
     */
    private final AtomicReference<SoftReference<YangStoreSnapshot>> ref =
            new AtomicReference<>(new SoftReference<YangStoreSnapshot>(null));

    private final SchemaContextProvider schemaContextProvider;
    private final NotificationCollectorTracker notificationPublisher;

    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "config-netconf-connector-capability-notifications");
        }
    });

    public YangStoreService(final SchemaContextProvider schemaContextProvider, final BundleContext context) {
        this.schemaContextProvider = schemaContextProvider;
        this.notificationPublisher = new NotificationCollectorTracker(context);
    }

    protected synchronized YangStoreSnapshot getYangStoreSnapshot() {
        SoftReference<YangStoreSnapshot> r = ref.get();
        YangStoreSnapshot ret = r.get();

        while (ret == null) {
            // We need to be compute a new value
            ret = new YangStoreSnapshot(schemaContextProvider.getSchemaContext());

            if (!ref.compareAndSet(r, new SoftReference<>(ret))) {
                LOG.debug("Concurrent refresh detected, recomputing snapshot");
                r = ref.get();
                ret = null;
            }
        }

        return ret;
    }

    @Override
    public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
        return getYangStoreSnapshot().getModuleMXBeanEntryMap();
    }

    @Override
    public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
        return getYangStoreSnapshot().getQNamesToIdentitiesToModuleMXBeanEntries();
    }

    @Override
    public Set<Module> getModules() {
        return getYangStoreSnapshot().getModules();
    }

    @Override
    public String getModuleSource(final ModuleIdentifier moduleIdentifier) {
        return getYangStoreSnapshot().getModuleSource(moduleIdentifier);
    }

    public void refresh() {
        final YangStoreSnapshot previous = ref.get().get();
        ref.set(new SoftReference<YangStoreSnapshot>(null));
        notificationExecutor.submit(new CapabilityChangeNotifier(previous));
    }

    private final class CapabilityChangeNotifier implements Runnable {
        private final YangStoreSnapshot previous;

        public CapabilityChangeNotifier(final YangStoreSnapshot previous) {
            this.previous = previous;
        }

        @Override
        public void run() {
            final YangStoreSnapshot current = getYangStoreSnapshot();

            if(current.equals(previous) == false) {
                notificationPublisher.onCapabilityChanged(current.computeDiff(previous));
            }
        }
    }

    /**
     * Looks for NetconfNotificationCollector service and publishes base netconf notifications if possible
     */
    private class NotificationCollectorTracker implements ServiceTrackerCustomizer<NetconfNotificationCollector, NetconfNotificationCollector>, BaseNetconfNotificationListener, AutoCloseable {

        private final BundleContext context;
        private final ServiceTracker<NetconfNotificationCollector, NetconfNotificationCollector> listenerTracker;
        private BaseNotificationPublisherRegistration publisherReg;

        public NotificationCollectorTracker(final BundleContext context) {
            this.context = context;
            listenerTracker = new ServiceTracker<>(context, NetconfNotificationCollector.class, this);
            listenerTracker.open();
        }

        @Override
        public synchronized NetconfNotificationCollector addingService(final ServiceReference<NetconfNotificationCollector> reference) {
            closePublisherRegistration();
            publisherReg = context.getService(reference).registerBaseNotificationPublisher();
            return null;
        }

        @Override
        public synchronized void modifiedService(final ServiceReference<NetconfNotificationCollector> reference, final NetconfNotificationCollector service) {
            closePublisherRegistration();
            publisherReg = context.getService(reference).registerBaseNotificationPublisher();
        }

        @Override
        public synchronized void removedService(final ServiceReference<NetconfNotificationCollector> reference, final NetconfNotificationCollector service) {
            closePublisherRegistration();
            publisherReg = null;
        }

        private void closePublisherRegistration() {
            if(publisherReg != null) {
                publisherReg.close();
            }
        }

        @Override
        public synchronized void close() {
            closePublisherRegistration();
            listenerTracker.close();
        }

        @Override
        public void onCapabilityChanged(final NetconfCapabilityChange capabilityChange) {
            if(publisherReg == null) {
                LOG.warn("Omitting notification due to missing notification service: {}", capabilityChange);
                return;
            }

            publisherReg.onCapabilityChanged(capabilityChange);
        }
    }
}
