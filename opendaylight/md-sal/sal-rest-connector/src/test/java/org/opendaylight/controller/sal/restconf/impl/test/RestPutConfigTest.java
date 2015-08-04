/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.IOException;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

/**
 * sal-rest-connector
 * org.opendaylight.controller.sal.restconf.impl.test
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: May 14, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RestPutConfigTest {

    private RestconfImpl restconfService;
    private ControllerContext controllerCx;
    private SchemaContext schemaCx;

    @Mock
    private BrokerFacade brokerFacade;

    @Before
    public void init() throws IOException, ReactorException {
        restconfService = RestconfImpl.getInstance();
        controllerCx = ControllerContext.getInstance();
        schemaCx = TestRestconfUtils.loadSchemaContext("/test-config-data/yang1/", null);
        controllerCx.setSchemas(schemaCx);
        restconfService.setControllerContext(controllerCx);
    }

    @Test
    public void testPutConfigData() {
        final String identifier = "test-interface:interfaces/interface/key";
        final InstanceIdentifierContext<?> iiCx = controllerCx.toInstanceIdentifier(identifier);
        final MapEntryNode data = Mockito.mock(MapEntryNode.class);
        final QName qName = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "interface");
        final QName qNameKey = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "name");
        final NodeIdentifierWithPredicates identWithPredicates = new NodeIdentifierWithPredicates(qName, qNameKey, "key");
        Mockito.when(data.getNodeType()).thenReturn(qName);
        Mockito.when(data.getIdentifier()).thenReturn(identWithPredicates);
        final NormalizedNodeContext payload = new NormalizedNodeContext(iiCx, data);

        mockingBrokerPut(iiCx.getInstanceIdentifier(), data);

        restconfService.updateConfigurationData(identifier, payload);
    }

    @Test
    public void testPutConfigDataCheckOnlyLastElement() {
        final String identifier = "test-interface:interfaces/interface/key/sub-interface/subkey";
        final InstanceIdentifierContext<?> iiCx = controllerCx.toInstanceIdentifier(identifier);
        final MapEntryNode data = Mockito.mock(MapEntryNode.class);
        final QName qName = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "sub-interface");
        final QName qNameSubKey = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "sub-name");
        final NodeIdentifierWithPredicates identWithPredicates = new NodeIdentifierWithPredicates(qName, qNameSubKey, "subkey");
        Mockito.when(data.getNodeType()).thenReturn(qName);
        Mockito.when(data.getIdentifier()).thenReturn(identWithPredicates);
        final NormalizedNodeContext payload = new NormalizedNodeContext(iiCx, data);

        mockingBrokerPut(iiCx.getInstanceIdentifier(), data);

        restconfService.updateConfigurationData(identifier, payload);
    }

    @Test(expected=RestconfDocumentedException.class)
    public void testPutConfigDataMissingUriKey() {
        final String identifier = "test-interface:interfaces/interface";
        controllerCx.toInstanceIdentifier(identifier);
    }

    @Test(expected=RestconfDocumentedException.class)
    public void testPutConfigDataDiferentKey() {
        final String identifier = "test-interface:interfaces/interface/key";
        final InstanceIdentifierContext<?> iiCx = controllerCx.toInstanceIdentifier(identifier);
        final MapEntryNode data = Mockito.mock(MapEntryNode.class);
        final QName qName = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "interface");
        final QName qNameKey = QName.create("urn:ietf:params:xml:ns:yang:test-interface", "2014-07-01", "name");
        final NodeIdentifierWithPredicates identWithPredicates = new NodeIdentifierWithPredicates(qName, qNameKey, "notSameKey");
        Mockito.when(data.getNodeType()).thenReturn(qName);
        Mockito.when(data.getIdentifier()).thenReturn(identWithPredicates);
        final NormalizedNodeContext payload = new NormalizedNodeContext(iiCx, data);

        mockingBrokerPut(iiCx.getInstanceIdentifier(), data);

        restconfService.updateConfigurationData(identifier, payload);
    }

    private void mockingBrokerPut(final YangInstanceIdentifier yii, final NormalizedNode<?, ?> data) {
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures.immediateCheckedFuture(null);
        Mockito.when(brokerFacade.commitConfigurationDataPut(schemaCx, yii, data)).thenReturn(checkedFuture);
        restconfService.setBroker(brokerFacade);
    }
}
