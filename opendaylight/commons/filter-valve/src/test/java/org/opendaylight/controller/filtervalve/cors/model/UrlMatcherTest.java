/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.model;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import org.junit.Test;

public class UrlMatcherTest {
    UrlMatcher<String> urlMatcher;

    @Test
    public void test() throws Exception {
        final String defaultFilter = "default";
        final String exactMatchFilter = "someFilter";
        final String jspFilter = "jspFilter";
        final String exactMatch = "/somePath";
        final String prefixFilter = "prefixFilter";
        LinkedHashMap<String, String> patternMap = new LinkedHashMap<>();
        patternMap.put(exactMatch, exactMatchFilter);
        patternMap.put("/*", defaultFilter);
        patternMap.put("*.jsp", jspFilter);
        patternMap.put("/foo/*", prefixFilter);
        urlMatcher = new UrlMatcher<>(patternMap);
        assertMatches("/abc", defaultFilter);
        assertMatches(exactMatch, exactMatchFilter, defaultFilter);
        assertMatches("/some.jsp", defaultFilter, jspFilter);
        assertMatches("/foo/bar", defaultFilter, prefixFilter);
        assertMatches("/foo/bar.jsp", defaultFilter, jspFilter, prefixFilter);
    }

    public void assertMatches(String testedPath, String... filters) {
        assertEquals(asList(filters), urlMatcher.findMatchingFilters(testedPath));
    }

}
