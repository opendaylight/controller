package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Cont;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Lf;
import org.opendaylight.controller.sal.restconf.impl.test.structures.LfLst;
import org.opendaylight.controller.sal.restconf.impl.test.structures.Lst;
import org.opendaylight.controller.sal.restconf.impl.test.structures.LstItem;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Iterables;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class NnAndYangWithJsonReaderEmptyDataTest extends
        AbstractBodyReaderTest {

    private String TEST_MODULE_NS_STRING = "simple:yang:types";
    private String TEST_MODULE_REVISION = "2013-11-5";

    final QName cont1 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "cont1");
    final QName lf11 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lf11");
    final QName lst11 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lst11");
    final QName keyLf111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lf111");
    final QName cont111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "cont111");
    private QName ls111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lst111");
    private QName keyLf1111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lf1111");
    private QName lflst1111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lflst1111");
    private QName lst1111 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lst1111");
    private QName lf1111A = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lf1111A");
    private QName lf1111B = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lf1111B");
    private QName ls112 = QName.create(TEST_MODULE_NS_STRING,
            TEST_MODULE_REVISION, "lst112");

    private static SchemaContext schemaContext;
    private NormalizedNodeJsonBodyWriter normalizedNodeJsonBodyWriter;

    public NnAndYangWithJsonReaderEmptyDataTest() throws NoSuchFieldException,
            SecurityException {
        super();
        normalizedNodeJsonBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialize() throws FileNotFoundException {
        schemaContext = schemaContextLoader("/nn-to-json/simple-yang-types",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void nnAndYangWithJsonReaderEmptyDataTest()
            throws WebApplicationException, IOException {

        DataSchemaNode cont1SchemaNode = schemaContext
                .getDataChildByName(cont1);

        assertTrue(cont1SchemaNode instanceof ContainerSchemaNode);
        ContainerSchemaNode con1ContSchemaNode = (ContainerSchemaNode) cont1SchemaNode;

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont1ContBuilder = Builders
                .containerBuilder(con1ContSchemaNode);

        DataSchemaNode lst11SchemaNode = con1ContSchemaNode
                .getDataChildByName(lst11);
        assertTrue(lst11SchemaNode instanceof ListSchemaNode);

        CollectionNodeBuilder<MapEntryNode, MapNode> lst11DataNodeBuilder = Builders
                .mapBuilder((ListSchemaNode) lst11SchemaNode);

        fillLst11(lst11DataNodeBuilder, lst11SchemaNode, cont1ContBuilder);

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null,
                        cont1SchemaNode, null, schemaContext),
                cont1ContBuilder.build());

        OutputStream jsonOutput = new ByteArrayOutputStream();

        normalizedNodeJsonBodyWriter.writeTo(testNormalizedNodeContext, null,
                null, null, mediaType, null, jsonOutput);

        verifyJsonOutputForEmptyData(jsonOutput.toString());
    }

    private void fillLst11(
            CollectionNodeBuilder<MapEntryNode, MapNode> lst11DataNodeBuilder,
            DataSchemaNode lst11SchemaNode,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont1ContBuilder) {
        for (int i = 0; i < 3; i++) {
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls11NodeValues = Builders
                    .mapEntryBuilder((ListSchemaNode) lst11SchemaNode);

            List<DataSchemaNode> instanceLf111 = ControllerContext
                    .findInstanceDataChildrenByName(
                            (DataNodeContainer) lst11SchemaNode,
                            keyLf111.getLocalName());
            DataSchemaNode schemaLf111 = Iterables
                    .getFirst(instanceLf111, null);
            ls11NodeValues.withChild(Builders
                    .leafBuilder((LeafSchemaNode) schemaLf111).withValue(i + 1)
                    .build());
            addCont111(ls11NodeValues, lst11SchemaNode, i);

            if (i == 0) {
                addLs111(ls11NodeValues, lst11SchemaNode);
            }
            if (i == 1) {
                // addLs111(ls11NodeValues, lst11SchemaNode);
                addLs112(ls11NodeValues, lst11SchemaNode);
            }

            lst11DataNodeBuilder.withChild(ls11NodeValues.build());
        }
        cont1ContBuilder.withChild(lst11DataNodeBuilder.build());
    }

    private void addLs112(
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls11NodeValues,
            DataSchemaNode lst11SchemaNode) {

        List<DataSchemaNode> instanceLst112 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) lst11SchemaNode,
                        ls112.getLocalName());

        DataSchemaNode lst112SchemaNode = Iterables.getFirst(instanceLst112,
                null);

        CollectionNodeBuilder<MapEntryNode, MapNode> lst112DataNodeBuilder = Builders
                .mapBuilder((ListSchemaNode) lst112SchemaNode);

        DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls112NodeValues = Builders
                .mapEntryBuilder((ListSchemaNode) lst112SchemaNode);

        lst112DataNodeBuilder.withChild(ls112NodeValues.build());

        ls11NodeValues.withChild(lst112DataNodeBuilder.build());
    }

    private void addLs111(
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls11NodeValues,
            DataSchemaNode lst11SchemaNode) {

        List<DataSchemaNode> instanceLst111 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) lst11SchemaNode,
                        ls111.getLocalName());

        DataSchemaNode lst111SchemaNode = Iterables.getFirst(instanceLst111,
                null);

        CollectionNodeBuilder<MapEntryNode, MapNode> lst111DataNodeBuilder = Builders
                .mapBuilder((ListSchemaNode) lst111SchemaNode);

        for (int i = 0; i < 4; i++) {
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls111NodeValues = Builders
                    .mapEntryBuilder((ListSchemaNode) lst111SchemaNode);

            List<DataSchemaNode> instanceLf1111 = ControllerContext
                    .findInstanceDataChildrenByName(
                            (DataNodeContainer) lst111SchemaNode,
                            keyLf1111.getLocalName());
            DataSchemaNode schemaLf1111 = Iterables.getFirst(instanceLf1111,
                    null);

            if (i == 0) {
                ls111NodeValues.withChild(Builders
                        .leafBuilder((LeafSchemaNode) schemaLf1111)
                        .withValue(35).build());
            } else if (i == 2) {
                ls111NodeValues.withChild(Builders
                        .leafBuilder((LeafSchemaNode) schemaLf1111)
                        .withValue(34).build());
            } else {
            }
            lst111DataNodeBuilder.withChild(ls111NodeValues.build());
        }
        ls11NodeValues.withChild(lst111DataNodeBuilder.build());
    }

    private void addCont111(
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls11NodeValues,
            DataSchemaNode lst11SchemaNode, int i) {
        List<DataSchemaNode> instanceCont111 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) lst11SchemaNode,
                        cont111.getLocalName());

        ContainerSchemaNode schemaCont111 = (ContainerSchemaNode) Iterables
                .getFirst(instanceCont111, null);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont111ContBuilder = Builders
                .containerBuilder(schemaCont111);
        if (i == 1) {
            addLflst1111(schemaCont111, cont111ContBuilder);
            addLst1111(schemaCont111, cont111ContBuilder, true);
        }

        if (i == 2) {
            addLst1111(schemaCont111, cont111ContBuilder, false);
        }

        ls11NodeValues.withChild(cont111ContBuilder.build());
    }

    private void addLst1111(
            ContainerSchemaNode schemaCont111,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont111ContBuilder,
            boolean addItems) {
        List<DataSchemaNode> instanceLst1111 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) schemaCont111,
                        lst1111.getLocalName());
        DataSchemaNode schemaLst1111 = Iterables
                .getFirst(instanceLst1111, null);

        CollectionNodeBuilder<MapEntryNode, MapNode> lst1111DataNodeBuilder = Builders
                .mapBuilder((ListSchemaNode) schemaLst1111);

        for (int i = 0; i < 3; i++) {

            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> ls1111NodeValues = Builders
                    .mapEntryBuilder((ListSchemaNode) schemaLst1111);

            List<DataSchemaNode> instanceLf1111A = ControllerContext
                    .findInstanceDataChildrenByName(
                            (DataNodeContainer) schemaLst1111,
                            lf1111A.getLocalName());
            DataSchemaNode schemaLf1111A = Iterables.getFirst(instanceLf1111A,
                    null);

            LeafNode<Object> buildedLf1111A = Builders
                    .leafBuilder((LeafSchemaNode) schemaLf1111A)
                    .withValue("lf1111A str12").build();

            if (i == 0 && addItems) {
                ls1111NodeValues.withChild(buildedLf1111A);
            } else if (addItems) {
                List<DataSchemaNode> instanceLf1111B = ControllerContext
                        .findInstanceDataChildrenByName(
                                (DataNodeContainer) schemaLst1111,
                                lf1111B.getLocalName());
                DataSchemaNode schemaLf1111B = Iterables.getFirst(
                        instanceLf1111B, null);

                byte transfVaule = Byte.parseByte("4");
                ls1111NodeValues.withChild(Builders
                        .leafBuilder((LeafSchemaNode) schemaLf1111B)
                        .withValue(transfVaule).build());
            }
            lst1111DataNodeBuilder.withChild(ls1111NodeValues.build());
        }
        cont111ContBuilder.withChild(lst1111DataNodeBuilder.build());

    }

    private void addLflst1111(
            DataSchemaNode schemaCont111,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont111ContBuilder) {
        List<DataSchemaNode> instanceCont111 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) schemaCont111,
                        lflst1111.getLocalName());

        DataSchemaNode schemaLfLst1111 = Iterables.getFirst(instanceCont111,
                null);

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> lfLst1111Builder = Builders
                .leafSetBuilder((LeafListSchemaNode) schemaLfLst1111);

        lfLst1111Builder.withChild(Builders
                .leafSetEntryBuilder(((LeafListSchemaNode) schemaLfLst1111))
                .withValue(1024).build());

        lfLst1111Builder.withChild(Builders
                .leafSetEntryBuilder(((LeafListSchemaNode) schemaLfLst1111))
                .withValue(4096).build());

        cont111ContBuilder.withChild(lfLst1111Builder.build());
    }

    private void verifyJsonOutputForEmptyData(final String jsonOutput) {
        assertNotNull(jsonOutput);
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

    private Cont jsonReadCont1(final JsonReader jReader) throws IOException {
        jReader.beginObject();
        assertNotNull("cont1 is missing.", jReader.hasNext());

        Cont dataFromJson = new Cont(jReader.nextName());
        dataFromJson = jsonReadCont1Elements(jReader, dataFromJson);

        assertFalse("cont shouldn't have other element.", jReader.hasNext());
        jReader.endObject();
        return dataFromJson;

    }

    private Cont jsonReadCont1Elements(final JsonReader jReader,
            final Cont redData) throws IOException {
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
                assertTrue("Key " + keyName + " doesn't exists in yang file.",
                        false);
            }
        }
        jReader.endObject();
        return redData;
    }

    private Lst jsonReadLst11(final JsonReader jReader, final Lst lst)
            throws IOException {
        jReader.beginArray();

        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst11Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst11Elements(final JsonReader jReader)
            throws IOException {
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
                assertTrue("Key " + keyName + " doesn't exists in yang file.",
                        false);
            }
        }
        jReader.endObject();
        return lstItem;
    }

    private Lst jsonReadLst111(final JsonReader jReader, final Lst lst)
            throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst111Elements(final JsonReader jReader)
            throws IOException {
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

    private Lst jsonReadLst112(final JsonReader jReader, final Lst lst)
            throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst112Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst112Elements(final JsonReader jReader)
            throws IOException {
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

    private Cont jsonReadCont111(final JsonReader jReader, Cont cont)
            throws IOException {
        jReader.beginObject();
        cont = jsonReadCont111Elements(jReader, cont);
        jReader.endObject();
        return cont;
    }

    private Cont jsonReadCont111Elements(final JsonReader jReader,
            final Cont cont) throws IOException {
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
                assertTrue("Key " + keyName + " doesn't exists in yang file.",
                        false);
            }
        }
        return cont;

    }

    private Lst jsonReadLst1111(final JsonReader jReader, final Lst lst)
            throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            LstItem lstItem = jsonReadLst1111Elements(jReader);
            lst.addLstItem(lstItem);
        }
        jReader.endArray();
        return lst;
    }

    private LstItem jsonReadLst1111Elements(final JsonReader jReader)
            throws IOException {
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

    private LfLst jsonReadLflstValues(final JsonReader jReader,
            final LfLst lfLst) throws IOException {
        jReader.beginArray();
        while (jReader.hasNext()) {
            lfLst.addLf(new Lf(nextValue(jReader)));
        }
        jReader.endArray();
        return lfLst;
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

    private void checkDataFromJsonEmpty(final Cont dataFromJson) {
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
        assertEquals(lst11_1.getLsts().get("lst111"),
                new Lst("lst111").addLstItem(new LstItem().addLf("lf1111", 35))
                        .addLstItem(new LstItem().addLf("lf1111", 34))
                        .addLstItem(new LstItem()));
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

        assertEquals(new LfLst("lflst1111").addLf(1024).addLf(4096),
                lst11_2_cont111.getLfLsts().get("lflst1111"));
        assertEquals(
                new Lst("lst1111")
                        .addLstItem(new LstItem().addLf("lf1111B", 4))
                        .addLstItem(
                                new LstItem().addLf("lf1111A", "lf1111A str12")),
                lst11_2_cont111.getLsts().get("lst1111"));
        // :-cont111
        assertEquals(lst11_2.getLsts().get("lst112"),
                new Lst("lst112").addLstItem(new LstItem()));
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

        assertEquals(new Lst("lst1111").addLstItem(new LstItem()),
                lst11_3_cont111.getLsts().get("lst1111"));
        // :-cont111
        // : lst11_3

    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }
}
