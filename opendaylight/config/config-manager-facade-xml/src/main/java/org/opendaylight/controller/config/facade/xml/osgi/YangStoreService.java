/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.osgi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.capability.ModuleListener;
import org.opendaylight.controller.config.util.capability.YangModuleCapability;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
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
     * The atomic reference is used to synchronize with {@link #refresh(org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext)}, e.g. when
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

    private final AtomicReference<SoftReference<BindingRuntimeContext>> refBindingContext =
            new AtomicReference<>(new SoftReference<BindingRuntimeContext>(null));

    private final SchemaContextProvider schemaContextProvider;
    private final SchemaSourceProvider<YangTextSchemaSource> sourceProvider;

    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "yangstore-capability-notifications");
        }
    });

    private final Set<ModuleListener> listeners = Collections.synchronizedSet(new HashSet<ModuleListener>());

    public YangStoreService(final SchemaContextProvider schemaContextProvider,
        final SchemaSourceProvider<YangTextSchemaSource> sourceProvider) {
        this.schemaContextProvider = schemaContextProvider;
        this.sourceProvider = sourceProvider;
    }

    synchronized YangStoreContext getYangStoreSnapshot() {
        SoftReference<YangStoreSnapshot> r = ref.get();
        YangStoreSnapshot ret = r.get();

        while (ret == null) {
            // We need to be compute a new value
            // TODO sourceProvider is not a snapshot
            ret = new YangStoreSnapshot(schemaContextProvider.getSchemaContext(), refBindingContext.get().get(), sourceProvider);

            if (!ref.compareAndSet(r, new SoftReference<>(ret))) {
                LOG.debug("Concurrent refresh detected, recomputing snapshot");
                r = ref.get();
                ret = null;
            }
        }

        return ret;
    }

    public YangStoreContext getCurrentSnapshot() {
        return getYangStoreSnapshot();
    }

    @Deprecated
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

    @Override
    public EnumResolver getEnumResolver() {
        return getYangStoreSnapshot().getEnumResolver();
    }

    public void refresh(final BindingRuntimeContext runtimeContext) {
        final YangStoreSnapshot previous = ref.get().get();
        ref.set(new SoftReference<YangStoreSnapshot>(null));
        refBindingContext.set(new SoftReference<>(runtimeContext));
        notificationExecutor.submit(new CapabilityChangeNotifier(previous));
    }

    public AutoCloseable registerModuleListener(final ModuleListener listener) {
        YangStoreContext context = ref.get().get();

        if (context == null) {
            context = getYangStoreSnapshot();
        }

        this.listeners.add(listener);
        listener.onCapabilitiesChanged(toCapabilities(context.getModules(), context), Collections.<Capability>emptySet());

        return new AutoCloseable() {
            @Override
            public void close() {
                YangStoreService.this.listeners.remove(listener);
            }
        };
    }

    private static Set<Capability> toCapabilities(final Set<Module> modules, final YangStoreContext current) {
        return ImmutableSet.copyOf(Collections2.transform(modules, new Function<Module, Capability>() {
            @Override
            public Capability apply(final Module input) {
                return new YangModuleCapability(input, current.getModuleSource(input));
            }
        }));
    }

    private final class CapabilityChangeNotifier implements Runnable {

        private final YangStoreSnapshot previous;

        public CapabilityChangeNotifier(final YangStoreSnapshot previous) {
            this.previous = previous;
        }

        @Override
        public void run() {
            final YangStoreContext current = getYangStoreSnapshot();

            if (!current.equals(previous)) {
                final Set<Module> prevModules = previous.getModules();
                final Set<Module> currModules = current.getModules();
                final Set<Module> removed = Sets.difference(prevModules, currModules);
                final Set<Module> added = Sets.difference(currModules, prevModules);

                final Set<Capability> addedCaps = toCapabilities(added, current);
                final Set<Capability> removedCaps = toCapabilities(removed, current);

                for (final ModuleListener listener : listeners) {
                    listener.onCapabilitiesChanged(addedCaps, removedCaps);
                }
            }
        }

    }
}
