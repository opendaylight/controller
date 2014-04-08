package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
