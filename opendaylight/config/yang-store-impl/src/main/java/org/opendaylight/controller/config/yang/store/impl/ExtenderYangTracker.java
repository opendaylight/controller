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
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note on consistency:
 * When this bundle is activated after other bundles containing yang files, the resolving order
 * is not preserved. We thus maintain two maps, one containing consistent snapshot, other inconsistent. The
 * container should eventually send all events and thus making the inconsistent map redundant.
 */
public class ExtenderYangTracker extends BundleTracker<Object> implements YangStoreService, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExtenderYangTracker.class);

    private final Multimap<Bundle, URL> consistentBundlesToYangURLs = HashMultimap.create();

    /*
    Map of currently problematic yang files that should get fixed eventually after all events are received.
     */
    private final Multimap<Bundle, URL> inconsistentBundlesToYangURLs = HashMultimap.create();

    private final YangStoreCache cache = new YangStoreCache();
    private final MbeParser mbeParser;


    public ExtenderYangTracker(Optional<Pattern> maybeBlacklist, BundleContext bundleContext) {
        this(new MbeParser(), maybeBlacklist, bundleContext);
    }

    @GuardedBy("this")
    private Optional<Pattern> maybeBlacklist;

    @VisibleForTesting
    ExtenderYangTracker(MbeParser mbeParser, Optional<Pattern> maybeBlacklist, BundleContext bundleContext) {
        super(bundleContext, BundleEvent.RESOLVED | BundleEvent.UNRESOLVED, null);
        this.mbeParser = mbeParser;
        this.maybeBlacklist = maybeBlacklist;
        open();
    }

    @Override
    public synchronized Object addingBundle(Bundle bundle, BundleEvent event) {

        // Ignore system bundle:
        // system bundle might have config-api on classpath &&
        // config-api contains yang files =>
        // system bundle might contain yang files from that bundle
        if (bundle.getBundleId() == 0)
            return bundle;

        if (maybeBlacklist.isPresent()) {
            Matcher m = maybeBlacklist.get().matcher(bundle.getSymbolicName());
            if (m.matches()) {
                logger.debug("Ignoring {} because it is in blacklist {}", bundle, maybeBlacklist);
                return bundle;
            }
        }

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

                Preconditions.checkArgument(addedURLs.size() > 0, "No change can occur when no URLs are changed");

                try(YangStoreSnapshotImpl snapshot = createSnapshot(mbeParser, proposedNewState)) {
                    onSnapshotSuccess(proposedNewState, snapshot);
                } catch(YangStoreException e) {
                    onSnapshotFailure(bundle, addedURLs, e);
                }
            }
        }
        return bundle;
    }

    private synchronized void onSnapshotFailure(Bundle bundle, List<URL> addedURLs, Exception failureReason) {
        // inconsistent state
        inconsistentBundlesToYangURLs.putAll(bundle, addedURLs);

        logger.debug("Yang store is falling back to last consistent state containing {}, inconsistent yang files {}",
                consistentBundlesToYangURLs, inconsistentBundlesToYangURLs, failureReason);
        logger.info("Yang store is falling back to last consistent state containing {} files, keeping {} inconsistent yang files due to {}",
                consistentBundlesToYangURLs.size(), inconsistentBundlesToYangURLs.size(), failureReason.toString());
        cache.setInconsistentURLsForReporting(inconsistentBundlesToYangURLs.values());
    }

    private synchronized void onSnapshotSuccess(Multimap<Bundle, URL> proposedNewState, YangStoreSnapshotImpl snapshot) {
        // consistent state
        // merge into
        consistentBundlesToYangURLs.clear();
        consistentBundlesToYangURLs.putAll(proposedNewState);

        logger.debug("Yang store updated to new consistent state containing {}", consistentBundlesToYangURLs);

        // If we cleared up some inconsistent models, report that
        if (!inconsistentBundlesToYangURLs.isEmpty()) {
            inconsistentBundlesToYangURLs.clear();
            logger.info("Yang store updated to new consistent state containing {} yang files", consistentBundlesToYangURLs.size());
        }

        updateCache(snapshot);
        cache.setInconsistentURLsForReporting(Collections.<URL> emptySet());
    }

    private synchronized void updateCache(YangStoreSnapshotImpl snapshot) {
        cache.cacheYangStore(consistentBundlesToYangURLs, snapshot);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        logger.debug("Modified bundle {} {} {}", bundle, event, object);
    }

    /**
     * If removing YANG files makes yang store inconsistent, method {@link #getYangStoreSnapshot()}
     * will throw exception. There is no rollback.
     */
    @Override
    public synchronized void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        logger.debug("Removed bundle {} {} {}", bundle, event, object);
        inconsistentBundlesToYangURLs.removeAll(bundle);
        consistentBundlesToYangURLs.removeAll(bundle);
    }

    @Override
    public synchronized YangStoreSnapshot getYangStoreSnapshot()
            throws YangStoreException {
        Optional<YangStoreSnapshot> yangStoreOpt = cache.getSnapshotIfPossible(consistentBundlesToYangURLs);
        if (yangStoreOpt.isPresent()) {
            logger.debug("Returning cached yang store {}", yangStoreOpt.get());
            return yangStoreOpt.get();
        }

        YangStoreSnapshotImpl snapshot = createSnapshot(mbeParser, consistentBundlesToYangURLs);
        updateCache(snapshot);
        return snapshot;
    }

    private static YangStoreSnapshotImpl createSnapshot(MbeParser mbeParser, Multimap<Bundle, URL> multimap) throws YangStoreException {
        try {
            YangStoreSnapshotImpl yangStoreSnapshot = mbeParser.parseYangFiles(fromUrlsToInputStreams(multimap));
            logger.trace("{} module entries parsed successfully from {} yang files",
                    yangStoreSnapshot.countModuleMXBeanEntries(), multimap.values().size());
            return yangStoreSnapshot;
        } catch (RuntimeException e) {
            StringBuffer causeStr = new StringBuffer();
            Throwable cause = e;
            while (cause != null) {
                causeStr.append(e.getMessage());
                causeStr.append("\n");
                cause = e.getCause();
            }
            throw new YangStoreException("Unable to parse yang files. \n" + causeStr.toString() +
                    "URLs: " + multimap, e);
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

    public synchronized void setMaybeBlacklist(Optional<Pattern> maybeBlacklistPattern) {
        maybeBlacklist = maybeBlacklistPattern;
        cache.invalidate();
    }
}
