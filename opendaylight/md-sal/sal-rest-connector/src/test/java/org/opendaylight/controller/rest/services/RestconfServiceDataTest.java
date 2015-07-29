package org.opendaylight.controller.rest.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.connector.impl.RestBrokerFacadeImpl;
import org.opendaylight.controller.rest.connector.impl.RestSchemaControllerImpl;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;
import org.opendaylight.controller.rest.test.utils.TestRestconfUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

/**
 * Test class tests {@link RestconfServiceData} class methods for non Mount
 * Point data. POST, PUT, PATCH, DELETE, GET in operational and config DataStore
 */
@RunWith(MockitoJUnitRunner.class)
public class RestconfServiceDataTest {

    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture;

    @Mock
    private CheckedFuture<Boolean, ReadFailedException> mockBoolCheckedFuture;

    @Mock
    @SuppressWarnings("rawtypes")
    private NormalizedNode mockNormalizedNode;

    @Mock
    private NormalizedNodeContext mockNormalizedNodeContext;

    @Mock
    private RestBrokerFacade mockDataBroker;

    @Mock
    private YangInstanceIdentifier mockYangII;

    @Mock
    @SuppressWarnings("rawtypes")
    private InstanceIdentifierContext mockIIC;

    @Mock
    private DOMDataBroker domDataBroker;

    @Mock
    private DOMDataReadWriteTransaction mockRWtx;

    private SchemaContext schemaCx;
    private RestSchemaController restSchemaCx;
    private RestconfServiceData restDataService;
    private RestBrokerFacade restBrokerFacade;

    final String identifier = "test-interface:interfaces/interface/key";

    private final String key = "key";

    @Before
    public void initialization() throws Exception {
        schemaCx = TestRestconfUtils.loadSchemaContext("/test-config-data/yang1/", schemaCx);

        restSchemaCx = new RestSchemaControllerImpl();
        restSchemaCx.setGlobalSchema(schemaCx);

        restDataService = new RestconfServiceDataImpl(mockDataBroker, restSchemaCx);

        restBrokerFacade = new RestBrokerFacadeImpl(domDataBroker);
    }

    @Test(expected=NullPointerException.class)
    public void createRestconfServiceWithNullInputTest() {
        new RestconfServiceDataImpl(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void createRestconfServiceWithNullSchemaContextTest() {
        new RestconfServiceDataImpl(mockDataBroker, null);
    }

    @Test(expected = NullPointerException.class)
    public void createRestconfServiceWithNullBrokerTest() {
        new RestconfServiceDataImpl(null, restSchemaCx);
    }

    @Test
    public void postConfigTest() throws Exception {
        when(
                mockDataBroker.commitConfigurationDataPost(any(SchemaContext.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPost(schemaCx, mockYangII, mockNormalizedNode);

        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPost(any(SchemaContext.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void putConfigTest() {
        when(
                mockDataBroker.commitConfigurationDataPut(any(SchemaContext.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPut(schemaCx, mockYangII, mockNormalizedNode);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPut(any(SchemaContext.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void getConfigTest() {
        when(mockDataBroker.readConfigurationData(any(YangInstanceIdentifier.class))).thenReturn(mockNormalizedNode);
        final NormalizedNode<?, ?> checkedNode = mockDataBroker.readConfigurationData(mockYangII);
        Preconditions.checkNotNull(checkedNode);
        assertEquals(mockNormalizedNode, checkedNode);
        verify(mockDataBroker, times(1)).readConfigurationData(any(YangInstanceIdentifier.class));
    }

    @Test
    public void getOperationalTest() {
        when(mockDataBroker.readOperationalData(any(YangInstanceIdentifier.class))).thenReturn(mockNormalizedNode);
        final NormalizedNode<?, ?> checkedNode = mockDataBroker.readOperationalData(mockYangII);
        Preconditions.checkNotNull(checkedNode);
        assertEquals(mockNormalizedNode, checkedNode);
        verify(mockDataBroker, times(1)).readOperationalData(any(YangInstanceIdentifier.class));
    }

    @Test
    public void deleteConfigTest() {
        when(mockDataBroker.commitConfigurationDataDelete(any(YangInstanceIdentifier.class))).thenReturn(
                mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataDelete(mockYangII);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(mockCheckedFuture, checkedFuture);
        verify(mockDataBroker, times(1)).commitConfigurationDataDelete(any(YangInstanceIdentifier.class));
    }

    /**
     * test for response 204 - no content
     */
    @Test
    public void patchConfigResponse_NO_CONTENT_204_Test() {
        when(mockNormalizedNodeContext.getData()).thenReturn(null);
        final Response resp = restDataService.patchConfigurationData("", mockNormalizedNodeContext);
        assertEquals(204, resp.getStatus());
    }

    /**
     * test for response 200 - OK
     */
    @Test
    public void patchConfigResponse_OK_200_Test() {
        final NormalizedNodeContext normNodeContext = prepareNn(identifier, key);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures
                .immediateCheckedFuture(null);

        when(
                mockDataBroker.commitConfigurationDataPatch(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(checkedFuture);

        final Response respOrig = restDataService.patchConfigurationData(identifier, normNodeContext);
        assertEquals(200, respOrig.getStatus());
    }

    /**
     * test for existing data for update
     *
     * @throws Exception
     */
    @Test(expected = RestconfDocumentedException.class)
    public void patchDataOnPathNotFoundTest() throws Exception {
        when(domDataBroker.newReadWriteTransaction()).thenReturn(mockRWtx);
        when(mockRWtx.exists(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class))).thenReturn(
                mockBoolCheckedFuture);
        when(mockBoolCheckedFuture.get()).thenReturn(false);

        restBrokerFacade.commitConfigurationDataPatch(schemaCx, mockYangII, mockNormalizedNode);
    }

    /**
     * test for PATCH method
     */
    @Test
    public void patchConfigTest() {
        when(
                mockDataBroker.commitConfigurationDataPatch(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPatch(schemaCx, mockYangII, mockNormalizedNode);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPatch(any(SchemaContext.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    /**
     * test for different key in data
     */
    @Test(expected = RestconfDocumentedException.class)
    public void testPatchConfigDataDiferentKey() {
        final String badKey = "badKey";
        final NormalizedNodeContext normNodeContext = prepareNn(identifier, badKey);
        restDataService.patchConfigurationData(identifier, normNodeContext);
    }

    /**
     * test for bad identifier
     */
    @Test(expected = RestconfDocumentedException.class)
    public void testPatchConfigDataMissingUriKey() {
        final String badIdentifier = "test-interface:interfaces/interface";
        restSchemaCx.toInstanceIdentifier(badIdentifier);
    }

    /**
     * prepare normalized node context for tests
     *
     * @param identifier
     * @param key
     * @return
     */
    private NormalizedNodeContext prepareNn(final String identifier, final String key) {
        final InstanceIdentifierContext<?> iiCx = restSchemaCx.toInstanceIdentifier(identifier);
        final MapEntryNode data = mock(MapEntryNode.class);
        final QName qName = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "interface");
        final QName qNameKey = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "name");
        final NodeIdentifierWithPredicates identWithPredicates = new NodeIdentifierWithPredicates(qName, qNameKey, key);

        when(data.getNodeType()).thenReturn(qName);
        when(data.getIdentifier()).thenReturn(identWithPredicates);

        return new NormalizedNodeContext(iiCx, data);
    }
}
