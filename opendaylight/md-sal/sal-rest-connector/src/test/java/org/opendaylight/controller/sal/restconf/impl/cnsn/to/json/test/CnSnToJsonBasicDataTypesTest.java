/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.common.collect.Maps;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.Node;

public class CnSnToJsonBasicDataTypesTest extends YangAndXmlAndDataSchemaLoader {

    static abstract class LeafVerifier {

        Object expectedValue;
        JsonToken expectedToken;

        LeafVerifier(Object expectedValue, JsonToken expectedToken) {
            this.expectedValue = expectedValue;
            this.expectedToken = expectedToken;
        }

        abstract Object getActualValue(JsonReader reader) throws IOException;

        void verify(JsonReader reader, String keyName) throws IOException {
            assertEquals("Json value for key " + keyName, expectedValue, getActualValue(reader));
        }

        JsonToken expectedTokenType() {
            return expectedToken;
        }
    }

    static class BooleanVerifier extends LeafVerifier {

        public BooleanVerifier(boolean expected) {
            super(expected, JsonToken.BOOLEAN);
        }

        @Override
        Object getActualValue(JsonReader reader) throws IOException {
            return reader.nextBoolean();
        }
    }

    static class NumberVerifier extends LeafVerifier {

        public NumberVerifier(Number expected) {
            super(expected, JsonToken.NUMBER);
        }

        @Override
        Object getActualValue(JsonReader reader) throws IOException {
            if (expectedValue instanceof Double) {
                return reader.nextDouble();
            } else if (expectedValue instanceof Long) {
                return reader.nextLong();
            } else if (expectedValue instanceof Integer) {
                return reader.nextInt();
            }

            return null;
        }
    }

    static class StringVerifier extends LeafVerifier {

        StringVerifier(String expected) {
            super(expected, JsonToken.STRING);
        }

        @Override
        Object getActualValue(JsonReader reader) throws IOException {
            return reader.nextString();
        }
    }

    static class EmptyVerifier extends LeafVerifier {

        EmptyVerifier() {
            super(null, null);
        }

        @Override
        Object getActualValue(JsonReader reader) throws IOException {
            reader.beginArray();
            reader.nextNull();
            reader.endArray();
            return null;
        }

    }

    static class ComplexAnyXmlVerifier extends LeafVerifier {

        ComplexAnyXmlVerifier() {
            super(null, JsonToken.BEGIN_OBJECT);
        }

        @Override
        void verify(JsonReader reader, String keyName) throws IOException {

            reader.beginObject();
            String innerKey = reader.nextName();
            assertEquals("Json reader child key for " + keyName, "data", innerKey);
            assertEquals("Json token type for key " + innerKey, JsonToken.BEGIN_OBJECT, reader.peek());

            reader.beginObject();
            verifyLeaf(reader, innerKey, "leaf1", "leaf1-value");
            verifyLeaf(reader, innerKey, "leaf2", "leaf2-value");

            String nextName = reader.nextName();
            assertEquals("Json reader child key for " + innerKey, "leaf-list", nextName);
            reader.beginArray();
            assertEquals("Json value for key " + nextName, "leaf-list-value1", reader.nextString());
            assertEquals("Json value for key " + nextName, "leaf-list-value2", reader.nextString());
            reader.endArray();

            nextName = reader.nextName();
            assertEquals("Json reader child key for " + innerKey, "list", nextName);
            reader.beginArray();
            verifyNestedLists(reader, 1);
            verifyNestedLists(reader, 3);
            reader.endArray();

            reader.endObject();
            reader.endObject();
        }

        void verifyNestedLists(JsonReader reader, int leafNum) throws IOException {
            reader.beginObject();

            String nextName = reader.nextName();
            assertEquals("Json reader next name", "nested-list", nextName);

            reader.beginArray();

            reader.beginObject();
            verifyLeaf(reader, "nested-list", "nested-leaf", "nested-value" + leafNum++);
            reader.endObject();

            reader.beginObject();
            verifyLeaf(reader, "nested-list", "nested-leaf", "nested-value" + leafNum);
            reader.endObject();

            reader.endArray();
            reader.endObject();
        }

        void verifyLeaf(JsonReader reader, String parent, String name, String value) throws IOException {
            String nextName = reader.nextName();
            assertEquals("Json reader child key for " + parent, name, nextName);
            assertEquals("Json token type for key " + parent, JsonToken.STRING, reader.peek());
            assertEquals("Json value for key " + nextName, value, reader.nextString());
        }

