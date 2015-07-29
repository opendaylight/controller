package org.opendaylight.controller.rest.services;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.services.impl.RestconfServiceDataImpl;

/**
 * Test class tests {@link RestconfServiceData} class methods for non Mount
 * Point data. POST, PUT, PATCH, DELETE in operational and config DataStore
 */
public class RestconfServiceDataTest {

    private RestBrokerFacade mockDataBroker;
    private RestSchemaController schemaCx;
    private RestconfServiceData restDataService;

    @Before
    public void initialization() {
        mockDataBroker = mock(RestBrokerFacade.class);
        // / TODO Mocking dataBroker + load some test schemaCx
        restDataService = new RestconfServiceDataImpl(mockDataBroker, schemaCx);
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
        new RestconfServiceDataImpl(null, schemaCx);
    }

    /* Test Methods */

    /**
     * Method tests POST method
     * {@link RestconfServiceData#createConfigurationData(org.opendaylight.controller.rest.common.NormalizedNodeContext, javax.ws.rs.core.UriInfo)}
     * Test create mocked response for createConfigurationData which has to be
     * the same as we set, and test checks nr. of call the mocked
     * {@link RestBrokerFacade#commitConfigurationDataPost(org.opendaylight.yangtools.yang.model.api.SchemaContext, org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode)}
     * exactly one time
     */
    @Test
    public void createPostTest() {

    }

}
