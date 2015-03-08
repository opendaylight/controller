/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.input.to.cnsn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.valid.DataValidationException;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPutListDataTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextTestModule;

    private static final String TEST_MODULE_NS_STRING = "test:module";
    private static final URI TEST_MODULE_NS;
    private static final String TEST_MODULE_REVISION = "2014-01-09";

    static {
        TEST_MODULE_NS = URI.create("test:module");
    }

    @Before
    public void initialize() throws FileNotFoundException {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setSchemas(schemaContextTestModule);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        when(brokerFacade.commitConfigurationDataPut(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(mock(CheckedFuture.class));
    }

    /**
     * Tests whether no exception is raised if number and values of keys in URI
     * and payload are equal
     */
    @Test
    public void testValidKeys() {
        putListDataTest("key1value", "15", "key1value", (short) 15);
    }

    /**
     * Tests whether an exception is raised if key values in URI and payload are
     * different.
     *
     * The exception should be raised from validation method
     * {@code RestconfImpl#validateListEqualityOfListInDataAndUri}
     */
    @Test
    @Ignore // RestconfDocumentedExceptionMapper needs update
    public void testUriAndPayloadKeysDifferent() {
        try {
            putListDataTest("key1value", "15", "key1value", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (final RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        try {
            putListDataTest("key1value", "15", "key1value1", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (final RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    /**
     * Tests whether an exception is raised if URI contains less key values then
     * payload.
     *
     * The exception is raised during {@code InstanceIdentifier} instance is
     * built from URI
     */
    @Test
    public void testMissingKeysInUri() {
        try {
            putListDataTest("key1value", null, "key1value", (short) 15);
            fail("RestconfDocumentedException expected");
        } catch (final RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
        }
    }

    /**
     * Tests whether an exception is raised if URI contains more key values then
     * payload.
     *
     * The exception should be raised from validation method
     * {@code RestconfImpl#validateListEqualityOfListInDataAndUri}
     */
    @Test
    public void testMissingKeysInPayload() {
        try {
            putListDataTest("key1value", "15", "key1value", null);
            fail("RestconfDocumentedException expected");
        } catch (final DataValidationException e) {
            // FIXME: thing about different approach for testing the Exception states
            // RestconfDocumentedException is not rise in new API because you get
            // DataValidationException from putListDataTest before you call the real rest service
//            verifyException(e, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
        }
    }

    private void verifyException(final RestconfDocumentedException e, final ErrorType errorType, final ErrorTag errorTag) {
        final List<RestconfError> errors = e.getErrors();
        assertEquals("getErrors() size", 1, errors.size());
        assertEquals("RestconfError getErrorType()", errorType, errors.get(0).getErrorType());
        assertEquals("RestconfError getErrorTag()", errorTag, errors.get(0).getErrorTag());
    }

    public void putListDataTest(final String uriKey1, final String uriKey2, final String payloadKey1,
            final Short payloadKey2) {
        final QName lstWithCompositeKey = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "lst-with-composite-key");
        final QName key1 = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "key1");
        final QName key2 = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "key2");

        final DataSchemaNode testNodeSchemaNode = schemaContextTestModule.getDataChildByName(lstWithCompositeKey);
        assertTrue(testNodeSchemaNode != null);
        assertTrue(testNodeSchemaNode instanceof ListSchemaNode);
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> testNodeContainer =
                Builders.mapEntryBuilder((ListSchemaNode) testNodeSchemaNode);

        List<DataSchemaNode> testChildren = ControllerContext.findInstanceDataChildrenByName(
                (ListSchemaNode) testNodeSchemaNode, key1.getLocalName());
        assertTrue(testChildren != null);
        final DataSchemaNode testLeafKey1SchemaNode = Iterables.getFirst(testChildren, null);
        assertTrue(testLeafKey1SchemaNode != null);
        assertTrue(testLeafKey1SchemaNode instanceof LeafSchemaNode);
        final NormalizedNodeAttrBuilder<NodeIdentifier, Object, LeafNode<Object>> leafKey1 =
                Builders.leafBuilder((LeafSchemaNode) testLeafKey1SchemaNode);
        leafKey1.withValue(payloadKey1);
        testNodeContainer.withChild(leafKey1.build());

        if (payloadKey2 != null) {
            testChildren = ControllerContext.findInstanceDataChildrenByName(
                    (ListSchemaNode) testNodeSchemaNode, key2.getLocalName());
            assertTrue(testChildren != null);
            final DataSchemaNode testLeafKey2SchemaNode = Iterables.getFirst(testChildren, null);
            assertTrue(testLeafKey2SchemaNode != null);
            assertTrue(testLeafKey2SchemaNode instanceof LeafSchemaNode);
            final NormalizedNodeAttrBuilder<NodeIdentifier, Object, LeafNode<Object>> leafKey2 =
                    Builders.leafBuilder((LeafSchemaNode) testLeafKey2SchemaNode);
            leafKey2.withValue(payloadKey2);
            testNodeContainer.withChild(leafKey2.build());
        }

        final NormalizedNodeContext testCompositeContext = new NormalizedNodeContext(new InstanceIdentifierContext(
                null, testNodeSchemaNode, null, schemaContextTestModule), testNodeContainer.build());

        restconfImpl.updateConfigurationData(toUri(uriKey1, uriKey2), testCompositeContext);
    }

    public void putListDataWithWrapperTest(final String uriKey1, final String uriKey2, final String payloadKey1,
            final Short payloadKey2) {
        putListDataTest(uriKey1, uriKey2, payloadKey1, payloadKey2);
    }

    private String toUri(final String uriKey1, final String uriKey2) {
        final StringBuilder uriBuilder = new StringBuilder("/test-module:lst-with-composite-key/");
        uriBuilder.append(uriKey1);
        if (uriKey2 != null) {
            uriBuilder.append("/");
            uriBuilder.append(uriKey2);
        }
        return uriBuilder.toString();
    }

}
