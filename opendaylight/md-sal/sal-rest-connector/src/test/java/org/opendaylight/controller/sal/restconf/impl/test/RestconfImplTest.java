/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.AsyncEventBus;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

//import java.awt.List;

/**
 * @See {@link InvokeRpcMethodTest}
 *
 */
public class RestconfImplTest {

    private RestconfImpl restconfImpl = null;
    private RestconfService restconfImplInterface = null;
    private static ControllerContext controllerContext = null;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy(ControllerContext.getInstance());
        controllerContext.setSchemas(schemaContext);
    }

    @Before
    public void initMethod() {
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setControllerContext(controllerContext);
    }

    /**
     * It tests error case when method subscribeToStream can't find stream name for specified identifier
     */
    @Test
    public void testSubscribeToStreamWithoutListener() throws Exception {
        try {
            restconfImpl.subscribeToStream("identifier", null);
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.UNKNOWN_ELEMENT, ErrorType.PROTOCOL, null);
        }
    }

    /**
     *
     * It tests error case when subscribeToStream method input parameter identifier isn't specified (=null)
     */
    @Test
    public void testSubscribeToStreamWithNullListener() throws Exception {
        try {
            restconfImpl.subscribeToStream(null, null);
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * It tests error case when subscribeToStream method when datastore URI parameter is missing in identifier
     */
    @Test
    public void testSubscribeToStreamWithMissingDatastoreInUri() throws Exception {
        String identifier = "streamName";
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier,
                new AsyncEventBus(Executors.newSingleThreadExecutor()));
        try {
            restconfImpl.subscribeToStream(identifier, null);
        } catch (RestconfDocumentedException e) {
            List<RestconfError> errValues = e.getErrors();
            assertEquals(ErrorType.APPLICATION, errValues.get(0).getErrorType());
            assertEquals(ErrorTag.MISSING_ATTRIBUTE, errValues.get(0).getErrorTag());
            assertEquals("Stream name doesn't contains datastore value (pattern /datastore=)", errValues.get(0)
                    .getErrorMessage());
        }
    }

    /**
     * It tests error case when subscribeToStream method when scope URI parameter is missing in identifier
     */
    @Test
    public void testSubscribeToStreamWithMissingScope() throws Exception {
        String identifier = "streamName/datastore=OPERATIONAL";
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier,
                new AsyncEventBus(Executors.newSingleThreadExecutor()));
        try {
            restconfImpl.subscribeToStream(identifier, null);
        } catch (RestconfDocumentedException e) {
            List<RestconfError> errValues = e.getErrors();
            assertEquals(ErrorType.APPLICATION, errValues.get(0).getErrorType());
            assertEquals(ErrorTag.MISSING_ATTRIBUTE, errValues.get(0).getErrorTag());
            assertEquals("Stream name doesn't contains datastore value (pattern /scope=)", errValues.get(0)
                    .getErrorMessage());
        }
    }

    @Test
    public void testSubscribeToStreamRegisterToLDC() throws Exception {
        String identifier = "streamName/datastore=OPERATIONAL/scope=SUBTREE";
        BrokerFacade brokerFacade = BrokerFacade.getInstance();
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier,
                new AsyncEventBus(Executors.newSingleThreadExecutor()));
        restconfImpl.setBroker(brokerFacade);
        brokerFacade.setContext(mock(ConsumerSession.class));
        DOMDataBroker domDBMock = mock(DOMDataBroker.class);
        brokerFacade.setDomDataBroker(domDBMock);

        UriInfo ui = mock(UriInfo.class);
        UriBuilder uiPathBuilder = UriBuilder.fromUri(URI.create("http://example.org:4242"));
        when(ui.getAbsolutePathBuilder()).thenReturn(uiPathBuilder);

        Response response = restconfImpl.subscribeToStream(identifier, ui);

        Mockito.verify(domDBMock, Mockito.times(1)).registerDataChangeListener(any(LogicalDatastoreType.class),
                any(YangInstanceIdentifier.class), any(ListenerAdapter.class), any(DataChangeScope.class));

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("http://example.org:8181/streamName/datastore=OPERATIONAL/scope=SUBTREE", response
                .getLocation().toString());
    }

    /**
     * tests error case in deleteConfigurationData when deleting from data isn't successfull
     */
    @Test
    public void testDeleteConfigurationDataWithIncorrectInstanceIdentifier() throws Exception {
        String identifier = "incorrect-instance-identifier";
        ControllerContext ctrlContext = mock(ControllerContext.class);

        InstanceIdentifierContext mockedInstIdContext = mock(InstanceIdentifierContext.class);
        when(mockedInstIdContext.getMountPoint()).thenReturn(null);
        when(mockedInstIdContext.getInstanceIdentifier()).thenReturn(null);

        when(ctrlContext.toInstanceIdentifier(eq(identifier))).thenReturn(mockedInstIdContext);
        restconfImpl.setControllerContext(ctrlContext);
        try {
            restconfImpl.deleteConfigurationData(identifier);
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.OPERATION_FAILED, ErrorType.APPLICATION, "Error creating data");
        }
    }

    @Test
    public void testGetOperationalReceived() throws Exception {
        assertNull(restconfImpl.getOperationalReceived());
    }

    /**
     * Not existing node is references in input data
     *
     */
    @Test
    public void normalizeNodeWithNotExistingNode() throws URISyntaxException {
        try {
            restconfImpl.createConfigurationData(prepareData("non-existing"));
            fail("Call of createConfigurationData can't be sucessfull");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * Data contains node which exists but isn't of type Data node container
     *
     */
    @Test
    public void normalizeNodeWithNoDataNodeContainer() throws URISyntaxException {
        try {
            restconfImpl.createConfigurationData(prepareData("no-data-container"));
            fail("Call of createConfigurationData can't be sucessfull");
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL,
                    "Root element has to be container or list yang datatype.");
        }
    }

    /**
     * Data contains top level data as simple node wrapper
     *
     */
    @Test
    public void normalizeNodeWithTopElementAsSimpleNodeWrapper() throws URISyntaxException {
        BrokerFacade mockBrokerFacade = mock(BrokerFacade.class);
        mockBrokerFacade.commitConfigurationDataPost(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        restconfImpl.setBroker(mockBrokerFacade);
        Response response = restconfImpl.createConfigurationData(prepareDataTopElementAsSimpleNode(true));
        assertEquals(204, response.getStatus());
    }

    /**
     * Data contains top level data as simple node (but not wrapper)
     *
     */
    @Test
    public void normalizeNodeWithTopElementAsSimpleNode() throws URISyntaxException {
        try {
            restconfImpl.createConfigurationData(prepareDataTopElementAsSimpleNode(false));
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.INVALID_VALUE, ErrorType.APPLICATION,
                    "Top level element is not interpreted as composite node.");
        }
    }

    /**
     * Data contains namesake nodes without specified namespace (exception awaited)
     *
     */
    @Test
    public void normalizeNodeWithNamesakesInData() throws URISyntaxException {
        try {
            restconfImpl.createConfigurationData(prepareNamesakeData());
        } catch (RestconfDocumentedException e) {
            verifyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL,
                    "is added as augment from more than one module");
        }
    }

    private CompositeNode prepareData(final String topElementName) throws URISyntaxException {
        SimpleNodeWrapper nameNode = new SimpleNodeWrapper("name", "inf1");
        CompositeNodeWrapper interfaceNode = new CompositeNodeWrapper("interface");
        interfaceNode.addValue(nameNode);
        CompositeNodeWrapper interfacesNode = new CompositeNodeWrapper(new URI(
                "urn:ietf:params:xml:ns:yang:ietf-interfaces"), topElementName);
        interfacesNode.addValue(interfaceNode);
        return interfacesNode;
    }

    private CompositeNode prepareNamesakeData() throws URISyntaxException {
        SimpleNodeWrapper nameNode = new SimpleNodeWrapper("namesake", "namesake value");
        CompositeNodeWrapper interfacesNode = new CompositeNodeWrapper(new URI(
                "urn:ietf:params:xml:ns:yang:ietf-interfaces"), "interfaces");
        interfacesNode.addValue(nameNode);
        return interfacesNode;
    }

    private SimpleNode<?> prepareDataTopElementAsSimpleNode(final boolean wrapper) throws URISyntaxException {
        if (wrapper) {
            return new SimpleNodeWrapper(new URI("urn:ietf:params:xml:ns:yang:ietf-interfaces"), "interfaces", null);
        } else {
            return NodeFactory.createImmutableSimpleNode(
                    QName.create("urn:ietf:params:xml:ns:yang:ietf-interfaces", "2013-07-04", "interfaces"), null,
                    null, null);
        }
    }

    private void verifyException(final RestconfDocumentedException e, final ErrorTag errorTag,
            final ErrorType errorType, final String message) {
        List<RestconfError> errors = e.getErrors();
        assertEquals(1, errors.size());
        assertEquals(errorTag, errors.get(0).getErrorTag());
        assertEquals(errorType, errors.get(0).getErrorType());
        if (message != null) {
            assertTrue(errors.get(0).getErrorMessage().contains(message));
        }
    }
}
