/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class YangStoreCache {
    private static final Logger logger = LoggerFactory.getLogger(YangStoreCache.class);

    @GuardedBy("this")
    private Set<URL> cachedUrls = null;
    @GuardedBy("this")
    private Optional<YangStoreSnapshot> cachedYangStoreSnapshot = getInitialSnapshot();
    @GuardedBy("this")
    private Collection<URL> inconsistentURLsForReporting = Collections.emptySet();

    synchronized Optional<YangStoreSnapshot> getSnapshotIfPossible(Multimap<Bundle, URL> bundlesToYangURLs) {
        Set<URL> urls = setFromMultimapValues(bundlesToYangURLs);

        if (cachedUrls==null || cachedUrls.equals(urls)) {
            Preconditions.checkState(cachedYangStoreSnapshot.isPresent());
            YangStoreSnapshot freshSnapshot = YangStoreSnapshotImpl.copy(cachedYangStoreSnapshot.get());
            if (inconsistentURLsForReporting.size() > 0){
                logger.warn("Some yang URLs are ignored: {}", inconsistentURLsForReporting);
            }
            return Optional.of(freshSnapshot);
        }

        return Optional.absent();
    }

    private static Set<URL> setFromMultimapValues(
            Multimap<Bundle, URL> bundlesToYangURLs) {
        Set<URL> urls = Sets.newHashSet(bundlesToYangURLs.values());
        Preconditions.checkState(bundlesToYangURLs.size() == urls.size());
        return urls;
    }

    synchronized void cacheYangStore(Multimap<Bundle, URL> urls,
                                     YangStoreSnapshot yangStoreSnapshot) {
        this.cachedUrls = setFromMultimapValues(urls);
        this.cachedYangStoreSnapshot = Optional.of(yangStoreSnapshot);
    }

    synchronized void invalidate() {
        cachedUrls.clear();
        if (cachedYangStoreSnapshot.isPresent()){
            cachedYangStoreSnapshot.get().close();
            cachedYangStoreSnapshot = Optional.absent();
        }
    }

    public synchronized void setInconsistentURLsForReporting(Collection<URL> urls){
        inconsistentURLsForReporting = urls;
    }

    private Optional<YangStoreSnapshot> getInitialSnapshot() {
        YangStoreSnapshot initialSnapshot = new YangStoreSnapshot() {
            @Override
            public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
                return Collections.emptyMap();
            }

            @Override
            public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
                return Collections.emptyMap();
            }

            @Override
            public Set<Module> getModules() {
                return Collections.emptySet();
            }

            @Override
            public Map<Module, String> getModulesToSources() {
                return Collections.emptyMap();
            }

            @Override
            public String getModuleSource(Module module) {
                throw new IllegalArgumentException("Cannot get sources in empty snapshot");
            }

            @Override
            public int countModuleMXBeanEntries() {
                return 0;
            }

            @Override
            public void close() {
            }
        };
        return Optional.of(initialSnapshot);
    }
}
