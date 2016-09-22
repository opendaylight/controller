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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.concurrent.GuardedBy;
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

public class YangStoreService implements YangStoreContext {

    private final SchemaSourceProvider<YangTextSchemaSource> sourceProvider;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("yangstore-capability-notifications").build());

    /**
     * Guarded by explicit lock to allow for properly synchronizing the initial notification and modification
     * of the listener set.
     */
    @GuardedBy("listeners")
    private final Set<ModuleListener> listeners = new HashSet<>();

    /**
     * This is the latest snapshot. Some of its state is always initialized, but the MXBean maps potentially cause
     * recomputation. Accessing those two specific methods needs to re-check whether the snapshot has changed
     * asynchronously and retry if it didi.
     */
    private volatile YangStoreSnapshot snap;

    public YangStoreService(final SchemaContextProvider schemaContextProvider,
        final SchemaSourceProvider<YangTextSchemaSource> sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public YangStoreContext getCurrentSnapshot() {
        return snap;
    }

    @Deprecated
    @Override
    public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
        Map<String, Map<String, ModuleMXBeanEntry>> ret;
        YangStoreSnapshot snapshot;

        do {
            snapshot = snap;
            ret = snapshot.getModuleMXBeanEntryMap();
        } while (!snapshot.equals(snap));

        return ret;
    }

    @Override
    public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
        Map<QName, Map<String, ModuleMXBeanEntry>> ret;
        YangStoreSnapshot snapshot;

        do {
            snapshot = snap;
            ret = snapshot.getQNamesToIdentitiesToModuleMXBeanEntries();
        } while (!snapshot.equals(snap));

        return ret;
    }

    @Override
    public Set<Module> getModules() {
        return snap.getModules();
    }

    @Override
    public String getModuleSource(final ModuleIdentifier moduleIdentifier) {
        return snap.getModuleSource(moduleIdentifier);
    }

    @Override
    public EnumResolver getEnumResolver() {
        return snap.getEnumResolver();
    }

    public void refresh(final BindingRuntimeContext runtimeContext) {
        final YangStoreSnapshot next = new YangStoreSnapshot(runtimeContext, sourceProvider);
        final YangStoreSnapshot previous = snap;
        snap = next;
        notificationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                notifyListeners(previous, next);
            }
        });
    }

    public AutoCloseable registerModuleListener(final ModuleListener listener) {
        final YangStoreContext context = snap;

        synchronized (listeners) {
            if (context != null) {
                listener.onCapabilitiesChanged(toCapabilities(context.getModules(), context), Collections.<Capability>emptySet());
            }
            this.listeners.add(listener);
        }

        return new AutoCloseable() {
            @Override
            public void close() {
                synchronized (listeners) {
                    listeners.remove(listener);
                }
            }
        };
    }

    void notifyListeners(final YangStoreSnapshot previous, final YangStoreSnapshot current) {
        final Set<Module> prevModules = previous.getModules();
        final Set<Module> currModules = current.getModules();
        final Set<Module> removed = Sets.difference(prevModules, currModules);
        final Set<Module> added = Sets.difference(currModules, prevModules);

        final Set<Capability> addedCaps = toCapabilities(added, current);
        final Set<Capability> removedCaps = toCapabilities(removed, current);

        synchronized (listeners) {
            for (final ModuleListener listener : listeners) {
                listener.onCapabilitiesChanged(addedCaps, removedCaps);
            }
        }
    }

    private static Set<Capability> toCapabilities(final Set<Module> modules, final YangStoreContext current) {
        return ImmutableSet.copyOf(Collections2.transform(modules, new Function<Module, Capability>() {
            @Override
            public Capability apply(final Module input) {
                return new YangModuleCapability(input, current.getModuleSource(input));
            }
        }));
    }
}
