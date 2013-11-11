package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.*;
import java.util.*;

import javax.validation.constraints.AssertFalse;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.structures.*;

import com.google.gson.stream.JsonReader;

public class YangAndXmlToJsonBasicYangTypesTest {

    @Test
    public void simpleYangTypesWithJsonReaderTest() {
        String jsonOutput;
        // jsonOutput =
        // TestUtils.readJsonFromFile("/yang-to-json-conversion/simple-yang-types/xml/awaited_output.json",
        // false);

        jsonOutput = TestUtils.convertXmlDataAndYangToJson("/yang-to-json-conversion/simple-yang-types/xml/data.xml",
                "/yang-to-json-conversion/simple-yang-types", "/yang-to-json-conversion/simple-yang-types/xml");

        verifyJsonOutput(jsonOutput);

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
                redData.addLf(new Lf(keyName, jReader.nextString()));
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
                lstItem.addLf(new Lf(keyName, jReader.nextString()));
            } else if (keyName.equals("lf112")) {
                lstItem.addLf(new Lf(keyName, jReader.nextString()));
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
                lstItem.addLf(new Lf(keyName, jReader.nextString()));
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
                lstItem.addLf(new Lf(keyName, jReader.nextString()));
            }
        }
        jReader.endObject();
        return lstItem;
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
                cont.addLf(new Lf(keyName, jReader.nextString()));
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
                lstItem.addLf(new Lf(keyName, jReader.nextString()));
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private LfLst jsonReadLflstValues(JsonReader jReader, LfLst lfLst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            lfLst.addLf(new Lf(jReader.nextString()));
        }
        jReader.endArray();
        return lfLst;
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

}
