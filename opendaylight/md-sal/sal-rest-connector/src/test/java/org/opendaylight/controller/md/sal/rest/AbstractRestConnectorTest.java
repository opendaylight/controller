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
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
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
}
