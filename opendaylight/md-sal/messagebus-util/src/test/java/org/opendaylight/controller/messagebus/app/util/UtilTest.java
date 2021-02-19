/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Unit tests for Util.
 *
 * @author ppalmar
 */
@Deprecated(forRemoval = true)
public class UtilTest {

    @Test
    public void testResultFor() throws Exception {
        {
            final String expectedResult = "dummy string";
            RpcResult<String> rpcResult = Util.resultRpcSuccessFor(expectedResult).get();
            assertEquals(expectedResult, rpcResult.getResult());
            assertTrue(rpcResult.isSuccessful());
            assertTrue(rpcResult.getErrors().isEmpty());
        }
        {
            final Integer expectedResult = 42;
            RpcResult<Integer> rpcResult = Util.resultRpcSuccessFor(expectedResult).get();
            assertEquals(expectedResult, rpcResult.getResult());
            assertTrue(rpcResult.isSuccessful());
            assertTrue(rpcResult.getErrors().isEmpty());
        }
    }

    @Test
    public void testExpandQname() {
        // match no path because the list of the allowed paths is empty
        {
            final List<SchemaPath> paths = new ArrayList<>();
            final Pattern regexPattern = Pattern.compile(".*"); // match everything
            final List<SchemaPath> matchingPaths = Util.expandQname(paths, regexPattern);
            assertTrue(matchingPaths.isEmpty());
        }

        // match no path because of regex pattern
        {
            final List<SchemaPath> paths = createSchemaPathList();
            final Pattern regexPattern = Pattern.compile("^@.*");
            final List<SchemaPath> matchingPaths = Util.expandQname(paths, regexPattern);
            assertTrue(matchingPaths.isEmpty());
        }

        // match all paths
        {
            final List<SchemaPath> paths = createSchemaPathList();
            final Pattern regexPattern = Pattern.compile(".*");
            final List<SchemaPath> matchingPaths = Util.expandQname(paths, regexPattern);
            assertTrue(matchingPaths.contains(paths.get(0)));
            assertTrue(matchingPaths.contains(paths.get(1)));
            assertEquals(paths.size(), matchingPaths.size());
        }

        // match one path only
        {
            final List<SchemaPath> paths = createSchemaPathList();
            final Pattern regexPattern = Pattern.compile(".*yyy$");
            final List<SchemaPath> matchingPaths = Util.expandQname(paths, regexPattern);
            assertTrue(matchingPaths.contains(paths.get(1)));
            assertEquals(1, matchingPaths.size());
        }
    }

    private static List<SchemaPath> createSchemaPathList() {
        final QName qname1 = QName.create("urn:odl:xxx", "2015-01-01", "localName");
        final QName qname2 = QName.create("urn:odl:yyy", "2015-01-01", "localName");
        final SchemaPath path1 = SchemaPath.create(true, qname1);
        final SchemaPath path2 = SchemaPath.create(true, qname2);
        return Arrays.asList(path1, path2);
    }
}
