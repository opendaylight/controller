/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.Address;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RemoteRpcRegistryMXBeanImplTest {

    private static final QName QNAME = QName.create("base", "local");
    private static final SchemaPath SCHEMA_PATH = SchemaPath.create(true, QNAME);
    private static final String VERSION = "version";
    private static final Map<String, Long> VERSIONS = ImmutableMap.of(VERSION, 1L);

    private static final Address ADDRESS = Address.apply("http", "local");
    private RemoteRpcRegistryMXBeanImpl mxBean;
    @Mock
    private RpcRegistry rpcRegistry;
    @Mock
    private RoutingTable table;
    @Mock
    private DOMRpcIdentifier rpcIdentifier;
    @Mock
    private DOMRpcIdentifier emptyRpcIdentifier;
    @Mock
    private Bucket<RoutingTable> bucket;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mxBean = new RemoteRpcRegistryMXBeanImpl(rpcRegistry);
        Mockito.doReturn(table).when(rpcRegistry).getLocalData();
        Mockito.doReturn(SCHEMA_PATH).when(rpcIdentifier).getType();
        Mockito.doReturn(Sets.newHashSet(rpcIdentifier)).when(table).getRoutes();

        Mockito.doReturn(YangInstanceIdentifier.EMPTY).when(emptyRpcIdentifier).getContextReference();
        Mockito.doReturn(YangInstanceIdentifier.builder(YangInstanceIdentifier.of(QNAME)).build())
                .when(rpcIdentifier).getContextReference();

        Mockito.doReturn(table).when(bucket).getData();
        final Map<Address, Bucket<RoutingTable>> buckets = ImmutableMap.of(ADDRESS, bucket);

        Mockito.doReturn(buckets).when(rpcRegistry).getRemoteBuckets();
        Mockito.doReturn(VERSIONS).when(rpcRegistry).getVersions();
    }

    @Test
    public void testGetGlobalRpc() throws Exception {
        final Set<String> globalRpc = mxBean.getGlobalRpc();
        Assert.assertNotNull(globalRpc);
        Assert.assertEquals(1, globalRpc.size());
        final String rpc = globalRpc.iterator().next();
        Assert.assertEquals(SCHEMA_PATH.toString(), rpc);
    }

    @Test
    public void testGetLocalRegisteredRoutedRpc() throws Exception {
        final Set<String> localRegisteredRoutedRpc = mxBean.getLocalRegisteredRoutedRpc();
        Assert.assertNotNull(localRegisteredRoutedRpc);
        Assert.assertEquals(1, localRegisteredRoutedRpc.size());
        final String localRpc = localRegisteredRoutedRpc.iterator().next();
        Assert.assertEquals("", localRpc);
    }

    @Test
    public void testFindRpcByName() throws Exception {
        final String key = "";
        final Map<String, String> rpcByName = mxBean.findRpcByName("");
        Assert.assertEquals("", rpcByName.get(""));
    }

    @Test
    public void testFindRpcByRoute() throws Exception {
        final String key ="";
        final Map<String, String> rpcByRoute = mxBean.findRpcByRoute("");
        Assert.assertEquals("http://local", rpcByRoute.get("route:/(base)local | name:AbsoluteSchemaPath{path=[(base)localName]}"));
    }

    @Test
    public void testGetBucketVersions() throws Exception {
        final String bucketVersions = mxBean.getBucketVersions();
        Assert.assertEquals(VERSIONS.toString(), bucketVersions);
    }
}