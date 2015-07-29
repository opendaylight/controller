package org.opendaylight.controller.rest.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
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

/**
 * Test class tests {@link RestconfServiceData} class methods for non Mount
 * Point data. POST, PUT, PATCH, DELETE, GET in operational and config DataStore
 */
@RunWith(MockitoJUnitRunner.class)
public class RestconfServiceDataTest {

    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture;

    @Mock
    @SuppressWarnings("rawtypes")
    private NormalizedNode mockNormalizedNode;

    @Mock
    private RestBrokerFacade mockDataBroker;

    @Mock
    private YangInstanceIdentifier mockYangII;

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

    /* Test Methods */

    /**
     * Method tests POST method
     * {@link RestconfServiceData#createConfigurationData(org.opendaylight.controller.rest.common.NormalizedNodeContext, javax.ws.rs.core.UriInfo)}
     * Test create mocked response for createConfigurationData which has to be
     * the same as we set, and test checks nr. of call the mocked
     * {@link RestBrokerFacade#commitConfigurationDataPost(org.opendaylight.yangtools.yang.model.api.SchemaContext, org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * exactly one time
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
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

    @Test
    public void patchConfig_200_OK_ResponseTest() {
     // TODO : implement test
        when(
                mockDataBroker.commitConfigurationDataPatch(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPatch(schemaCx, mockYangII, mockNormalizedNode);
        assertEquals(mockCheckedFuture, checkedFuture);
        verify(mockDataBroker, times(1)).commitConfigurationDataPatch(any(SchemaContext.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void patchConfig_204_NoContent_ResponseTest() {
        when(
                mockDataBroker.commitConfigurationDataPatch(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
                .commitConfigurationDataPatch(schemaCx, mockYangII, null);
        assertEquals(mockCheckedFuture, checkedFuture);
        verify(mockDataBroker, times(1)).commitConfigurationDataPatch(any(SchemaContext.class),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void patchConfigTest() {
        when(
                mockDataBroker.commitConfigurationDataPatch(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(mockCheckedFuture);
//        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mockDataBroker
//                .commitConfigurationDataPut(schemaCx, mockYangII, mockNormalizedNode);
//        Preconditions.checkNotNull(checkedFuture);
//        assertEquals(checkedFuture, mockCheckedFuture);
//
//        verify(mockDataBroker, times(1)).commitConfigurationDataPatch(any(SchemaContext.class),
//                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }
}
