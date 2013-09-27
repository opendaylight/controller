/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;

import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class ExtenderYangTracker extends BundleTracker<Object> implements
        YangStoreService {

    private static final Logger logger = LoggerFactory
            .getLogger(ExtenderYangTracker.class);

    private final Multimap<Bundle, URL> bundlesToYangURLs = HashMultimap
            .create();
    private final YangStoreCache cache = new YangStoreCache();
    private final MbeParser mbeParser;

    public ExtenderYangTracker(BundleContext context) {
        this(context, new MbeParser());

    }

    @VisibleForTesting
    ExtenderYangTracker(BundleContext context, MbeParser mbeParser) {
        super(context, Bundle.ACTIVE, null);
        this.mbeParser = mbeParser;
        logger.trace("Registered as extender with context {}", context);
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {

        // Ignore system bundle
        //
        // system bundle has config-api on classpath &&
        // config-api contains yang files =>
        // system bundle contains yang files from that bundle
        if (bundle.getBundleId() == 0)
            return bundle;

        Enumeration<URL> yangURLs = bundle.findEntries("META-INF/yang",
                "*.yang", false);

        if (yangURLs == null)
            return bundle;

        synchronized (this) {
            while (yangURLs.hasMoreElements()) {
                URL yang = yangURLs.nextElement();
                logger.debug("Bundle {} found yang file {}", bundle, yang);
                bundlesToYangURLs.put(bundle, yang);
            }
        }

        return bundle;
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        synchronized (this) {
            Collection<URL> urls = bundlesToYangURLs.removeAll(bundle);
            logger.debug(
                    "Removed following yang URLs {} because of removed bundle {}",
                    urls, bundle);
        }
    }

    @Override
    public synchronized YangStoreSnapshot getYangStoreSnapshot()
            throws YangStoreException {
        Optional<YangStoreSnapshot> yangStoreOpt = cache
                .getCachedYangStore(bundlesToYangURLs);
        if (yangStoreOpt.isPresent()) {
            logger.debug("Returning cached yang store {}", yangStoreOpt.get());
            return yangStoreOpt.get();
        }

        try {
            YangStoreSnapshot yangStoreSnapshot = mbeParser
                    .parseYangFiles(fromUrlsToInputStreams());
            logger.debug(
                    "{} module entries parsed successfully from {} yang files",
                    yangStoreSnapshot.countModuleMXBeanEntries(),
                    bundlesToYangURLs.values().size());
            cache.cacheYangStore(bundlesToYangURLs, yangStoreSnapshot);

            return yangStoreSnapshot;
        } catch (RuntimeException e) {
            logger.warn(
                    "Unable to parse yang files, yang files that were picked up so far: {}",
                    bundlesToYangURLs, e);
            throw new YangStoreException("Unable to parse yang files", e);
        }
    }

    private Collection<InputStream> fromUrlsToInputStreams() {
        return Collections2.transform(bundlesToYangURLs.values(),
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
