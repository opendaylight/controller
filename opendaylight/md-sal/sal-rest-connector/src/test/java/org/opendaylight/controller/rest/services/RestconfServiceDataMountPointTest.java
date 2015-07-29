package org.opendaylight.controller.rest.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.connector.impl.RestSchemaControllerImpl;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;
import org.opendaylight.controller.rest.test.utils.TestRestconfUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

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

    private SchemaContext schemaCx;
    private RestSchemaController restSchemaCx;
    private RestconfServiceData restDataService;

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

}
