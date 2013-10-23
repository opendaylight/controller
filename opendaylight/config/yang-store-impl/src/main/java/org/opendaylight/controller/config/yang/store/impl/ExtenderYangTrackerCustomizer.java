/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreListenerRegistration;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yang.store.spi.YangStoreListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Note on consistency:
 * When this bundle is activated after other bundles containing yang files, the resolving order
 * is not preserved. We thus maintain two maps, one containing consistent snapshot, other inconsistent. The
 * container should eventually send all events and thus making the inconsistent map redundant.
 */
public class ExtenderYangTrackerCustomizer implements BundleTrackerCustomizer<Object>, YangStoreService {

    private static final Logger logger = LoggerFactory
            .getLogger(ExtenderYangTrackerCustomizer.class);

    private final Multimap<Bundle, URL> consistentBundlesToYangURLs = HashMultimap.create();

    /*
    Map of currently problematic yang files that should get fixed eventually after all events are received.
     */
    private final Multimap<Bundle, URL> inconsistentBundlesToYangURLs = HashMultimap.create();

    private final YangStoreCache cache = new YangStoreCache();
    private final MbeParser mbeParser;
    private final List<YangStoreListener> listeners = new ArrayList<>();

    public ExtenderYangTrackerCustomizer() {
        this(new MbeParser());

    }

    @VisibleForTesting
    ExtenderYangTrackerCustomizer(MbeParser mbeParser) {
        this.mbeParser = mbeParser;
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {

        // Ignore system bundle:
        // system bundle might have config-api on classpath &&
        // config-api contains yang files =>
        // system bundle might contain yang files from that bundle
        if (bundle.getBundleId() == 0)
            return bundle;

        Enumeration<URL> enumeration = bundle.findEntries("META-INF/yang", "*.yang", false);
        if (enumeration != null && enumeration.hasMoreElements()) {
            synchronized (this) {
                List<URL> addedURLs = new ArrayList<>();
                while (enumeration.hasMoreElements()) {
                    URL url = enumeration.nextElement();
                    addedURLs.add(url);
                }
                logger.trace("Bundle {} has event {}, bundle state {}, URLs {}", bundle, event, bundle.getState(), addedURLs);
                // test that yang store is consistent
                Multimap<Bundle, URL> proposedNewState = HashMultimap.create(consistentBundlesToYangURLs);
                proposedNewState.putAll(inconsistentBundlesToYangURLs);
                proposedNewState.putAll(bundle, addedURLs);
                boolean adding = true;
                if (tryToUpdateState(addedURLs, proposedNewState, adding) == false) {
                    inconsistentBundlesToYangURLs.putAll(bundle, addedURLs);
                }
            }
        }
        return bundle;
    }

    private synchronized boolean tryToUpdateState(Collection<URL> changedURLs, Multimap<Bundle, URL> proposedNewState, boolean adding) {
        Preconditions.checkArgument(changedURLs.size() > 0, "No change can occur when no URLs are changed");
        try(YangStoreSnapshot snapshot = createSnapshot(mbeParser, proposedNewState)) {
            // consistent state
            // merge into
            consistentBundlesToYangURLs.clear();
            consistentBundlesToYangURLs.putAll(proposedNewState);
            inconsistentBundlesToYangURLs.clear();
            // update cache
            updateCache(snapshot);
            logger.info("Yang store updated to new consistent state");
            logger.trace("Yang store updated to new consistent state containing {}", consistentBundlesToYangURLs);

            notifyListeners(changedURLs, adding);
            return true;
        } catch(YangStoreException e) {
            // inconsistent state
            logger.debug("Yang store is falling back on last consistent state containing {}, inconsistent yang files {}, reason {}",
                    consistentBundlesToYangURLs, inconsistentBundlesToYangURLs, e.toString());
            return false;
        }
    }

    private void updateCache(YangStoreSnapshot snapshot) {
        cache.cacheYangStore(consistentBundlesToYangURLs, snapshot);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        logger.debug("Modified bundle {} {} {}", bundle, event, object);
    }

    /**
     * Notifiers get only notified when consistent snapshot has changed.
     */
    private void notifyListeners(Collection<URL> changedURLs, boolean adding) {
        Preconditions.checkArgument(changedURLs.size() > 0, "Cannot notify when no URLs changed");
        if (changedURLs.size() > 0) {
            RuntimeException potential = new RuntimeException("Error while notifying listeners");
            for (YangStoreListener listener : listeners) {
                try {
                    if (adding) {
                        listener.onAddedYangURL(changedURLs);
                    } else {
                        listener.onRemovedYangURL(changedURLs);
                    }
                } catch(RuntimeException e) {
                    potential.addSuppressed(e);
                }
            }
            if (potential.getSuppressed().length > 0) {
                throw potential;
            }
        }
    }


    /**
     * If removing YANG files makes yang store inconsistent, method {@link #getYangStoreSnapshot()}
     * will throw exception. There is no rollback.
     */
    @Override
    public synchronized void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        inconsistentBundlesToYangURLs.removeAll(bundle);
        Collection<URL> consistentURLsToBeRemoved = consistentBundlesToYangURLs.removeAll(bundle);

        if (consistentURLsToBeRemoved.isEmpty()){
            return; // no change
        }
        boolean adding = false;
        notifyListeners(consistentURLsToBeRemoved, adding);
    }