        @Override
        Object getActualValue(JsonReader reader) throws IOException {
            return null;
        }
    }

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/simple-data-types");
    }

    @Test
    public void simpleYangDataTest() throws Exception {

        Node<?> node = TestUtils.readInputToCnSn("/cnsn-to-json/simple-data-types/xml/data.xml",
                XmlToCompositeNodeProvider.INSTANCE);

        TestUtils.normalizeCompositeNode(node, modules, "simple-data-types:cont");

        String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(node, modules, dataSchemaNode,
                StructuredDataToJsonProvider.INSTANCE);

        assertNotNull(jsonOutput);

        verifyJsonOutput(jsonOutput);
    }

    private void verifyJsonOutput(String jsonOutput) {
        StringReader strReader = new StringReader(jsonOutput);
        JsonReader jReader = new JsonReader(strReader);

        String exception = null;
        try {
            jsonReadCont(jReader);
        } catch (IOException e) {
            exception = e.getMessage();
        }

        assertNull("Error during reading Json output: " + exception, exception);
    }

    private void jsonReadCont(JsonReader jReader) throws IOException {
        jReader.beginObject();
        assertNotNull("cont1 is missing.", jReader.hasNext());

        // Cont dataFromJson = new Cont(jReader.nextName());
        jReader.nextName();
        jsonReadContElements(jReader);

        assertFalse("cont shouldn't have other element.", jReader.hasNext());
        jReader.endObject();
        // return dataFromJson;
    }

    private void jsonReadContElements(JsonReader jReader) throws IOException {
        jReader.beginObject();

        Map<String, LeafVerifier> expectedMap = Maps.newHashMap();
        expectedMap.put("lfnint8Min", new NumberVerifier(Integer.valueOf(-128)));
        expectedMap.put("lfnint8Max", new NumberVerifier(Integer.valueOf(127)));
        expectedMap.put("lfnint16Min", new NumberVerifier(Integer.valueOf(-32768)));
        expectedMap.put("lfnint16Max", new NumberVerifier(Integer.valueOf(32767)));
        expectedMap.put("lfnint32Min", new NumberVerifier(Integer.valueOf(-2147483648)));
        expectedMap.put("lfnint32Max", new NumberVerifier(Long.valueOf(2147483647)));
        expectedMap.put("lfnint64Min", new NumberVerifier(Long.valueOf(-9223372036854775808L)));
        expectedMap.put("lfnint64Max", new NumberVerifier(Long.valueOf(9223372036854775807L)));
        expectedMap.put("lfnuint8Max", new NumberVerifier(Integer.valueOf(255)));
        expectedMap.put("lfnuint16Max", new NumberVerifier(Integer.valueOf(65535)));
        expectedMap.put("lfnuint32Max", new NumberVerifier(Long.valueOf(4294967295L)));
        expectedMap.put("lfstr", new StringVerifier("row1\\\\\\nro\\\"w2\\nrow3"));  //output should containe "row1\\\nro\"w2\nrow3"
        expectedMap.put("lfstr1", new StringVerifier(""));
        expectedMap.put("lfbool1", new BooleanVerifier(true));
        expectedMap.put("lfbool2", new BooleanVerifier(false));
        expectedMap.put("lfbool3", new BooleanVerifier(false));
        expectedMap.put("lfdecimal1", new NumberVerifier(new Double(43.32)));
        expectedMap.put("lfdecimal2", new NumberVerifier(new Double(-0.43)));
        expectedMap.put("lfdecimal3", new NumberVerifier(new Double(43)));
        expectedMap.put("lfdecimal4", new NumberVerifier(new Double(43E3)));
        expectedMap.put("lfdecimal6", new NumberVerifier(new Double(33.12345)));
        expectedMap.put("lfenum", new StringVerifier("enum3"));
        expectedMap.put("lfbits", new StringVerifier("bit3 bit2"));
        expectedMap.put("lfbinary", new StringVerifier("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"));
        expectedMap.put("lfunion1", new StringVerifier("324"));
        expectedMap.put("lfunion2", new StringVerifier("33.3"));
        expectedMap.put("lfunion3", new StringVerifier("55"));
        expectedMap.put("lfunion4", new StringVerifier("true"));
        expectedMap.put("lfunion5", new StringVerifier("true"));
        expectedMap.put("lfunion6", new StringVerifier("10"));
        expectedMap.put("lfunion7", new StringVerifier(""));
        expectedMap.put("lfunion8", new StringVerifier(""));
        expectedMap.put("lfunion9", new StringVerifier(""));
        expectedMap.put("lfunion10", new StringVerifier("bt1"));
        expectedMap.put("lfunion11", new StringVerifier("33"));
        expectedMap.put("lfunion12", new StringVerifier("false"));
        expectedMap.put("lfunion13", new StringVerifier("b1"));
        expectedMap.put("lfunion14", new StringVerifier("zero"));
        expectedMap.put("lfempty", new EmptyVerifier());
        expectedMap.put("identityref1", new StringVerifier("simple-data-types:iden"));
        expectedMap.put("complex-any", new ComplexAnyXmlVerifier());
        expectedMap.put("simple-any", new StringVerifier("simple"));
        expectedMap.put("empty-any", new StringVerifier(""));

        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            JsonToken peek = jReader.peek();

            LeafVerifier verifier = expectedMap.remove(keyName);
            assertNotNull("Found unexpected leaf: " + keyName, verifier);

            JsonToken expToken = verifier.expectedTokenType();
            if (expToken != null) {
                assertEquals("Json token type for key " + keyName, expToken, peek);
            }

            verifier.verify(jReader, keyName);
        }

        if (!expectedMap.isEmpty()) {
            fail("Missing leaf nodes in Json output: " + expectedMap.keySet());
        }

        jReader.endObject();
    }

    @Test
    public void testBadData() throws Exception {

        try {
            Node<?> node = TestUtils.readInputToCnSn("/cnsn-to-json/simple-data-types/xml/bad-data.xml",
                    XmlToCompositeNodeProvider.INSTANCE);

            TestUtils.normalizeCompositeNode(node, modules, "simple-data-types:cont");
            fail("Expected RestconfDocumentedException");
        } catch (RestconfDocumentedException e) {
            assertEquals("getErrorTag", ErrorTag.INVALID_VALUE, e.getErrors().get(0).getErrorTag());
        }
    }
}
