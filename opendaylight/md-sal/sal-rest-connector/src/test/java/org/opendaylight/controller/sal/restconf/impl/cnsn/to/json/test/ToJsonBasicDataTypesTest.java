package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class ToJsonBasicDataTypesTest {

    @Test
    public void simpleYangDataTest() {
        String jsonOutput = "";
        CompositeNode compositeNode = TestUtils.loadCompositeNode("/cnsn-to-json/simple-data-types/xml/data.xml");

        Set<Module> modules = TestUtils.resolveModules("/cnsn-to-json/simple-data-types");
        assertEquals(1, modules.size());
        Module module = TestUtils.resolveModule(null, modules);
        assertNotNull(module);
        DataSchemaNode dataSchemaNode = TestUtils.resolveDataSchemaNode(module, null);
        assertNotNull(dataSchemaNode);

        TestUtils.normalizeCompositeNode(compositeNode, modules, dataSchemaNode, "simple-data-types:cont");

        try {
            jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(compositeNode, modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            assertTrue(false); // shouldn't get here
        }

        System.out.println(jsonOutput);
        verifyJsonOutput(jsonOutput);
    }

    private CompositeNode prepareData() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
                ModifyAction.CREATE, null);

        List<Node<?>> childNodes = new ArrayList<>();
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint8Min"), cont, (byte) -128,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint8Max"), cont, (byte) 127,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint16Min"), cont, (short) -32768,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint16Max"), cont, (short) 32767,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint32Min"), cont,
                (int) -2147483648, ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint32Max"), cont, (int) 2147483647,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint64Min"), cont, new Long(
                "-9223372036854775807"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnint64Max"), cont, new Long(
                "9223372036854775807"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnuint8Max"), cont, (short) 255,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnuint16Max"), cont, (int) 65535,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfnuint32Max"), cont, new Long(
                "4294967295"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfstr"), cont, "lfstr",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfstr1"), cont, "",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfbool1"), cont, Boolean.TRUE,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfbool2"), cont, Boolean.FALSE,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfdecimal1"), cont, new BigDecimal(
                "43.32"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfdecimal2"), cont, new BigDecimal(
                "-0.43"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfdecimal3"), cont, new BigDecimal(
                "43"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfdecimal4"), cont, new BigDecimal(
                "43E3"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfdecimal6"), cont, new BigDecimal(
                "33.12345"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfenum"), cont, "enum3",
                ModifyAction.CREATE, null));

        HashSet<String> bits = new HashSet<String>();
        bits.add("bit3");
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfbits"), cont, bits,
                ModifyAction.CREATE, null));

        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfbinary"), cont, DatatypeConverter
                .parseBase64Binary("AAaacdabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"),
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfempty"), cont, null,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion1"), cont, (int) 324,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion2"), cont, new BigDecimal(
                "33.3"), ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion3"), cont, "55",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion4"), cont, Boolean.TRUE,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion5"), cont, "true",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion6"), cont, "false",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion7"), cont, null,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion8"), cont, "",
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion9"), cont, "",
                ModifyAction.CREATE, null));

        HashSet<String> bits2 = new HashSet<String>();
        bits2.add("bt1");
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion10"), cont, bits2,
                ModifyAction.CREATE, null));

        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion11"), cont, (short) 33,
                ModifyAction.CREATE, null));
        childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfunion12"), cont, Boolean.FALSE,
                ModifyAction.CREATE, null));
        try {
            childNodes.add(NodeFactory.createMutableSimpleNode(TestUtils.buildQName("identityref1"), cont, new QName(
                    new URI("simple:data:types"), "iden"), ModifyAction.CREATE, null));
        } catch (URISyntaxException e) {
        }

        cont.getChildren().addAll(childNodes);

        cont.init();

        return cont;
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
        boolean exceptForDecimal5Raised = false;
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
        // boolean lfref1Checked = false;
        boolean lfemptyChecked = false;
        boolean lfstr1Checked = false;
        boolean lfidentityrefChecked = false;

        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            JsonToken peek = null;
            try {
                peek = jReader.peek();
            } catch (IOException e) {
                if (keyName.equals("lfdecimal5")) {
                    exceptForDecimal5Raised = true;
                } else {
                    assertTrue("Key " + keyName + " has incorrect value for specifed type", false);
                }
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