    @Override
    public synchronized YangStoreSnapshot getYangStoreSnapshot()
            throws YangStoreException {
        Optional<YangStoreSnapshot> yangStoreOpt = cache.getCachedYangStore(consistentBundlesToYangURLs);
        if (yangStoreOpt.isPresent()) {
            logger.trace("Returning cached yang store {}", yangStoreOpt.get());
            return yangStoreOpt.get();
        }
        YangStoreSnapshot snapshot = createSnapshot(mbeParser, consistentBundlesToYangURLs);
        updateCache(snapshot);
        return snapshot;
    }

    private static YangStoreSnapshot createSnapshot(MbeParser mbeParser, Multimap<Bundle, URL> multimap) throws YangStoreException {
        try {
            YangStoreSnapshot yangStoreSnapshot = mbeParser.parseYangFiles(fromUrlsToInputStreams(multimap));
            logger.trace("{} module entries parsed successfully from {} yang files",
                    yangStoreSnapshot.countModuleMXBeanEntries(), multimap.values().size());
            return yangStoreSnapshot;
        } catch (RuntimeException e) {
            throw new YangStoreException("Unable to parse yang files from following URLs: " + multimap, e);
        }
    }

    private static Collection<InputStream> fromUrlsToInputStreams(Multimap<Bundle, URL> multimap) {
        return Collections2.transform(multimap.values(),
                new Function<URL, InputStream>() {

                    @Override
                    public InputStream apply(URL url) {
                        try {
                            return url.openStream();
                        } catch (IOException e) {
                            logger.warn("Unable to open stream from {}", url);
                            throw new IllegalStateException(
                                    "Unable to open stream from " + url, e);
                        }
                    }
                });
    }

    @Override
    public synchronized YangStoreListenerRegistration registerListener(final YangStoreListener listener) {
        listeners.add(listener);
        return new YangStoreListenerRegistration() {
            @Override
            public void close() {
                listeners.remove(listener);
            }
        };
    }

    private static final class YangStoreCache {

        Set<URL> cachedUrls;
        YangStoreSnapshot cachedYangStoreSnapshot;

        Optional<YangStoreSnapshot> getCachedYangStore(
                Multimap<Bundle, URL> bundlesToYangURLs) {
            Set<URL> urls = setFromMultimapValues(bundlesToYangURLs);
            if (cachedUrls != null && cachedUrls.equals(urls)) {
                Preconditions.checkState(cachedYangStoreSnapshot != null);
                return Optional.of(cachedYangStoreSnapshot);
            }
            return Optional.absent();
        }

        private static Set<URL> setFromMultimapValues(
                Multimap<Bundle, URL> bundlesToYangURLs) {
            Set<URL> urls = Sets.newHashSet(bundlesToYangURLs.values());
            Preconditions.checkState(bundlesToYangURLs.size() == urls.size());
            return urls;
        }

        void cacheYangStore(Multimap<Bundle, URL> urls,
                YangStoreSnapshot yangStoreSnapshot) {
            this.cachedUrls = setFromMultimapValues(urls);
            this.cachedYangStoreSnapshot = yangStoreSnapshot;
        }

    }
}
