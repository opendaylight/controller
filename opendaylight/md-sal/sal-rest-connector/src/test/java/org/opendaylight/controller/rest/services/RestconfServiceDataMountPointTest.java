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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
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
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

@RunWith(MockitoJUnitRunner.class)
public class RestconfServiceDataMountPointTest {
    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture;

    @Mock
    @SuppressWarnings("rawtypes")
    private NormalizedNode mockNormalizedNode;

    @Mock
    private RestBrokerFacade mockDataBroker;

    @Mock
    private YangInstanceIdentifier mockYangII;

    @Mock
    private DOMMountPoint mockDOMMountPoint;

    @Mock
    private CheckedFuture<Boolean, ReadFailedException> mockBoolCheckedFuture;

    @Mock
    private NormalizedNodeContext mockNormalizedNodeContext;

    @Mock
    @SuppressWarnings("rawtypes")
    private InstanceIdentifierContext mockIIC;

    @Mock
    private DOMDataBroker domDataBroker;

    @Mock
    private DOMDataReadWriteTransaction mockRWtx;

    @Mock
    private SchemaNode mockSchemaNode;

    @Mock
    private Optional<DOMDataBroker> domDataBrokerService;

    @Mock
    private DOMMountPointService mockDomMountPointService;

    private SchemaContext schemaCx;
    private RestSchemaController restSchemaCx;
    private RestconfServiceData restDataService;

    final String identifier = "test-interface:interfaces/interface/key";

    private final String key = "key";

    @Before
    public void initialization() throws Exception {
        schemaCx = TestRestconfUtils.loadSchemaContext("/test-config-data/yang1/", schemaCx);

        restSchemaCx = new RestSchemaControllerImpl();
        restSchemaCx.setGlobalSchema(schemaCx);

        restDataService = new RestconfServiceDataImpl(mockDataBroker, restSchemaCx);
    }

    @Test(expected = NullPointerException.class)
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
                mockDataBroker.commitConfigurationDataPost(any(DOMMountPoint.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPost(mockDOMMountPoint, mockYangII, mockNormalizedNode);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPost(any(DOMMountPoint.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void putConfigTest() {
        when(
                mockDataBroker.commitConfigurationDataPut(any(DOMMountPoint.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPut(mockDOMMountPoint, mockYangII, mockNormalizedNode);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPut(any(DOMMountPoint.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void getConfigTest() {
        when(mockDataBroker.readConfigurationData(any(DOMMountPoint.class), any(YangInstanceIdentifier.class)))
                .thenReturn(mockNormalizedNode);
        final NormalizedNode<?, ?> checkedNode = mockDataBroker.readConfigurationData(mockDOMMountPoint, mockYangII);
        Preconditions.checkNotNull(checkedNode);
        assertEquals(mockNormalizedNode, checkedNode);
        verify(mockDataBroker, times(1)).readConfigurationData(any(DOMMountPoint.class),
                any(YangInstanceIdentifier.class));
    }

    @Test
    public void getOperationalTest() {
        when(mockDataBroker.readOperationalData(any(DOMMountPoint.class), any(YangInstanceIdentifier.class)))
                .thenReturn(mockNormalizedNode);
        final NormalizedNode<?, ?> checkedNode = mockDataBroker.readOperationalData(mockDOMMountPoint, mockYangII);
        Preconditions.checkNotNull(checkedNode);
        assertEquals(mockNormalizedNode, checkedNode);
        verify(mockDataBroker, times(1)).readOperationalData(any(DOMMountPoint.class),
                any(YangInstanceIdentifier.class));
    }

    @Test
    public void deleteConfigTest() {
        when(mockDataBroker.commitConfigurationDataDelete(any(DOMMountPoint.class), any(YangInstanceIdentifier.class)))
                .thenReturn(
                mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataDelete(mockDOMMountPoint, mockYangII);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(mockCheckedFuture, checkedFuture);
        verify(mockDataBroker, times(1)).commitConfigurationDataDelete(any(DOMMountPoint.class),
                any(YangInstanceIdentifier.class));
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
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures
                .immediateCheckedFuture(null);

        final NormalizedNodeContext nn = prepareNn(identifier, key);
        final QName qname = nn.getData().getNodeType();
        final YangInstanceIdentifier yangIIC = nn.getInstanceIdentifierContext().getInstanceIdentifier();

        when(mockNormalizedNodeContext.getData()).thenReturn(mockNormalizedNode);
        when(mockNormalizedNode.getNodeType()).thenReturn(qname);
        when(mockNormalizedNodeContext.getInstanceIdentifierContext()).thenReturn(mockIIC);
        when(mockIIC.getInstanceIdentifier()).thenReturn(yangIIC);
        when(mockIIC.getMountPoint()).thenReturn(mockDOMMountPoint);
        when(mockIIC.getSchemaNode()).thenReturn(mockSchemaNode);
        when(
                mockDataBroker.commitConfigurationDataPatch(any(DOMMountPoint.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(checkedFuture);

        final Response respOrig = restDataService.patchConfigurationData(identifier, mockNormalizedNodeContext);
        assertEquals(200, respOrig.getStatus());
    }

    /**
     * test for PATCH method
     */
    @Test
    public void patchConfigTest() {
        when(
                mockDataBroker.commitConfigurationDataPatch(any(DOMMountPoint.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPatch(mockDOMMountPoint, mockYangII, mockNormalizedNode);
        Preconditions.checkNotNull(checkedFuture);
        assertEquals(checkedFuture, mockCheckedFuture);

        verify(mockDataBroker, times(1)).commitConfigurationDataPatch(any(DOMMountPoint.class),
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
        final InstanceIdentifierContext<?> iiCx = restSchemaCx.toMountPointIdentifier(identifier);
        final MapEntryNode data = mock(MapEntryNode.class);
        final QName qName = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "interface");
        final QName qNameKey = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "name");
        final NodeIdentifierWithPredicates identWithPredicates = new NodeIdentifierWithPredicates(qName, qNameKey, key);

        when(data.getNodeType()).thenReturn(qName);
        when(data.getIdentifier()).thenReturn(identWithPredicates);

        return new NormalizedNodeContext(iiCx, data);
    }
}
