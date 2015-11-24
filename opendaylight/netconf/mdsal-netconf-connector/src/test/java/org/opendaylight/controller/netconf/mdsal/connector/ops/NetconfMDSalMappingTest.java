/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.ConcurrentDOMDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.controller.netconf.mdsal.connector.ops.get.Get;
import org.opendaylight.controller.netconf.mdsal.connector.ops.get.GetConfig;
import org.opendaylight.controller.netconf.util.test.NetconfXmlUnitRecursiveQualifier;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.builder.impl.BuilderUtils;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NetconfMDSalMappingTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMDSalMappingTest.class);

    private static final String RPC_REPLY_ELEMENT = "rpc-reply";
    private static final String DATA_ELEMENT = "data";
    private static final String FILTER_NODE = "filter";
    private static final String GET_CONFIG = "get-config";
    private static final QName TOP = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "top");
    private static final QName USERS = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "users");
    private static final QName USER = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "user");
    private static final QName MODULES = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "modules");
    private static final QName AUGMENTED_CONTAINER = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "augmented-container");
    private static final QName AUGMENTED_STRING_IN_CONT = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "identifier");
    private static final QName CHOICE_NODE = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "choice-node");
    private static final QName AUGMENTED_CASE = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "augmented-case");
    private static final QName CHOICE_WRAPPER = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "choice-wrapper");
    private static final QName INNER_CHOICE = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "inner-choice");
    private static final QName INNER_CHOICE_TEXT = QName.create("urn:opendaylight:mdsal:mapping:test", "2015-02-26", "text");

    private static final YangInstanceIdentifier AUGMENTED_CONTAINER_IN_MODULES =
            YangInstanceIdentifier.builder().node(TOP).node(MODULES).build().node(new AugmentationIdentifier(Collections.singleton(AUGMENTED_CONTAINER)));

    private static Document RPC_REPLY_OK = null;

    static {
        try {
            RPC_REPLY_OK = XmlFileLoader.xmlFileToDocument("messages/mapping/rpc-reply_ok.xml");
        } catch (Exception e) {
            LOG.debug("unable to load rpc reply ok.", e);
            RPC_REPLY_OK = XmlUtil.newDocument();
        }
    }

    private CurrentSchemaContext currentSchemaContext = null;
    private SchemaContext schemaContext = null;
    private String sessionIdForReporting = "netconf-test-session1";

    private TransactionProvider transactionProvider = null;

    @Before
    public void setUp() throws Exception {

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        this.schemaContext = parseSchemas(getYangSchemas());
        schemaContext.getModules();
        final SchemaService schemaService = createSchemaService();

        final DOMStore operStore = InMemoryDOMDataStoreFactory.create("DOM-OPER", schemaService);
        final DOMStore configStore = InMemoryDOMDataStoreFactory.create("DOM-CFG", schemaService);

        final EnumMap<LogicalDatastoreType, DOMStore> datastores = new EnumMap<>(LogicalDatastoreType.class);
        datastores.put(LogicalDatastoreType.CONFIGURATION, configStore);
        datastores.put(LogicalDatastoreType.OPERATIONAL, operStore);

        ExecutorService listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                16, 16, "CommitFutures");

        final ConcurrentDOMDataBroker cdb = new ConcurrentDOMDataBroker(datastores, listenableFutureExecutor);
        this.transactionProvider = new TransactionProvider(cdb, sessionIdForReporting);
        this.currentSchemaContext = new CurrentSchemaContext(schemaService);

    }

    @Test
    public void testEmptyDatastore() throws Exception {
        assertEmptyDatastore(get());
        assertEmptyDatastore(getConfigCandidate());
        assertEmptyDatastore(getConfigRunning());
    }

    @Test
    public void testEditRunning() throws Exception {

        try {
            edit("messages/mapping/editConfigs/editConfig_running.xml");
            fail("Should have failed - edit config on running datastore is not supported");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.operation_not_supported);
            assertTrue(e.getErrorType() == ErrorType.protocol);
        }

    }

    @Test
    public void testCandidateTransaction() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_n1.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_n1_control.xml"));
        assertEmptyDatastore(getConfigRunning());

        verifyResponse(discardChanges(), RPC_REPLY_OK);
        assertEmptyDatastore(getConfigCandidate());

    }

    @Test
    public void testEditWithCommit() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_n1.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_n1_control.xml"));

        verifyResponse(commit(), RPC_REPLY_OK);
        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_n1_control.xml"));

        deleteDatastore();

    }

    @Test
    public void testKeyOrder() throws Exception {
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_keys_1.xml"), RPC_REPLY_OK);
        verifyResponse(commit(), RPC_REPLY_OK);
        final Document configRunning = getConfigRunning();
        final String responseAsString = XmlUtil.toString(configRunning);
        verifyResponse(configRunning, XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_keys_1_control.xml"));

        final int key3 = responseAsString.indexOf("key3");
        final int key1 = responseAsString.indexOf("key1");
        final int key2 = responseAsString.indexOf("key2");

        assertTrue(String.format("Key ordering invalid, should be key3(%d) < key1(%d) < key2(%d)", key3, key1, key2),
            key3 < key1 && key1 < key2);

        deleteDatastore();
    }

    @Test
    public void testMultipleEditsWithMerge() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_1.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_control_1.xml"));
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_single_1.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_control_2.xml"));
        assertEmptyDatastore(getConfigRunning());

        verifyResponse(commit(), RPC_REPLY_OK);
        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_control_2.xml"));

        deleteDatastore();

    }

    @Test
    public void testMoreComplexEditConfigs() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_1.xml"), RPC_REPLY_OK);
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_single_1.xml"), RPC_REPLY_OK);

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_2.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_after_more_complex_merge.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_3.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_after_more_complex_merge_2.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_4_replace.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_after_replace.xml"));
        verifyResponse(commit(), RPC_REPLY_OK);

        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_after_replace.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_replace_default.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_replace_default_control.xml"));
        verifyResponse(commit(), RPC_REPLY_OK);

        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_replace_default_control.xml"));

        deleteDatastore();

    }

    @Test
    public void testLock() throws Exception {

        verifyResponse(lockCandidate(), RPC_REPLY_OK);

        try {
            lock();
            fail("Should have failed - locking of running datastore is not supported");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.operation_not_supported);
            assertTrue(e.getErrorType() == ErrorType.application);
        }
    }

    @Test
    public void testUnlock() throws Exception {

        verifyResponse(unlockCandidate(), RPC_REPLY_OK);

        try {
            unlock();
            fail("Should have failed - unlocking of running datastore is not supported");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.operation_not_supported);
            assertTrue(e.getErrorType() == ErrorType.application);
        }
    }

    @Test
    public void testEditWithCreate() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_create.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfig_create_n1_control.xml"));


        try {
            edit("messages/mapping/editConfigs/editConfig_create.xml");
            fail("Create should have failed - data already exists");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.data_exists);
            assertTrue(e.getErrorType() == ErrorType.protocol);
        }

        verifyResponse(discardChanges(), RPC_REPLY_OK);

    }

    @Test
    public void testDeleteNonExisting() throws Exception {

        assertEmptyDatastore(getConfigCandidate());
        assertEmptyDatastore(getConfigRunning());

        try {
            edit("messages/mapping/editConfigs/editConfig_delete-top.xml");
            fail("Delete should have failed - data is missing");
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.data_missing);
            assertTrue(e.getErrorType() == ErrorType.protocol);
        }

    }

    @Test
    public void testEditMissingDefaultOperation() throws Exception {

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_missing_default-operation_1.xml"), RPC_REPLY_OK);
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_missing_default-operation_2.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_missing_default-operation_control.xml"));

        verifyResponse(commit(), RPC_REPLY_OK);
        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_missing_default-operation_control.xml"));

        deleteDatastore();
    }

    public static void printDocument(Document doc) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc),
                new StreamResult(writer));
        LOG.warn(writer.getBuffer().toString());
    }

    @Test
    public void testEditConfigWithMultipleOperations() throws Exception {
        deleteDatastore();

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_setup.xml"), RPC_REPLY_OK);
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_1.xml"), RPC_REPLY_OK);

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_2.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_2_control.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_3_leaf_operations.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_3_control.xml"));

        deleteDatastore();

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_setup.xml"), RPC_REPLY_OK);
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_default-replace.xml"), RPC_REPLY_OK);

        try {
            edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_create_existing.xml");
            fail();
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.data_exists);
            assertTrue(e.getErrorType() == ErrorType.protocol);
        }

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_delete_children_operations.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_delete_children_operations_control.xml"));
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_remove-non-existing.xml"), RPC_REPLY_OK);

        try {
            edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_delete-non-existing.xml");
            fail();
        } catch (NetconfDocumentedException e) {
            assertTrue(e.getErrorSeverity() == ErrorSeverity.error);
            assertTrue(e.getErrorTag() == ErrorTag.data_missing);
            assertTrue(e.getErrorType() == ErrorType.protocol);
        }

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_5_choice_setup.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_5_choice_setup-control.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_5_choice_setup2.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_5_choice_setup2-control.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_merge_multiple_operations_5_choice_delete.xml"), RPC_REPLY_OK);
        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/editConfigs/editConfig_merge_multiple_operations_4_delete_children_operations_control.xml"));

        deleteDatastore();
    }

    @Test
    public void testFiltering() throws Exception {

        assertEmptyDatastore(getConfigCandidate());
        assertEmptyDatastore(getConfigRunning());

        verifyResponse(getConfigCandidate(), XmlFileLoader.xmlFileToDocument("messages/mapping/get-empty-response.xml"));
        verifyResponse(getConfigRunning(), XmlFileLoader.xmlFileToDocument("messages/mapping/get-empty-response.xml"));
        verifyResponse(getConfigWithFilter("messages/mapping/filters/get-filter-users.xml"),
                XmlFileLoader.xmlFileToDocument("messages/mapping/get-empty-response.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig-filtering-setup.xml"), RPC_REPLY_OK);
        verifyResponse(commit(), RPC_REPLY_OK);

        //TODO uncomment these tests once we can parse KeyedListNode as a selection node, currently you cannot use a KeyedList as a selection node in filter
//        verifyFilterIdentifier("messages/mapping/filters/get-filter-alluser.xml",
//                YangInstanceIdentifier.builder().node(TOP).node(USERS).node(USER).build());
        verifyFilterIdentifier("messages/mapping/filters/get-filter-company-info.xml",
                YangInstanceIdentifier.builder().node(TOP).node(USERS).node(USER).build());
        verifyFilterIdentifier("messages/mapping/filters/get-filter-modules-and-admin.xml",
                YangInstanceIdentifier.builder().node(TOP).build());
        verifyFilterIdentifier("messages/mapping/filters/get-filter-only-names-types.xml",
                YangInstanceIdentifier.builder().node(TOP).node(USERS).node(USER).build());
//        verifyFilterIdentifier("messages/mapping/filters/get-filter-specific-module-type-and-user.xml",
//                YangInstanceIdentifier.builder().node(TOP).build());
//        verifyFilterIdentifier("messages/mapping/filters/get-filter-superuser.xml",
//                YangInstanceIdentifier.builder().node(TOP).node(USERS).node(USER).build());
        verifyFilterIdentifier("messages/mapping/filters/get-filter-users.xml",
                YangInstanceIdentifier.builder().node(TOP).node(USERS).build());

        YangInstanceIdentifier ident = YangInstanceIdentifier.
                builder(AUGMENTED_CONTAINER_IN_MODULES).
                node(AUGMENTED_CONTAINER).
                node(AUGMENTED_STRING_IN_CONT).build();

        verifyFilterIdentifier("messages/mapping/filters/get-filter-augmented-string.xml", ident);
        verifyFilterIdentifier("messages/mapping/filters/get-filter-augmented-case.xml",
                YangInstanceIdentifier.builder().node(TOP).node(CHOICE_NODE).node(AUGMENTED_CASE).build());

        verifyResponse(getConfigWithFilter("messages/mapping/filters/get-filter-augmented-case.xml"),
                XmlFileLoader.xmlFileToDocument("messages/mapping/filters/response-augmented-case.xml"));
        verifyResponse(edit("messages/mapping/editConfigs/editConfig-filtering-setup2.xml"), RPC_REPLY_OK);
        verifyResponse(commit(), RPC_REPLY_OK);

        verifyFilterIdentifier("messages/mapping/filters/get-filter-augmented-case-inner-choice.xml",
                YangInstanceIdentifier.builder().node(TOP).node(CHOICE_NODE).node(CHOICE_WRAPPER).build());
        verifyFilterIdentifier("messages/mapping/filters/get-filter-augmented-case-inner-case.xml",
                YangInstanceIdentifier.builder().node(TOP).node(CHOICE_NODE).node(CHOICE_WRAPPER).node(INNER_CHOICE).node(INNER_CHOICE_TEXT).build());

        verifyResponse(getConfigWithFilter("messages/mapping/filters/get-filter-augmented-string.xml"),
                XmlFileLoader.xmlFileToDocument("messages/mapping/filters/response-augmented-string.xml"));
        verifyResponse(getConfigWithFilter("messages/mapping/filters/get-filter-augmented-case-inner-choice.xml"),
                XmlFileLoader.xmlFileToDocument("messages/mapping/filters/response-augmented-case-inner-choice.xml"));
        verifyResponse(getConfigWithFilter("messages/mapping/filters/get-filter-augmented-case-inner-case.xml"),
                XmlFileLoader.xmlFileToDocument("messages/mapping/filters/response-augmented-case-inner-choice.xml"));

        verifyResponse(edit("messages/mapping/editConfigs/editConfig_delete-top.xml"), RPC_REPLY_OK);
        verifyResponse(commit(), RPC_REPLY_OK);

    }

    private void verifyFilterIdentifier(String resource, YangInstanceIdentifier identifier) throws Exception{
        TestingGetConfig getConfig = new TestingGetConfig(sessionIdForReporting, currentSchemaContext, transactionProvider);
        Document request = XmlFileLoader.xmlFileToDocument(resource);
        YangInstanceIdentifier iid = getConfig.getInstanceIdentifierFromDocument(request);
        assertTrue(iid.equals(identifier));
    }

    private class TestingGetConfig extends GetConfig{
        public TestingGetConfig(String sessionId, CurrentSchemaContext schemaContext, TransactionProvider transactionProvider) {
            super(sessionId, schemaContext, transactionProvider);
        }

        public YangInstanceIdentifier getInstanceIdentifierFromDocument(Document request) throws NetconfDocumentedException {
            XmlElement filterElement = XmlElement.fromDomDocument(request).getOnlyChildElement(GET_CONFIG).getOnlyChildElement(FILTER_NODE);
            return getInstanceIdentifierFromFilter(filterElement);
        }
    }

    private void deleteDatastore() throws Exception{
        verifyResponse(edit("messages/mapping/editConfigs/editConfig_delete-root.xml"), RPC_REPLY_OK);
        assertEmptyDatastore(getConfigCandidate());

        verifyResponse(commit(), RPC_REPLY_OK);
        assertEmptyDatastore(getConfigRunning());
    }

    private void verifyResponse(Document response, Document template) throws IOException, TransformerException {
        DetailedDiff dd = new DetailedDiff(new Diff(response, template));
        dd.overrideElementQualifier(new NetconfXmlUnitRecursiveQualifier());

        printDocument(response);
        printDocument(template);

        assertTrue(dd.toString(), dd.similar());
    }

    private void assertEmptyDatastore(Document response) {

        NodeList nodes = response.getChildNodes();
        assertTrue(nodes.getLength() == 1);

        assertEquals(nodes.item(0).getLocalName(),RPC_REPLY_ELEMENT);

        NodeList replyNodes = nodes.item(0).getChildNodes();
        assertTrue(replyNodes.getLength() == 1);

        Node dataNode = replyNodes.item(0);
        assertEquals(dataNode.getLocalName(), DATA_ELEMENT);
        assertFalse(dataNode.hasChildNodes());

    }

    private Document commit() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Commit commit = new Commit(sessionIdForReporting, transactionProvider);
        return executeOperation(commit, "messages/mapping/commit.xml");
    }

    private Document discardChanges() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        DiscardChanges discardOp = new DiscardChanges(sessionIdForReporting, transactionProvider);
        return executeOperation(discardOp, "messages/mapping/discardChanges.xml");
    }

    private Document edit(String resource) throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        EditConfig editConfig = new EditConfig(sessionIdForReporting, currentSchemaContext, transactionProvider);
        return executeOperation(editConfig, resource);
    }

    private Document get() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Get get = new Get(sessionIdForReporting, currentSchemaContext, transactionProvider);
        return executeOperation(get, "messages/mapping/get.xml");
    }

    private Document getConfigRunning() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        GetConfig getConfig = new GetConfig(sessionIdForReporting, currentSchemaContext, transactionProvider);
        return executeOperation(getConfig, "messages/mapping/getConfig.xml");
    }

    private Document getConfigCandidate() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        GetConfig getConfig = new GetConfig(sessionIdForReporting, currentSchemaContext, transactionProvider);
        return executeOperation(getConfig, "messages/mapping/getConfig_candidate.xml");
    }

    private Document getConfigWithFilter(String resource) throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        GetConfig getConfig = new GetConfig(sessionIdForReporting, currentSchemaContext, transactionProvider);
        return executeOperation(getConfig, resource);
    }

    private Document lock() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Lock lock = new Lock(sessionIdForReporting);
        return executeOperation(lock, "messages/mapping/lock.xml");
    }

    private Document unlock() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Unlock unlock = new Unlock(sessionIdForReporting);
        return executeOperation(unlock, "messages/mapping/unlock.xml");
    }

    private Document lockCandidate() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Lock lock = new Lock(sessionIdForReporting);
        return executeOperation(lock, "messages/mapping/lock_candidate.xml");
    }

    private Document unlockCandidate() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Unlock unlock = new Unlock(sessionIdForReporting);
        return executeOperation(unlock, "messages/mapping/unlock_candidate.xml");
    }

    private Document executeOperation(NetconfOperation op, String filename) throws ParserConfigurationException, SAXException, IOException, NetconfDocumentedException {
        final Document request = XmlFileLoader.xmlFileToDocument(filename);

        HandlingPriority priority = op.canHandle(request);
        Preconditions.checkState(priority != HandlingPriority.CANNOT_HANDLE);

        final Document response = op.handle(request, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);

        LOG.debug("Got response {}" , response);

        return response;
    }

    private Collection<InputStream> getYangSchemas() {
        final List<String> schemaPaths = Arrays.asList("/META-INF/yang/config.yang", "/yang/mdsal-netconf-mapping-test.yang");
        final List<InputStream> schemas = new ArrayList<>();

        for (String schemaPath : schemaPaths) {
            InputStream resourceAsStream = getClass().getResourceAsStream(schemaPath);
            schemas.add(resourceAsStream);
        }

        return schemas;
    }

    private SchemaContext parseSchemas(Collection<InputStream> schemas) throws IOException, YangSyntaxErrorException {
        final YangParserImpl parser = new YangParserImpl();
        Collection<ByteSource> sources = BuilderUtils.streamsToByteSources(schemas);
        return parser.parseSources(sources);
    }

    private SchemaService createSchemaService() {
        return new SchemaService() {

            @Override
            public void addModule(Module module) {
            }

            @Override
            public void removeModule(Module module) {

            }

            @Override
            public SchemaContext getSessionContext() {
                return schemaContext;
            }

            @Override
            public SchemaContext getGlobalContext() {
                return schemaContext;
            }

            @Override
            public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener listener) {
                listener.onGlobalContextUpdated(getGlobalContext());
                return new ListenerRegistration<SchemaContextListener>() {
                    @Override
                    public void close() {

                    }

                    @Override
                    public SchemaContextListener getInstance() {
                        return listener;
                    }
                };
            }
        };
    }
}