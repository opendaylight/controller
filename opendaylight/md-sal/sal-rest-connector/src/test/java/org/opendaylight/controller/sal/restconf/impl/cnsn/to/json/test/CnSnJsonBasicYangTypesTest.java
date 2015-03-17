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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Cont;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Lf;
import org.opendaylight.controller.sal.restconf.impl.test.structures.LfLst;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Lst;
import org.opendaylight.controller.sal.restconf.impl.test.structures.LstItem;

public class CnSnJsonBasicYangTypesTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/simple-yang-types", 1, "simple-yang-types", "cont1");
    }

    private void verifyJsonOutputForEmptyData(final String jsonOutput) {
        assertNotNull(jsonOutput);
        final StringReader strReader = new StringReader(jsonOutput);
        final JsonReader jReader = new JsonReader(strReader);

        String exception = null;
        Cont dataFromJson = null;
        try {
            dataFromJson = jsonReadCont1(jReader);
        } catch (final IOException e) {
            exception = e.getMessage();
        }

        assertNotNull("Data structures from json are missing.", dataFromJson);
        checkDataFromJsonEmpty(dataFromJson);

        assertNull("Error during reading Json output: " + exception, exception);
    }

    private void verifyJsonOutput(final String jsonOutput) {
        assertNotNull(jsonOutput);
        final StringReader strReader = new StringReader(jsonOutput);
        final JsonReader jReader = new JsonReader(strReader);

        String exception = null;
        Cont dataFromJson = null;
        try {
            dataFromJson = jsonReadCont1(jReader);
        } catch (final IOException e) {
            exception = e.getMessage();
        }

        assertNotNull("Data structures from json are missing.", dataFromJson);
        checkDataFromJson(dataFromJson);

        assertNull("Error during reading Json output: " + exception, exception);
    }

    private Cont jsonReadCont1(final JsonReader jReader) throws IOException {
        jReader.beginObject();
        assertNotNull("cont1 is missing.", jReader.hasNext());

        Cont dataFromJson = new Cont(jReader.nextName());
        dataFromJson = jsonReadCont1Elements(jReader, dataFromJson);

        assertFalse("cont shouldn't have other element.", jReader.hasNext());
        jReader.endObject();
        return dataFromJson;

    }

    private Cont jsonReadCont1Elements(final JsonReader jReader, final Cont redData) throws IOException {
        jReader.beginObject();
        while (jReader.hasNext()) {
            final String keyName = jReader.nextName();
            if (keyName.equals("lf11")) {
                redData.addLf(new Lf(keyName, nextValue(jReader)));
            } else if (keyName.equals("lflst11")) {
                LfLst lfLst = new LfLst(keyName);
                lfLst = jsonReadLflstValues(jReader, lfLst);
                redData.addLfLst(lfLst);
            } else if (keyName.equals("lflst12")) {
                final LfLst lfLst = new LfLst(keyName);
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

    private Lst jsonReadLst11(final JsonReader jReader, final Lst lst) throws IOException {
        jReader.beginArray();

        while (jReader.hasNext()) {
            final LstItem lstItem = jsonReadLst11Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst11Elements(final JsonReader jReader) throws IOException {
        final LstItem lstItem = new LstItem();
        jReader.beginObject();
        while (jReader.hasNext()) {
            final String keyName = jReader.nextName();
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

    private Lst jsonReadLst112(final JsonReader jReader, final Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            final LstItem lstItem = jsonReadLst112Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst112Elements(final JsonReader jReader) throws IOException {
        final LstItem lstItem = new LstItem();
        jReader.beginObject();
        if (jReader.hasNext()) {
            final String keyName = jReader.nextName();
            if (keyName.equals("lf1121")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;

    }

    private Lst jsonReadLst111(final JsonReader jReader, final Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            final LstItem lstItem = jsonReadLst111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst111Elements(final JsonReader jReader) throws IOException {
        final LstItem lstItem = new LstItem();
        jReader.beginObject();
        if (jReader.hasNext()) {
            final String keyName = jReader.nextName();
            if (keyName.equals("lf1111")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private Object nextValue(final JsonReader jReader) throws IOException {
        if (jReader.peek().equals(JsonToken.NULL)) {
            jReader.nextNull();
            return null;
        } else if (jReader.peek().equals(JsonToken.NUMBER)) {
            return jReader.nextInt();
        } else {
            return jReader.nextString();
        }
    }

    private Cont jsonReadCont111(final JsonReader jReader, Cont cont) throws IOException {
        jReader.beginObject();
        cont = jsonReadCont111Elements(jReader, cont);
        jReader.endObject();
        return cont;
    }

    private Cont jsonReadCont111Elements(final JsonReader jReader, final Cont cont) throws IOException {
        while (jReader.hasNext()) {
            final String keyName = jReader.nextName();
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

    private Lst jsonReadLst1111(final JsonReader jReader, final Lst lst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            final LstItem lstItem = jsonReadLst1111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst1111Elements(final JsonReader jReader) throws IOException {
        jReader.beginObject();
        final LstItem lstItem = new LstItem();
        while (jReader.hasNext()) {
            final String keyName = jReader.nextName();
            if (keyName.equals("lf1111A") || keyName.equals("lf1111B")) {
                lstItem.addLf(new Lf(keyName, nextValue(jReader)));
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private LfLst jsonReadLflstValues(final JsonReader jReader, final LfLst lfLst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            lfLst.addLf(new Lf(nextValue(jReader)));
        }
        jReader.endArray();
        return lfLst;
    }

    private void checkDataFromJsonEmpty(final Cont dataFromJson) {
        assertTrue(dataFromJson.getLfs().isEmpty());
        assertTrue(dataFromJson.getLfLsts().isEmpty());
        assertTrue(dataFromJson.getConts().isEmpty());

        final Map<String, Lst> lsts = dataFromJson.getLsts();
        assertEquals(1, lsts.size());
        final Lst lst11 = lsts.get("lst11");
        assertNotNull(lst11);
        final Set<LstItem> lstItems = lst11.getLstItems();
        assertNotNull(lstItems);

        LstItem lst11_1 = null;
        LstItem lst11_2 = null;
        LstItem lst11_3 = null;
        for (final LstItem lstItem : lstItems) {
            if (lstItem.getLfs().get("lf111").getValue().equals(1)) {
                lst11_1 = lstItem;
            } else if (lstItem.getLfs().get("lf111").getValue().equals(2)) {
                lst11_2 = lstItem;
            } else if (lstItem.getLfs().get("lf111").getValue().equals(3)) {
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
        assertEquals(lst11_1.getLsts().get("lst111"), new Lst("lst111").addLstItem(new LstItem().addLf("lf1111", 35))
                .addLstItem(new LstItem().addLf("lf1111", 34)).addLstItem(new LstItem()).addLstItem(new LstItem()));
        assertEquals(lst11_1.getConts().get("cont111"), new Cont("cont111"));
        // : lst11_1

        // lst11_2
        assertTrue(lst11_2.getLfLsts().isEmpty());
        assertEquals(1, lst11_2.getLfs().size());
        assertEquals(1, lst11_2.getConts().size());
        assertEquals(1, lst11_2.getLsts().size());

        final Cont lst11_2_cont111 = lst11_2.getConts().get("cont111");

        // -cont111
        assertNotNull(lst11_2_cont111);
        assertTrue(lst11_2_cont111.getLfs().isEmpty());
        assertEquals(1, lst11_2_cont111.getLfLsts().size());
        assertEquals(1, lst11_2_cont111.getLsts().size());
        assertTrue(lst11_2_cont111.getConts().isEmpty());

        assertEquals(new LfLst("lflst1111").addLf(1024).addLf(4096), lst11_2_cont111.getLfLsts().get("lflst1111"));
        assertEquals(
                new Lst("lst1111").addLstItem(new LstItem().addLf("lf1111B", 4)).addLstItem(
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
        final Cont lst11_3_cont111 = lst11_3.getConts().get("cont111");
        assertEquals(0, lst11_3_cont111.getLfs().size());
        assertEquals(0, lst11_3_cont111.getLfLsts().size());
        assertEquals(1, lst11_3_cont111.getLsts().size());
        assertTrue(lst11_3_cont111.getConts().isEmpty());

        assertEquals(new Lst("lst1111").addLstItem(new LstItem()).addLstItem(new LstItem()), lst11_3_cont111.getLsts()
                .get("lst1111"));
        // :-cont111
        // : lst11_3

    }

    private void checkDataFromJson(final Cont dataFromJson) {
        assertNotNull(dataFromJson.getLfs().get("lf11"));
        assertEquals(dataFromJson.getLfs().get("lf11"), new Lf("lf11", "lf"));

        LfLst lflst11 = null;
        LfLst lflst12 = null;

        lflst11 = dataFromJson.getLfLsts().get("lflst11");
        lflst12 = dataFromJson.getLfLsts().get("lflst12");

        assertNotNull(lflst11);
        assertNotNull(lflst12);

        assertEquals(3, lflst11.getLfs().size());
        assertTrue(lflst11.getLfs().contains(new Lf(55)));
        assertTrue(lflst11.getLfs().contains(new Lf(56)));
        assertTrue(lflst11.getLfs().contains(new Lf(57)));

        assertEquals(3, lflst12.getLfs().size());
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str1")));
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str2")));
        assertTrue(lflst12.getLfs().contains(new Lf("lflst12 str3")));

        assertEquals(1, dataFromJson.getLsts().size());
        final Lst lst11 = dataFromJson.getLsts().get("lst11");
        assertNotNull(lst11);
        assertEquals(2, lst11.getLstItems().size());

        LstItem lst11_1 = null;
        LstItem lst11_2 = null;
        for (final LstItem lstItem : lst11.getLstItems()) {
            final Lf lf = lstItem.getLfs().get("lf111");
            if (lf != null && lf.getValue().equals(140)) {
                lst11_1 = lstItem;
            } else if (lf != null && lf.getValue().equals(141)) {
                lst11_2 = lstItem;
            }
        }

        checkLst11_1(lst11_1);
        checkLst11_2(lst11_2);
    }

    private void checkLst11_2(final LstItem lst11_2) {
        assertNotNull(lst11_2);
        assertEquals(2, lst11_2.getLfs().size());
        assertEquals(1, lst11_2.getConts().size());
        assertEquals(2, lst11_2.getLsts().size());

        assertEquals(lst11_2.getLfs().get("lf112"), new Lf("lf112", "lf112 str2"));

        final Cont lst11_2_cont = lst11_2.getConts().get("cont111");
        assertEquals(0, lst11_2_cont.getConts().size());
        assertEquals(1, lst11_2_cont.getLfLsts().size());
        assertEquals(1, lst11_2_cont.getLfs().size());
        assertEquals(1, lst11_2_cont.getLsts().size());

        // cont111 check
        assertEquals(new Lf("lf1111", "lf1111 str2"), lst11_2_cont.getLfs().get("lf1111"));
        assertEquals(new LfLst("lflst1111").addLf(new Lf(2049)).addLf(new Lf(1025)).addLf(new Lf(4097)), lst11_2_cont
                .getLfLsts().get("lflst1111"));

        assertNotNull(lst11_2_cont.getLsts().get("lst1111"));
        checkLst1111(lst11_2_cont.getLsts().get("lst1111").getLstItems(), new Lf("lf1111A", "lf1111A str21"), new Lf(
                "lf1111B", 5), new Lf("lf1111A", "lf1111A str22"), new Lf("lf1111B", 8));

        checkLst11x(lst11_2.getLsts().get("lst111"), new LstItem().addLf(new Lf("lf1111", 55)),
                new LstItem().addLf(new Lf("lf1111", 56)));
        checkLst11x(lst11_2.getLsts().get("lst112"), new LstItem().addLf(new Lf("lf1121", "lf1121 str22")),
                new LstItem().addLf(new Lf("lf1121", "lf1121 str21")));
    }

    private void checkLst11_1(final LstItem lst11_1) {
        assertNotNull(lst11_1);

        assertEquals(2, lst11_1.getLfs().size());
        assertEquals(1, lst11_1.getConts().size());
        assertEquals(2, lst11_1.getLsts().size());

        assertEquals(lst11_1.getLfs().get("lf112"), new Lf("lf112", "lf112 str"));

        final Cont lst11_1_cont = lst11_1.getConts().get("cont111");
        assertEquals(0, lst11_1_cont.getConts().size());
        assertEquals(1, lst11_1_cont.getLfLsts().size());
        assertEquals(1, lst11_1_cont.getLfs().size());
        assertEquals(1, lst11_1_cont.getLsts().size());

        // cont111 check
        assertEquals(new Lf("lf1111", "lf1111 str"), lst11_1_cont.getLfs().get("lf1111"));
        assertEquals(new LfLst("lflst1111").addLf(new Lf(2048)).addLf(new Lf(1024)).addLf(new Lf(4096)), lst11_1_cont
                .getLfLsts().get("lflst1111"));

        assertNotNull(lst11_1_cont.getLsts().get("lst1111"));
        checkLst1111(lst11_1_cont.getLsts().get("lst1111").getLstItems(), new Lf("lf1111A", "lf1111A str11"), new Lf(
                "lf1111B", 4), new Lf("lf1111A", "lf1111A str12"), new Lf("lf1111B", 7));

        checkLst11x(lst11_1.getLsts().get("lst111"), new LstItem().addLf(new Lf("lf1111", 65)));
        checkLst11x(lst11_1.getLsts().get("lst112"), new LstItem().addLf(new Lf("lf1121", "lf1121 str11")));
    }

    private void checkLst11x(final Lst lst, final LstItem... lstItems) {
        assertNotNull(lst);

        final Lst requiredLst = new Lst(lst.getName());
        for (final LstItem lstItem : lstItems) {
            requiredLst.addLstItem(lstItem);
        }

        assertEquals(requiredLst, lst);

    }

    private void checkLst1111(final Set<LstItem> lstItems, final Lf lf11, final Lf lf12, final Lf lf21, final Lf lf22) {
        LstItem lst11_1_cont_lst1111_1 = null;
        LstItem lst11_1_cont_lst1111_2 = null;
        for (final LstItem lstItem : lstItems) {
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
