/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.input.to.cnsn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.test.DummyFuture;
import org.opendaylight.controller.sal.restconf.impl.test.DummyFuture.Builder;
import org.opendaylight.controller.sal.restconf.impl.test.DummyRpcResult;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPutListDataTest {

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextTestModule;

    private static final String TEST_MODULE_NS = "test:module";
    private static final String TEST_MODULE_REVISION = "2014-01-09";

    @Before
    public void initialize() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setSchemas(schemaContextTestModule);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        Builder<TransactionStatus> futureBuilder = new DummyFuture.Builder<TransactionStatus>();
        futureBuilder.rpcResult(new DummyRpcResult.Builder<TransactionStatus>().result(TransactionStatus.COMMITED)
                .build());
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(futureBuilder.build());
    }

    /**
     * Tests whether no exception is raised if number and values of keys in URI
     * and payload are equal
     */
    @Test
    public void putListDataEqualUriPayloadKeysTest() {
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
    public void putListDataDifferentUriPayloadKeysTest1() {
        try {
            putListDataTest("key1value", "15", "key1value", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.APPLICATION, ErrorTag.MALFORMED_MESSAGE);
        }

        try {
            putListDataTest("key1value", "15", "key1value1", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.APPLICATION, ErrorTag.MALFORMED_MESSAGE);
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
    public void putListDataLessUriMorePayloadKeysTest() {
        try {
            putListDataTest("key1value", null, "key1value", (short) 15);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
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
    public void putListDataMoreUriLessPayloadKeysTest() {
        try {
            putListDataTest("key1value", "15", "key1value", null);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.APPLICATION, ErrorTag.MALFORMED_MESSAGE);
        }
    }

    private void verifyException(final RestconfDocumentedException e, final ErrorType errorType, final ErrorTag errorTag) {
        List<RestconfError> errors = e.getErrors();
        assertEquals(1, errors.size());
        assertEquals(errorType, errors.get(0).getErrorType());
        assertEquals(errorTag, errors.get(0).getErrorTag());
    }

    public void putListDataTest(final String uriKey1, final String uriKey2, final String payloadKey1,
            final Short payloadKey2) {
        QName lstWithCompositeKey = QName.create(TEST_MODULE_NS, TEST_MODULE_REVISION, "lst-with-composite-key");
        QName key1 = QName.create(TEST_MODULE_NS, TEST_MODULE_REVISION, "key1");
        QName key2 = QName.create(TEST_MODULE_NS, TEST_MODULE_REVISION, "key2");

        CompositeNodeBuilder<ImmutableCompositeNode> payloadBuilder = ImmutableCompositeNode.builder();
        payloadBuilder.setQName(lstWithCompositeKey).addLeaf(key1, payloadKey1);
        if (key2 != null) {
            payloadBuilder.addLeaf(key2, payloadKey2);
        }

        final StringBuilder uriBuilder = new StringBuilder("/test-module:lst-with-composite-key/");
        uriBuilder.append(uriKey1);
        if (uriKey2 != null) {
            uriBuilder.append("/");
            uriBuilder.append(uriKey2);
        }
        restconfImpl.updateConfigurationData(uriBuilder.toString(), payloadBuilder.toInstance());
    }

}
