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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;



















//import java.awt.List;
import java.util.List;
import java.io.FileNotFoundException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

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
    


    @Test(expected = RestconfDocumentedException.class)
    public void testSubscribeToStreamWithNullOrEmptySreamName() throws Exception {
        restconfImpl.subscribeToStream("identifier", null);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testSubscribeToStreamWithNullListener() throws Exception {
        restconfImpl.subscribeToStream(null, null);
    }

    @Test
    public void testSubscribeToStreamWithMissingDatastoreInUri() throws Exception {
        String identifier = "streamName";
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier);        
        try{
            restconfImpl.subscribeToStream(identifier, null);
        }
        catch(RestconfDocumentedException e){
            List<RestconfError> errValues = e.getErrors();           
            assertEquals(ErrorType.APPLICATION, errValues.get(0).getErrorType());
            assertEquals(ErrorTag.MISSING_ATTRIBUTE, errValues.get(0).getErrorTag());
            assertEquals("Stream name doesn't contains datastore value (pattern /datastore=)", errValues.get(0).getErrorMessage());
        }
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testSubscribeToStreamWithMissingScope() throws Exception {
        String identifier = "streamName=54/datastore=OPERATIONAL";
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier);        
        try{
            restconfImpl.subscribeToStream(identifier, null);
        }
        catch(RestconfDocumentedException e){
            List<RestconfError> errValues = e.getErrors();           
            assertEquals(ErrorType.APPLICATION, errValues.get(0).getErrorType());
            assertEquals(ErrorTag.MISSING_ATTRIBUTE, errValues.get(0).getErrorTag());
            assertEquals("Stream name doesn't contains datastore value (pattern /scope=)", errValues.get(0).getErrorMessage());
            throw e;
        }
    }

    @Test
    public void testSubscribeToStreamRegisterToLDC() throws Exception {
        String identifier = "streamName=54/datastore=OPERATIONAL/scope=SUBTREE";        
        BrokerFacade bf = BrokerFacade.getInstance();
        Notificator.createListener(YangInstanceIdentifier.builder().node(QName.create("arg0")).build(), identifier);
        restconfImpl.setBroker(bf);
        bf.setContext(mock(ConsumerSession.class));
        DOMDataBroker domDBMock = mock(DOMDataBroker.class);
        bf.setDomDataBroker(domDBMock);
        
        UriInfo ui = mock(UriInfo.class);
        UriBuilder uiPathBuilder = UriBuilder.fromUri(URI.create("http://example.org:4242"));
        when(ui.getAbsolutePathBuilder()).thenReturn(uiPathBuilder );
        
        Response response = restconfImpl.subscribeToStream(identifier, ui);
        
        Mockito.verify(domDBMock, Mockito.times(1)).registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(YangInstanceIdentifier.class),
                any(ListenerAdapter.class),
                any(DataChangeScope.class));
        
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("http://example.org:8181/streamName=54/datastore=OPERATIONAL/scope=SUBTREE", response.getLocation().toString());
    }
    
    @Test(expected = RestconfDocumentedException.class)
    public void testDeleteConfigurationDataErrorCreatingData() throws Exception {
        String identifier = "ietf-yang-types:interfaces-state";
        //ControllerContext ctrlContext = ControllerContext.getInstance();
        ControllerContext ctrlContext = mock(ControllerContext.class);
        InstanceIdentifierContext instIdContext = mock(InstanceIdentifierContext.class);
        when(ctrlContext.toInstanceIdentifier(identifier)).thenReturn(instIdContext);
        restconfImpl.setControllerContext(ctrlContext);
        try{
            restconfImpl.deleteConfigurationData(identifier);     
        }
        catch(RestconfDocumentedException e){
            List<RestconfError> errValues = e.getErrors(); 
            assertEquals("Error creating data", errValues.get(0).getErrorMessage());
            throw e;
        }
    }
    
    @Test(expected = RestconfDocumentedException.class)
    public void testDeleteConfigurationDataMountPointNotNull() throws Exception { //yang-ext:mount
    String identifier = "/ietf-interfaces:interfaces/interface/name";
//    ControllerContext ctrlContext = mock(ControllerContext.class);
//    InstanceIdentifierContext instIdContext = mock(InstanceIdentifierContext.class);
//    when(ctrlContext.toInstanceIdentifier(identifier)).thenReturn(instIdContext);
//    DOMMountPoint mountPoint = mock(DOMMountPoint.class);
//    when(instIdContext.getMountPoint()).thenReturn(mountPoint);
//    SchemaContext schemaCxt = mock(SchemaContext.class);
        //when(domMP.getSchemaContext()).thenReturn(schemaCxt);
            //DataNormalizer datNorm = mock(DataNormalizer.class);
        //QName qn = QName.create("input");
//    QName qname = TestUtils.buildQName("interfaces","test:module", "2014-01-09");
//    YangInstanceIdentifier yangII = YangInstanceIdentifier.of(qname);
        
        
//    when(mountPoint.getSchemaContext()).thenReturn(schemaCxt);
//    when(instIdContext.getInstanceIdentifier()).thenReturn(yangII);
        
        //when(datNorm.toNormalized(instIdContext.getInstanceIdentifier())).thenReturn(yangII);
//    restconfImpl.setControllerContext(ctrlContext);
        restconfImpl.deleteConfigurationData(identifier);
//        try{
//            restconfImpl.deleteConfigurationData(identifier);     
//        }
//        catch(RestconfDocumentedException e){
//            List<RestconfError> errValues = e.getErrors(); 
//            assertEquals("Error creating data", errValues.get(0).getErrorMessage());
//            throw e;
//        }
    }

    @Test
    public void testGetOperationalReceived() throws Exception {
        assertNull(restconfImpl.getOperationalReceived());
    }
}
