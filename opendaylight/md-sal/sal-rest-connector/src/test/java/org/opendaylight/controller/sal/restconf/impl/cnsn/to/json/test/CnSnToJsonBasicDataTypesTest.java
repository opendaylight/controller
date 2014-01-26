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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class CnSnToJsonBasicDataTypesTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/simple-data-types");
    }

    @Test
    public void simpleYangDataTest() {

        CompositeNode compositeNode = TestUtils.readInputToCnSn("/cnsn-to-json/simple-data-types/xml/data.xml",
                XmlToCompositeNodeProvider.INSTANCE);

        String jsonOutput = null;

        TestUtils.normalizeCompositeNode(compositeNode, modules, "simple-data-types:cont");

        try {
            jsonOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, dataSchemaNode,
                    StructuredDataToJsonProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
        }
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
        List<String> loadedLfs = new ArrayList<>();
        boolean enumChecked = false;
        boolean bitsChecked = false;
        boolean lfdecimal6Checked = false;
        boolean lfdecimal4Checked = false;
        boolean lfdecimal3Checked = false;
        boolean lfdecimal2Checked = false;
        boolean lfdecimal1Checked = false;
        boolean lfbool1Checked = false;
        boolean lfbool2Checked = false;
        boolean lfstrChecked = false;
        boolean lfbinaryChecked = false;
        boolean lfemptyChecked = false;
        boolean lfstr1Checked = false;
        boolean lfidentityrefChecked = false;

        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            JsonToken peek = null;
            try {
                peek = jReader.peek();
            } catch (IOException e) {
                assertTrue("Key " + keyName + " has incorrect value for specifed type", false);
            }

            if (keyName.startsWith("lfnint") || keyName.startsWith("lfnuint")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                try {
                    jReader.nextLong();
                } catch (NumberFormatException e) {
                    assertTrue("Key " + keyName + " has incorrect value - " + e.getMessage(), false);
                }
                loadedLfs.add(keyName.substring(3));
            } else if (keyName.equals("identityref1")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
                assertEquals("simple-data-types:iden", jReader.nextString());
                lfidentityrefChecked = true;
            } else if (keyName.equals("lfstr")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
                assertEquals("lfstr", jReader.nextString());
                lfstrChecked = true;
            } else if (keyName.equals("lfstr1")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
                assertEquals("", jReader.nextString());
                lfstr1Checked = true;
            } else if (keyName.equals("lfbool1")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.BOOLEAN, peek);
                assertEquals(true, jReader.nextBoolean());
                lfbool1Checked = true;
            } else if (keyName.equals("lfbool2")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.BOOLEAN, peek);
                assertEquals(false, jReader.nextBoolean());
                lfbool2Checked = true;
            } else if (keyName.equals("lfbool3")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.BOOLEAN, peek);
                assertEquals(false, jReader.nextBoolean());
            } else if (keyName.equals("lfdecimal1")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                assertEquals(new Double(43.32), (Double) jReader.nextDouble());
                lfdecimal1Checked = true;
            } else if (keyName.equals("lfdecimal2")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                assertEquals(new Double(-0.43), (Double) jReader.nextDouble());
                lfdecimal2Checked = true;
            } else if (keyName.equals("lfdecimal3")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                assertEquals(new Double(43), (Double) jReader.nextDouble());
                lfdecimal3Checked = true;
            } else if (keyName.equals("lfdecimal4")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                assertEquals(new Double(43E3), (Double) jReader.nextDouble());
                lfdecimal4Checked = true;
            } else if (keyName.equals("lfdecimal6")) {
                assertEquals("Key " + keyName + " has incorrect type", JsonToken.NUMBER, peek);
                assertEquals(new Double(33.12345), (Double) jReader.nextDouble());
                lfdecimal6Checked = true;
            } else if (keyName.equals("lfenum")) {
                assertEquals("enum3", jReader.nextString());
                enumChecked = true;
            } else if (keyName.equals("lfbits")) {
                assertEquals("bit3 bit2", jReader.nextString());
                bitsChecked = true;
            } else if (keyName.equals("lfbinary")) {
                assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", jReader.nextString());
                lfbinaryChecked = true;
            } else if (keyName.equals("lfempty")) {
                jReader.beginArray();
                jReader.nextNull();
                jReader.endArray();
                lfemptyChecked = true;
            } else if (keyName.startsWith("lfunion")) {
                checkLfUnion(jReader, keyName, peek);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }

        }
        Collections.sort(loadedLfs);
        String expectedLfsStr = "[int16Max, int16Min, int32Max, int32Min, int64Max, int64Min, int8Max, int8Min, uint16Max, uint32Max, uint8Max]";
        String actualLfsStr = loadedLfs.toString();
        assertEquals("Some leaves are missing", expectedLfsStr, actualLfsStr);
        assertTrue("Enum wasn't checked", enumChecked);
        assertTrue("Bits wasn't checked", bitsChecked);
        assertTrue("Decimal1 wasn't checked", lfdecimal1Checked);
        assertTrue("Decimal2 wasn't checked", lfdecimal2Checked);
        assertTrue("Decimal3 wasn't checked", lfdecimal3Checked);
        assertTrue("Decimal4 wasn't checked", lfdecimal4Checked);
        assertTrue("Decimal5 wasn't checked", lfdecimal6Checked);
        assertTrue("lfbool1 wasn't checked", lfbool1Checked);
        assertTrue("lfbool2 wasn't checked", lfbool2Checked);
        assertTrue("lfstr wasn't checked", lfstrChecked);
        assertTrue("lfstr1 wasn't checked", lfstr1Checked);
        assertTrue("lfbinary wasn't checked", lfbinaryChecked);
        assertTrue("lfempty wasn't checked", lfemptyChecked);
        assertTrue("lfidentityref wasn't checked", lfidentityrefChecked);
        jReader.endObject();
    }

    private void checkLfUnion(JsonReader jReader, String keyName, JsonToken peek) throws IOException {
        if (keyName.equals("lfunion1")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("324", jReader.nextString());
        } else if (keyName.equals("lfunion2")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("33.3", jReader.nextString());
        } else if (keyName.equals("lfunion3")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("55", jReader.nextString());
        } else if (keyName.equals("lfunion4")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("true", jReader.nextString());
        } else if (keyName.equals("lfunion5")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("true", jReader.nextString());
        } else if (keyName.equals("lfunion6")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("false", jReader.nextString());
        } else if (keyName.equals("lfunion7")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("", jReader.nextString());
        } else if (keyName.equals("lfunion8")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("", jReader.nextString());
        } else if (keyName.equals("lfunion9")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("", jReader.nextString());
        } else if (keyName.equals("lfunion10")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("bt1", jReader.nextString());
        } else if (keyName.equals("lfunion11")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("33", jReader.nextString());
        } else if (keyName.equals("lfunion12")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("false", jReader.nextString());
        } else if (keyName.equals("lfunion13")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("44", jReader.nextString());
        } else if (keyName.equals("lfunion14")) {
            assertEquals("Key " + keyName + " has incorrect type", JsonToken.STRING, peek);
            assertEquals("21", jReader.nextString());
        }
    }
}
