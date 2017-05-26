/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;

public class StringUtilTest {
    @Test
    public void testPrefixAndJoin() {
        assertEquals(" extends p1.Foo,Bar", StringUtil.prefixAndJoin(asList(
                new FullyQualifiedName("p1", "Foo"), new FullyQualifiedName("", "Bar")), "extends"));
    }

    @Test
    public void testAddAsterixAtEachLineStart() {
        String input = "foo   \nbar";
        String expectedOutput = "* foo\n* bar\n";
        assertEquals(expectedOutput, StringUtil.addAsterixAtEachLineStart(input));
    }

    @Test
    @Ignore
    public void testCopyright() throws IOException {
        assertTrue(StringUtil.loadCopyright().isPresent());
    }

    @Test
    public void testFormatting() {
        {
            String input = "  \tpack;\n" +
                "class Bar{ \n" +
                " method() {\n" +
                "  body\n" +
                "}\n" +
                "  }";
            String expected = "pack;\n" +
                "class Bar{\n" +
                "    method() {\n" +
                "        body\n" +
                "    }\n" +
                "}\n";
            assertEquals(expected, StringUtil.formatJavaSource(input));
        }
        {
            String input = "{\n" +
                    "bar\n" +
                    "}\n" +
                    "\n\nbaz\n\n\n\n";
            String expected = "{\n" +
                    "    bar\n" +
                    "}\n\n" +
                    "baz\n";
            assertEquals(expected, StringUtil.formatJavaSource(input));
        }
    }
}
