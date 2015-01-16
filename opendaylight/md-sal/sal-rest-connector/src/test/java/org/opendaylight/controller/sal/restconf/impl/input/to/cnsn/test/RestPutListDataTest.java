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

import com.google.common.util.concurrent.CheckedFuture;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
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
        ControllerContext controllerContext = ControllerContext.getInstance();
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
    public void testUriAndPayloadKeysDifferent() {
        try {
            putListDataTest("key1value", "15", "key1value", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        try {
            putListDataTest("key1value", "15", "key1value1", (short) 16);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
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
        } catch (RestconfDocumentedException e) {
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
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
        }
        try {
            putListDataWithWrapperTest("key1value", "15", "key1value", null);
            fail("RestconfDocumentedException expected");
        } catch (RestconfDocumentedException e) {
            // this exception is raised from RestconfImpl.normalizeCompositeNode()
            verifyException(e, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
        }

    }

    private void verifyException(final RestconfDocumentedException e, final ErrorType errorType, final ErrorTag errorTag) {
        List<RestconfError> errors = e.getErrors();
        assertEquals("getErrors() size", 1, errors.size());
        assertEquals("RestconfError getErrorType()", errorType, errors.get(0).getErrorType());
        assertEquals("RestconfError getErrorTag()", errorTag, errors.get(0).getErrorTag());
    }

    public void putListDataTest(final String uriKey1, final String uriKey2, final String payloadKey1,
            final Short payloadKey2) {
        QName lstWithCompositeKey = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "lst-with-composite-key");
        QName key1 = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "key1");
        QName key2 = QName.create(TEST_MODULE_NS_STRING, TEST_MODULE_REVISION, "key2");

        CompositeNodeBuilder<ImmutableCompositeNode> payloadBuilder = ImmutableCompositeNode.builder();
        payloadBuilder.setQName(lstWithCompositeKey).addLeaf(key1, payloadKey1);
        if (payloadKey2 != null) {
            payloadBuilder.addLeaf(key2, payloadKey2);
        }

        restconfImpl.updateConfigurationData(toUri(uriKey1, uriKey2), payloadBuilder.build());
    }

    public void putListDataWithWrapperTest(final String uriKey1, final String uriKey2, final String payloadKey1,
            final Short payloadKey2) {
        CompositeNodeWrapper payloadBuilder = new CompositeNodeWrapper(TEST_MODULE_NS, "lst-with-composite-key");
        payloadBuilder.addValue(new SimpleNodeWrapper(TEST_MODULE_NS, "key1", payloadKey1));
        if (payloadKey2 != null) {
            payloadBuilder.addValue(new SimpleNodeWrapper(TEST_MODULE_NS, "key2", payloadKey2));
        }
        restconfImpl.updateConfigurationData(toUri(uriKey1, uriKey2), payloadBuilder);
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
