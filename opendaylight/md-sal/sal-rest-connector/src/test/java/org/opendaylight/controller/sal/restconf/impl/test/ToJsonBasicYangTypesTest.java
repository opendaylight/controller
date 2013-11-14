package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.*;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.structures.*;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

import com.google.gson.stream.*;

public class ToJsonBasicYangTypesTest {

    /**
     * Test of json output when as input are specified composite node with empty
     * data + YANG file
     */
    @Test
    public void compositeNodeAndYangWithJsonReaderEmptyDataTest() {
        String jsonOutput = TestUtils.convertCompositeNodeDataAndYangToJson(prepareCompositeNodeWithEmpties(),
                "/yang-to-json-conversion/simple-yang-types", "/yang-to-json-conversion/simple-yang-types/xml");
        verifyJsonOutputForEmpty(jsonOutput);
    }

    /**
     * Test of json output when as input are specified xml file (no empty
     * elements)and YANG file
     */
    @Test
    public void xmlAndYangTypesWithJsonReaderTest() {
        String jsonOutput = TestUtils.convertCompositeNodeDataAndYangToJson(
                TestUtils.loadCompositeNode("/yang-to-json-conversion/simple-yang-types/xml/data.xml"),
                "/yang-to-json-conversion/simple-yang-types", "/yang-to-json-conversion/simple-yang-types/xml");
        verifyJsonOutput(jsonOutput);
    }

    private void verifyJsonOutputForEmpty(String jsonOutput) {
        StringReader strReader = new StringReader(jsonOutput);
        JsonReader jReader = new JsonReader(strReader);

        String exception = null;
        Cont dataFromJson = null;
        try {
            dataFromJson = jsonReadCont1(jReader);
        } catch (IOException e) {
            exception = e.getMessage();
        }

        assertNotNull("Data structures from json are missing.", dataFromJson);
        checkDataFromJsonEmpty(dataFromJson);

        assertNull("Error during reading Json output: " + exception, exception);
    }

    private void verifyJsonOutput(String jsonOutput) {
        StringReader strReader = new StringReader(jsonOutput);
        JsonReader jReader = new JsonReader(strReader);

        String exception = null;
        Cont dataFromJson = null;
        try {
            dataFromJson = jsonReadCont1(jReader);
        } catch (IOException e) {
            exception = e.getMessage();
        }

        assertNotNull("Data structures from json are missing.", dataFromJson);
        checkDataFromJson(dataFromJson);

        assertNull("Error during reading Json output: " + exception, exception);
    }

    private Cont jsonReadCont1(JsonReader jReader) throws IOException {
        jReader.beginObject();
        assertNotNull("cont1 is missing.", jReader.hasNext());

        Cont dataFromJson = new Cont(jReader.nextName());
        dataFromJson = jsonReadCont1Elements(jReader, dataFromJson);

        assertFalse("cont shouldn't have other element.", jReader.hasNext());
        jReader.endObject();
        return dataFromJson;

    }

