/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.schema;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

public class NetconfRemoteSchemaYangSourceProviderTest {

    @Mock
    private RemoteDeviceId deviceId;
    @Mock
    private RpcImplementation rpcImplementation;
    @Mock
    private ListenableFuture listenableFuture;
    @Mock
    private RpcResult rpcResult;
    @Mock
    private CompositeNode compositeNode;
    @Mock
    private SimpleNode simpleNode;

    private SourceIdentifier sourceIdentifier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(listenableFuture).when(rpcImplementation).invokeRpc(any(QName.class), any(CompositeNode.class));
        doNothing().when(listenableFuture).addListener(any(Runnable.class), any(Executor.class));
        doReturn(compositeNode).when(rpcResult).getResult();
        doReturn(Sets.newHashSet()).when(rpcResult).getErrors();
        doReturn("devId").when(deviceId).toString();
        doReturn(simpleNode).when(compositeNode).getFirstSimpleByName(any(QName.class));
        doReturn(compositeNode).when(compositeNode).getFirstCompositeByName(any(QName.class));
        doReturn("asd").when(simpleNode).getValue();
        sourceIdentifier = new SourceIdentifier("source", "rev");
    }

    @Test
    public void testGetSource() throws Exception {
        NetconfRemoteSchemaYangSourceProvider sourceProvider = new NetconfRemoteSchemaYangSourceProvider(deviceId, rpcImplementation);
        assertNotNull( sourceProvider.getSourceChecked(sourceIdentifier));
    }

    @Test
    public void testResultToYangSourceTransformer () throws Exception {
        doReturn(true).when(rpcResult).isSuccessful();
        NetconfRemoteSchemaYangSourceProvider.ResultToYangSourceTransformer transformer =
                new NetconfRemoteSchemaYangSourceProvider.ResultToYangSourceTransformer(deviceId, sourceIdentifier, "module", Optional.of("revision"));
        assertNotNull(transformer.apply(rpcResult));
    }

    @Test(expected = IllegalStateException.class)
    public void testResultToYangSourceTransformerException() throws Exception {
        doReturn(false).when(rpcResult).isSuccessful();
        NetconfRemoteSchemaYangSourceProvider.ResultToYangSourceTransformer transformer =
                new NetconfRemoteSchemaYangSourceProvider.ResultToYangSourceTransformer(deviceId, sourceIdentifier, "module", Optional.of("revision"));
        transformer.apply(rpcResult);
    }

    @Test
    public void testNetconfYangTextSchemaSource() throws Exception {
        NetconfRemoteSchemaYangSourceProvider.NetconfYangTextSchemaSource schemaSource= new
                NetconfRemoteSchemaYangSourceProvider.NetconfYangTextSchemaSource(deviceId, sourceIdentifier, Optional.of("ref"));

        Objects.ToStringHelper stringHelper = Objects.toStringHelper("class");
        assertNotNull(schemaSource.addToStringAttributes(stringHelper));
        assertNotNull(schemaSource.openStream());
    }
}
