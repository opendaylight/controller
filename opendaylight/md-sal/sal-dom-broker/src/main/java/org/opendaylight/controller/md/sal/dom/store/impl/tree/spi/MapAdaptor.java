/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romix.scala.collection.concurrent.TrieMap;

/*
 * A simple layer on top of maps, which performs snapshot mediation and optimization of
 * what the underlying implementation is.
 */
final class MapAdaptor {
    public static final int DEFAULT_TRIEMAP_CUTOFF = 100;
    public static final String TRIEMAP_CUTOFF_PROP = "org.opendaylight.yangtools.datatree.triemap.cutoff";

    private static final Logger LOG = LoggerFactory.getLogger(MapAdaptor.class);
    private static final MapAdaptor INSTANCE = new MapAdaptor();
    private final int trieMapCutoff;

    private MapAdaptor() {
        int cutoff = DEFAULT_TRIEMAP_CUTOFF;

        try {
            final String p = System.getProperty(TRIEMAP_CUTOFF_PROP);
            if (p != null) {
                try {
                    int pi = Integer.valueOf(p);
                    if (pi < 0) {
                        LOG.warn("Ignoring illegal value of {}: has to be a positive number", TRIEMAP_CUTOFF_PROP);
                    } else {
                        cutoff = pi;
                    }
                } catch (NumberFormatException e) {
                    LOG.warn("Ignoring non-numerical value of {}", TRIEMAP_CUTOFF_PROP, e);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to get {}", TRIEMAP_CUTOFF_PROP, e);
        }

        LOG.debug("Configured HashMap/TrieMap cutoff at {} entries", cutoff);
        trieMapCutoff = cutoff;
    }

    public static MapAdaptor getInstance() {
        return INSTANCE;
    }

    /**
     * Input is treated is supposed to be left unmodified, result must be mutable.
     *
     * @param input
     * @return
     */
    public <K, V> Map<K, V> takeSnapshot(final Map<K, V> input) {
        if (input instanceof TrieMapFacade) {
            return ((TrieMapFacade<K, V>)input).toReadWrite();
        }

        LOG.trace("Converting input {} to a HashMap", input);

        // FIXME: be a bit smart about allocation based on observed size

        final Map<K, V> ret = new HashMap<>(input);
        LOG.trace("Read-write HashMap is {}", ret);
        return ret;
    }

    /**
     * Input will be thrown away, result will be retained for read-only access or
     * {@link #takeSnapshot(Map)} purposes.
     *
     * @param input
     * @return
     */
    public <K, V> Map<K, V> optimize(final Map<K, V> input) {
        final int size = input.size();

        if (size == 0) {
            /*
             * No-brainer :)
             */
            LOG.trace("Reducing input {} to an empty map", input);
            return Collections.<K, V>emptyMap();
        }
        if (size == 1) {
            /*
             * FIXME: Favor memory: create a singleton instance
             */
        }
        if (size < trieMapCutoff) {
            /*
             * Favor access speed: use a HashMap and copy it on modification.
             */
            if (input instanceof HashMap) {
                return input;
            }

            LOG.trace("Copying input {} to a HashMap ({} entries)", input, size);
            final Map<K, V> ret = new HashMap<>(input);
            LOG.trace("Read-only HashMap is {}", ret);
            return ret;
        }

        /*
         * Favor isolation speed: use a TrieMap and perform snapshots
         *
         * This one is a bit tricky, as the TrieMap is concurrent and does not
         * keep an uptodate size. Updating it requires a full walk -- which is
         * O(N) and we want to avoid that. So we wrap it in an interceptor,
         * which will maintain the size for us.
         */
        if (input instanceof TrackingTrieMapFacade) {
            return ((TrackingTrieMapFacade<K, V>)input).toReadOnly();
        }

        LOG.trace("Copying input {} to a TrieMap ({} entries)", input, size);
        final TrieMap<K, V> map = TrieMap.empty();
        map.putAll(input);
        final Map<K, V> ret = new TrieMapFacade<>(map, size);
        LOG.trace("Read-only TrieMap is {}", ret);
        return ret;
    }
}
