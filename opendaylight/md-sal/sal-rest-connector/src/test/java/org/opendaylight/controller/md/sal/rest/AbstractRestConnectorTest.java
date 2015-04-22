/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 * Basic abstract help class for a Restconf test suite.
 * Use it for all classes witch are working with {@link RestConnectorProviderImpl}
 * in JUnit BeforeClass statement.
 * Class provides help methods for e.g. loadingSchemaContext or mocking
 * parts of {@link RestConnectorProviderImpl}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 20, 2015
 */
public abstract class AbstractRestConnectorTest {

    protected static RestConnectorProviderImpl restConnector = RestConnectorProviderImpl.getInstance();

    @Mock
    protected ProviderSession mockSession;
    @Mock
    protected DOMMountPointService mockMountPointService;
    @Mock
    protected SchemaService mockSchemaService;
    @Mock
    protected DOMRpcService mockRpcService;
    @Mock
    protected DOMDataBroker mockDataBroker;

    public void initialization() {
        final SchemaContext schemaCx = TestRestconfUtils.loadSchemaContext("/modules", null);
        Mockito.when(mockSchemaService.getGlobalContext()).thenReturn(schemaCx);
        Mockito.when(mockSession.getService(DOMMountPointService.class)).thenReturn(mockMountPointService);
        Mockito.when(mockSession.getService(SchemaService.class)).thenReturn(mockSchemaService);
        Mockito.when(mockSession.getService(DOMRpcService.class)).thenReturn(mockRpcService);
        Mockito.when(mockSession.getService(DOMDataBroker.class)).thenReturn(mockDataBroker);
        restConnector.setWebsocketPort(new PortNumber(8181));
        restConnector.onSessionInitiated(mockSession);
    }

    public void closing() throws Exception {
        restConnector.close();
    }

    /**
     * Method overwrite actual global schema from RestConnectorImpl
     *
     * @param schemaContext
     */
    protected static void setSchemaContext(final SchemaContext schemaContext) {
        restConnector.onGlobalContextUpdated(schemaContext);
    }

    /**
     * Method prepare default mocked {@link ProviderSession} and initialize
     * {@link RestConnectorProviderImpl} for using in a test suite.
     */
    protected static void defaultOnSessionInitMocking() {
        final ProviderSession ps = mock(ProviderSession.class);
        final SchemaService ss = mock(SchemaService.class);
        when(ss.getGlobalContext()).thenReturn(null);
        when(ps.getService(SchemaService.class)).thenReturn(ss);
        restConnector.onSessionInitiated(ps);
    }

    /**
     * Method is loading SchemaContext from given path from src/test/resources
     * <code>Note:</code> existing loaded SchemaContext will be updated only.
     *
     * @param yangPath
     */
    protected static void loadSchemaContextFromFile(final String yangPath) {
        SchemaContext sc = null;
        try {
            sc = RestConnectorProviderImpl.getSchemaContext();
            // we have to set null for mocked SchemaContext
            if (Mockito.mockingDetails(sc).isMock()) {
                sc = null;
            }
        }
        catch (final RestconfDocumentedException e) {
            // NOOP
        }
        sc = TestRestconfUtils.loadSchemaContext(yangPath, sc);
        restConnector.onGlobalContextUpdated(sc);
    }

    protected static void baseValidationNormalizedNodeContext(final NormalizedNodeContext nnCx, final String expectedLocalName) {
        Assert.assertNotNull(nnCx);
        Assert.assertNotNull(nnCx.getData());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext().getSchemaNode());
        Assert.assertNotNull(nnCx.getInstanceIdentifierContext().getSchemaContext());
        Assert.assertEquals(expectedLocalName, nnCx.getData().getNodeType().getLocalName());
        Assert.assertTrue(nnCx.getInstanceIdentifierContext().getSchemaNode().getQName().toString().contains(expectedLocalName));
    }

    protected void mockWriteDefaultDataBroker(final YangInstanceIdentifier yii) {
        final DOMDataReadWriteTransaction readWriteTx = Mockito.mock(DOMDataReadWriteTransaction.class);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures.immediateCheckedFuture(null);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedReadFuture =
                Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> absent());
        Mockito.when(readWriteTx.read(LogicalDatastoreType.CONFIGURATION, yii)).thenReturn(checkedReadFuture);
        final CheckedFuture<Boolean, ReadFailedException> checkedExistFuture = Futures.immediateCheckedFuture(Boolean.TRUE);
        Mockito.when(readWriteTx.exists(Matchers.eq(LogicalDatastoreType.CONFIGURATION), Matchers.any(YangInstanceIdentifier.class)))
                .thenReturn(checkedExistFuture);
        Mockito.when(readWriteTx.submit()).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newReadWriteTransaction()).thenReturn(readWriteTx);
    }

    protected void mockDeleteDefaultDataBroker() {
        final DOMDataWriteTransaction writeOnlyTx = Mockito.mock(DOMDataWriteTransaction.class);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures.immediateCheckedFuture(null);
        Mockito.when(writeOnlyTx.submit()).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTx);
    }

    protected void mockReadDefaultDataBroker(final NormalizedNode<?, ?> nn, final YangInstanceIdentifier yii, final LogicalDatastoreType ds) {
        final DOMDataReadOnlyTransaction readOnlyTx = Mockito.mock(DOMDataReadOnlyTransaction.class);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedFuture =
                Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> of(nn));
        Mockito.when(readOnlyTx.read(ds, yii)).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTx);
    }
}