    private Cont jsonReadCont1Elements(JsonReader jReader, Cont redData) throws IOException {
        jReader.beginObject();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf11")) {
                redData.addLf(new Lf(keyName, nextValue(jReader)));
            } else if (keyName.equals("lflst11")) {
                LfLst lfLst = new LfLst(keyName);
                lfLst = jsonReadLflstValues(jReader, lfLst);
                redData.addLfLst(lfLst);
            } else if (keyName.equals("lflst12")) {
                LfLst lfLst = new LfLst(keyName);
                jsonReadLflstValues(jReader, lfLst);
                redData.addLfLst(lfLst);
            } else if (keyName.equals("lst11")) {
                Lst lst = new Lst(keyName);
                lst = jsonReadLst11(jReader, lst);
                redData.addLst(lst);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }
        }
        jReader.endObject();
        return redData;

    }

    private Lst jsonReadLst11(JsonReader jReader, Lst lst) throws IOException {
        jReader.beginArray();

        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst11Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst11Elements(JsonReader jReader) throws IOException {
        LstItem lstItem = new LstItem();
        jReader.beginObject();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf111")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            } else if (keyName.equals("lf112")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            } else if (keyName.equals("cont111")) {
                Cont cont = new Cont(keyName);
                cont = jsonReadCont111(jReader, cont);
                lstItem.addCont(cont);
            } else if (keyName.equals("lst111")) {
                Lst lst = new Lst(keyName);
                lst = jsonReadLst111(jReader, lst);
                lstItem.addLst(lst);
            } else if (keyName.equals("lst112")) {
                Lst lst = new Lst(keyName);
                lst = jsonReadLst112(jReader, lst);
                lstItem.addLst(lst);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private Lst jsonReadLst112(JsonReader jReader, Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst112Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst112Elements(JsonReader jReader) throws IOException {
        LstItem lstItem = new LstItem();
        jReader.beginObject();
        if (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1121")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;

    }

    private Lst jsonReadLst111(JsonReader jReader, Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst111Elements(JsonReader jReader) throws IOException {
        LstItem lstItem = new LstItem();
        jReader.beginObject();
        if (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1111")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private String nextValue(JsonReader jReader) throws IOException {
        if (jReader.peek().equals(JsonToken.NULL)) {
            jReader.nextNull();
            return null;
        } else {
            return jReader.nextString();
        }
    }

    private Cont jsonReadCont111(JsonReader jReader, Cont cont) throws IOException {
        jReader.beginObject();
        cont = jsonReadCont111Elements(jReader, cont);
        jReader.endObject();
        return cont;
    }

    private Cont jsonReadCont111Elements(JsonReader jReader, Cont cont) throws IOException {
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1111")) {
                cont.addLf(new Lf(keyName, nextValue(jReader)));
            } else if (keyName.equals("lflst1111")) {
                LfLst lfLst = new LfLst(keyName);
                lfLst = jsonReadLflstValues(jReader, lfLst);
                cont.addLfLst(lfLst);
            } else if (keyName.equals("lst1111")) {
                Lst lst = new Lst(keyName);
                lst = jsonReadLst1111(jReader, lst);
                cont.addLst(lst);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }
        }
        return cont;

    }

    private Lst jsonReadLst1111(JsonReader jReader, Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst1111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst1111Elements(JsonReader jReader) throws IOException {
        jReader.beginObject();
        LstItem lstItem = new LstItem();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1111A") || keyName.equals("lf1111B")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private LfLst jsonReadLflstValues(JsonReader jReader, LfLst lfLst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            lfLst.addLf(new Lf(nextValue(jReader)));
        }
        jReader.endArray();
        return lfLst;
    }

    private void checkDataFromJsonEmpty(Cont dataFromJson) {
        assertTrue(dataFromJson.getLfs().isEmpty());
        assertTrue(dataFromJson.getLfLsts().isEmpty());
        assertTrue(dataFromJson.getConts().isEmpty());

        Map<String, Lst> lsts = dataFromJson.getLsts();
        assertEquals(1, lsts.size());
        Lst lst11 = lsts.get("lst11");
        assertNotNull(lst11);
        Set<LstItem> lstItems = lst11.getLstItems();
        assertNotNull(lstItems);

        LstItem lst11_1 = null;
        LstItem lst11_2 = null;
        LstItem lst11_3 = null;
        for (LstItem lstItem : lstItems) {
            if (lstItem.getLfs().get("lf111").getValue().equals("1")) {
                lst11_1 = lstItem;
            } else if (lstItem.getLfs().get("lf111").getValue().equals("2")) {
                lst11_2 = lstItem;
            } else if (lstItem.getLfs().get("lf111").getValue().equals("3")) {
                lst11_3 = lstItem;
            }
        }

        assertNotNull(lst11_1);
        assertNotNull(lst11_2);
        assertNotNull(lst11_3);

        // lst11_1
        assertTrue(lst11_1.getLfLsts().isEmpty());
        assertEquals(1, lst11_1.getLfs().size());
        assertEquals(1, lst11_1.getConts().size());
        assertEquals(1, lst11_1.getLsts().size());
        assertEquals(lst11_1.getLsts().get("lst111"), new Lst("lst111").addLstItem(new LstItem().addLf("lf1111", "35"))
                .addLstItem(new LstItem().addLf("lf1111", "34")).addLstItem(new LstItem()).addLstItem(new LstItem()));
        assertEquals(lst11_1.getConts().get("cont111"), new Cont("cont111"));
        // : lst11_1

        // lst11_2
        assertTrue(lst11_2.getLfLsts().isEmpty());
        assertEquals(1, lst11_2.getLfs().size());
        assertEquals(1, lst11_2.getConts().size());
        assertEquals(1, lst11_2.getLsts().size());

        Cont lst11_2_cont111 = lst11_2.getConts().get("cont111");

        // -cont111
        assertNotNull(lst11_2_cont111);
        assertTrue(lst11_2_cont111.getLfs().isEmpty());
        assertEquals(1, lst11_2_cont111.getLfLsts().size());
        assertEquals(1, lst11_2_cont111.getLsts().size());
        assertTrue(lst11_2_cont111.getConts().isEmpty());

        assertEquals(new LfLst("lflst1111").addLf("1024").addLf("4096"), lst11_2_cont111.getLfLsts().get("lflst1111"));
        assertEquals(
                new Lst("lst1111").addLstItem(new LstItem().addLf("lf1111B", "4")).addLstItem(
                        new LstItem().addLf("lf1111A", "lf1111A str12")), lst11_2_cont111.getLsts().get("lst1111"));
        // :-cont111
        assertEquals(lst11_2.getLsts().get("lst112"), new Lst("lst112").addLstItem(new LstItem()));
        // : lst11_2

        // lst11_3
        assertEquals(1, lst11_3.getLfs().size());
        assertTrue(lst11_3.getLfLsts().isEmpty());
        assertTrue(lst11_3.getLsts().isEmpty());
        assertTrue(lst11_3.getLsts().isEmpty());

        // -cont111
        Cont lst11_3_cont111 = lst11_3.getConts().get("cont111");
        assertEquals(0, lst11_3_cont111.getLfs().size());
        assertEquals(0, lst11_3_cont111.getLfLsts().size());
        assertEquals(1, lst11_3_cont111.getLsts().size());
        assertTrue(lst11_3_cont111.getConts().isEmpty());

        assertEquals(new Lst("lst1111").addLstItem(new LstItem()).addLstItem(new LstItem()), lst11_3_cont111.getLsts()
                .get("lst1111"));
        // :-cont111
        // : lst11_3

    }

    private void checkDataFromJson(Cont dataFromJson) {
        assertNotNull(dataFromJson.getLfs().get("lf11"));
        assertEquals(dataFromJson.getLfs().get("lf11"), new Lf("lf11", "lf"));

        LfLst lflst11 = null;
        LfLst lflst12 = null;

        lflst11 = dataFromJson.getLfLsts().get("lflst11");
        lflst12 = dataFromJson.getLfLsts().get("lflst12");

        assertNotNull(lflst11);
        assertNotNull(lflst12);

        assertEquals(3, lflst11.getLfs().size());
        assertTrue(lflst11.getLfs().contains(new Lf("55")));
        assertTrue(lflst11.getLfs().contains(new Lf("56")));
        assertTrue(lflst11.getLfs().contains(new Lf("57")));

        assertEquals(3, lflst12.getLfs().size());
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str1")));
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str2")));
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str3")));

        assertEquals(1, dataFromJson.getLsts().size());
        Lst lst11 = dataFromJson.getLsts().get("lst11");
        assertNotNull(lst11);
        assertEquals(2, lst11.getLstItems().size());

        LstItem lst11_1 = null;
        LstItem lst11_2 = null;
        for (LstItem lstItem : lst11.getLstItems()) {
            Lf lf = lstItem.getLfs().get("lf111");
            if (lf != null && lf.getValue().equals("140")) {
                lst11_1 = lstItem;
            } else if (lf != null && lf.getValue().equals("141")) {
                lst11_2 = lstItem;
            }
        }

        checkLst11_1(lst11_1);
        checkLst11_2(lst11_2);
    }

    private void checkLst11_2(LstItem lst11_2) {
        assertNotNull(lst11_2);
        assertEquals(2, lst11_2.getLfs().size());
        assertEquals(1, lst11_2.getConts().size());
        assertEquals(2, lst11_2.getLsts().size());

        assertEquals(lst11_2.getLfs().get("lf112"), new Lf("lf112", "lf112 str2"));

        Cont lst11_2_cont = lst11_2.getConts().get("cont111");
        assertEquals(0, lst11_2_cont.getConts().size());
        assertEquals(1, lst11_2_cont.getLfLsts().size());
        assertEquals(1, lst11_2_cont.getLfs().size());
        assertEquals(1, lst11_2_cont.getLsts().size());

        // cont111 check
        assertEquals(new Lf("lf1111", "lf1111 str2"), lst11_2_cont.getLfs().get("lf1111"));
        assertEquals(new LfLst("lflst1111").addLf(new Lf("2049")).addLf(new Lf("1025")).addLf(new Lf("4097")),
                lst11_2_cont.getLfLsts().get("lflst1111"));

        assertNotNull(lst11_2_cont.getLsts().get("lst1111"));
        checkLst1111(lst11_2_cont.getLsts().get("lst1111").getLstItems(), new Lf("lf1111A", "lf1111A str21"), new Lf(
                "lf1111B", "5"), new Lf("lf1111A", "lf1111A str22"), new Lf("lf1111B", "8"));

        checkLst11x(lst11_2.getLsts().get("lst111"), new LstItem().addLf(new Lf("lf1111", "55")),
                new LstItem().addLf(new Lf("lf1111", "56")));
        checkLst11x(lst11_2.getLsts().get("lst112"), new LstItem().addLf(new Lf("lf1121", "lf1121 str22")),
                new LstItem().addLf(new Lf("lf1121", "lf1121 str21")));
    }

    private void checkLst11_1(LstItem lst11_1) {
        assertNotNull(lst11_1);

        assertEquals(2, lst11_1.getLfs().size());
        assertEquals(1, lst11_1.getConts().size());
        assertEquals(2, lst11_1.getLsts().size());

        assertEquals(lst11_1.getLfs().get("lf112"), new Lf("lf112", "lf112 str"));

        Cont lst11_1_cont = lst11_1.getConts().get("cont111");
        assertEquals(0, lst11_1_cont.getConts().size());
        assertEquals(1, lst11_1_cont.getLfLsts().size());
        assertEquals(1, lst11_1_cont.getLfs().size());
        assertEquals(1, lst11_1_cont.getLsts().size());

        // cont111 check
        assertEquals(new Lf("lf1111", "lf1111 str"), lst11_1_cont.getLfs().get("lf1111"));
        assertEquals(new LfLst("lflst1111").addLf(new Lf("2048")).addLf(new Lf("1024")).addLf(new Lf("4096")),
                lst11_1_cont.getLfLsts().get("lflst1111"));

        assertNotNull(lst11_1_cont.getLsts().get("lst1111"));
        checkLst1111(lst11_1_cont.getLsts().get("lst1111").getLstItems(), new Lf("lf1111A", "lf1111A str11"), new Lf(
                "lf1111B", "4"), new Lf("lf1111A", "lf1111A str12"), new Lf("lf1111B", "7"));

        checkLst11x(lst11_1.getLsts().get("lst111"), new LstItem().addLf(new Lf("lf1111", "65")));
        checkLst11x(lst11_1.getLsts().get("lst112"), new LstItem().addLf(new Lf("lf1121", "lf1121 str11")));
    }

    private void checkLst11x(Lst lst, LstItem... lstItems) {
        assertNotNull(lst);

        Lst requiredLst = new Lst(lst.getName());
        for (LstItem lstItem : lstItems) {
            requiredLst.addLstItem(lstItem);
        }

        assertEquals(requiredLst, lst);

    }

    private void checkLst1111(Set<LstItem> lstItems, Lf lf11, Lf lf12, Lf lf21, Lf lf22) {
        LstItem lst11_1_cont_lst1111_1 = null;
        LstItem lst11_1_cont_lst1111_2 = null;
        for (LstItem lstItem : lstItems) {
            if (new LstItem().addLf(lf11).addLf(lf12).equals(lstItem)) {
                lst11_1_cont_lst1111_1 = lstItem;
            } else if (new LstItem().addLf(lf21).addLf(lf22).equals(lstItem)) {
                lst11_1_cont_lst1111_2 = lstItem;
            }
        }

        assertNotNull(lst11_1_cont_lst1111_1);
        assertNotNull(lst11_1_cont_lst1111_2);
    }

    private CompositeNode prepareCompositeNodeWithEmpties() {
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont1", "simple:yang:types", "2013-11-5"), null, null, ModifyAction.CREATE, null);

        // lst11_1
        MutableCompositeNode lst11_1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst11"), cont1,
                null, ModifyAction.CREATE, null);
        cont1.getChildren().add(lst11_1);

        MutableSimpleNode<?> lf111_1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf111"), lst11_1, "1",
                ModifyAction.CREATE, null);
        lst11_1.getChildren().add(lf111_1);

        // lst111_1_1
        MutableCompositeNode lst111_1_1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst111"),
                lst11_1, null, ModifyAction.CREATE, null);
        lst11_1.getChildren().add(lst111_1_1);
        MutableSimpleNode<?> lf1111_1_1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1111"),
                lst111_1_1, "34", ModifyAction.CREATE, null);
        lst111_1_1.getChildren().add(lf1111_1_1);
        lst111_1_1.init();
        // :lst111_1_1

        // lst111_1_2
        MutableCompositeNode lst111_1_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst111"),
                lst11_1, null, ModifyAction.CREATE, null);
        lst11_1.getChildren().add(lst111_1_2);
        MutableSimpleNode<?> lf1111_1_2 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1111"),
                lst111_1_2, "35", ModifyAction.CREATE, null);
        lst111_1_2.getChildren().add(lf1111_1_2);
        lst111_1_2.init();
        // :lst111_1_2

        // lst111_1_3
        MutableCompositeNode lst111_1_3 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst111"),
                lst11_1, null, ModifyAction.CREATE, null);
        lst11_1.getChildren().add(lst111_1_3);
        lst111_1_2.init();
        // :lst111_1_3

        // lst111_1_4
        MutableCompositeNode lst111_1_4 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst111"),
                lst11_1, null, ModifyAction.CREATE, null);
        lst11_1.getChildren().add(lst111_1_4);
        lst111_1_2.init();
        // :lst111_1_4

        MutableCompositeNode cont111_1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont111"),
                lst11_1, null, ModifyAction.CREATE, null);
        lst11_1.getChildren().add(cont111_1);

        lst11_1.init();
        // :lst11_1

        // lst11_2
        MutableCompositeNode lst11_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst11"), cont1,
                null, ModifyAction.CREATE, null);
        cont1.getChildren().add(lst11_2);

        MutableSimpleNode<?> lf111_2 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf111"), lst11_2, "2",
                ModifyAction.CREATE, null);
        lst11_2.getChildren().add(lf111_2);

        // cont111_2
        MutableCompositeNode cont111_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont111"),
                lst11_2, null, ModifyAction.CREATE, null);
        lst11_2.getChildren().add(cont111_2);

        MutableSimpleNode<?> lflst1111_2_2 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lflst1111"),
                cont111_2, "1024", ModifyAction.CREATE, null);
        cont111_2.getChildren().add(lflst1111_2_2);
        MutableSimpleNode<?> lflst1111_2_3 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lflst1111"),
                cont111_2, "4096", ModifyAction.CREATE, null);
        cont111_2.getChildren().add(lflst1111_2_3);

        // lst1111_2
        MutableCompositeNode lst1111_2_1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst1111"),
                cont111_2, null, ModifyAction.CREATE, null);
        cont111_2.getChildren().add(lst1111_2_1);
        MutableSimpleNode<?> lf1111B_2_1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1111B"),
                lst1111_2_1, "4", ModifyAction.CREATE, null);
        lst1111_2_1.getChildren().add(lf1111B_2_1);
        lst1111_2_1.init();

        MutableCompositeNode lst1111_2_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst1111"),
                cont111_2, null, ModifyAction.CREATE, null);
        cont111_2.getChildren().add(lst1111_2_2);
        MutableSimpleNode<?> lf1111B_2_2 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1111A"),
                lst1111_2_2, "lf1111A str12", ModifyAction.CREATE, null);
        lst1111_2_2.getChildren().add(lf1111B_2_2);
        lst1111_2_2.init();
        // :lst1111_2

        cont111_2.init();
        // :cont111_2

        MutableCompositeNode lst112_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst112"), lst11_2,
                null, ModifyAction.CREATE, null);
        lst11_2.getChildren().add(lst112_2);
        lst112_2.init();
        lst11_2.init();

        // :lst11_2

        // lst11_3
        MutableCompositeNode lst11_3 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst11"), cont1,
                null, ModifyAction.CREATE, null);
        cont1.getChildren().add(lst11_3);

        MutableSimpleNode<?> lf111_3 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf111"), lst11_3, "3",
                ModifyAction.CREATE, null);
        lst11_3.getChildren().add(lf111_3);

        // cont111_3
        MutableCompositeNode cont111_3 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont111"),
                lst11_3, null, ModifyAction.CREATE, null);
        lst11_3.getChildren().add(cont111_3);

        MutableCompositeNode lst1111_3_1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst1111"),
                cont111_3, null, ModifyAction.CREATE, null);
        cont111_3.getChildren().add(lst1111_3_1);
        lst1111_3_1.init();

        MutableCompositeNode lst1111_3_2 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("lst1111"),
                cont111_3, null, ModifyAction.CREATE, null);
        cont111_3.getChildren().add(lst1111_3_2);
        lst1111_3_2.init();

        cont111_3.init();
        // :cont111_3

        lst11_3.init();
        // :lst11_3

        cont1.init();
        return cont1;
    }

}
