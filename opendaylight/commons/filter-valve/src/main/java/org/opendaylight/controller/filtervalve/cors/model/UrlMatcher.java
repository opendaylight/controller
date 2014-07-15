/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.immutableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Match incoming URL with user defined patterns according to servlet specification.
 * In the Web application deployment descriptor, the following syntax is used to define mappings:
 * <ul>
 * <li>A string beginning with a ‘/’ character and ending with a ‘/*’ suffix is used for path mapping.</li>
 * <li>A string beginning with a ‘*.’ prefix is used as an extension mapping.</li>
 * <li>All other strings are used for exact matches only.</li>
 * </ul>
 */
public class UrlMatcher<FILTER> {
    private static final Logger logger = LoggerFactory.getLogger(UrlMatcher.class);
    // order index for each FILTER is kept as Entry.value
    private final Map<String, Entry<FILTER, Integer>> prefixMap = new HashMap<>(); // contains patterns ending with '/*', '*' is stripped from each key
    private final Map<String, Entry<FILTER, Integer>> suffixMap = new HashMap<>(); // contains patterns starting with '*.' prefix, '*' is stripped from each key
    private final Map<String, Entry<FILTER, Integer>> exactMatchMap = new HashMap<>(); // contains exact matches only

    /**
     * @param patternMap order preserving map containing path info pattern as key
     */
    public UrlMatcher(LinkedHashMap<String, FILTER> patternMap) {
        int idx = 0;
        for (Entry<String, FILTER> entry : patternMap.entrySet()) {
            idx++;
            String pattern = checkNotNull(entry.getKey());
            FILTER value = entry.getValue();
            Entry<FILTER, Integer> valueWithIdx = immutableEntry(value, idx);
            if (pattern.startsWith("/") && pattern.endsWith("/*")) {
                pattern = pattern.substring(0, pattern.length() - 1);
                prefixMap.put(pattern, valueWithIdx);
            } else if (pattern.startsWith("*.")) {
                pattern = pattern.substring(1);
                suffixMap.put(pattern, valueWithIdx);
            } else {
                exactMatchMap.put(pattern, valueWithIdx);
            }
        }
    }

    /**
     * Find filters matching path
     *
     * @param path relative and decoded path to resource
     * @return list of matching filters
     */
    public List<FILTER> findMatchingFilters(String path) {
        checkNotNull(path);
        TreeMap<Integer, FILTER> sortedMap = new TreeMap<>();
        // add matching prefixes
        for (Entry<String, Entry<FILTER, Integer>> prefixEntry : prefixMap.entrySet()) {
            if (path.startsWith(prefixEntry.getKey())) {
                put(sortedMap, prefixEntry.getValue());
            }
        }
        // add matching suffixes
        for (Entry<String, Entry<FILTER, Integer>> suffixEntry : suffixMap.entrySet()) {
            if (path.endsWith(suffixEntry.getKey())) {
                put(sortedMap, suffixEntry.getValue());
            }
        }
        // add exact match
        Entry<FILTER, Integer> exactMatch = exactMatchMap.get(path);
        if (exactMatch != null) {
            put(sortedMap, exactMatch);
        }
        ArrayList<FILTER> filters = new ArrayList<>(sortedMap.values());
        logger.trace("Matching filters for path {} are {}", path, filters);
        return filters;
    }

    private void put(TreeMap<Integer, FILTER> sortedMap, Entry<FILTER, Integer> entry) {
        sortedMap.put(entry.getValue(), entry.getKey());
    }
}
