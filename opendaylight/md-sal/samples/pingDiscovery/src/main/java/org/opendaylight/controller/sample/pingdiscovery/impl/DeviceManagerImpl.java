/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sample.pingdiscovery.impl;

import java.util.Collections;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sample.pingdiscovery.DeviceManager;
import org.opendaylight.controller.sample.pingdiscovery.DeviceMountHandler;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.device.ip.rev140515.Node1;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.device.ip.rev140515.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceManagerImpl implements DeviceManager {

    private final Logger log = LoggerFactory.getLogger( DeviceManagerImpl.class );
    private DataProviderService dataBrokerService;
    private DeviceMountHandler rpcProvider;

    public void setDataBrokerService(DataProviderService dataBrokerService) {
        this.dataBrokerService = dataBrokerService;
    }

    public void setRpcProvider(DeviceMountHandler rpcProvider) {
        this.rpcProvider = rpcProvider;
    }

    @Override
    public boolean createDevice( final String ipAddrStr) {

        boolean retval = false ;

        InstanceIdentifier<Nodes> path = InstanceIdentifier
                .builder(Nodes.class).build();

        // InstanceIdentifier<Nodes>.builder(INVENTORY_PATH)
        // .nodeWithKey(INVENTORY_NODE,
        // Collections.<QName, Object>singletonMap(INVENTORY_ID, "DevinsDevice"
        // )).toInstance();

        String nodeIdStr = "ping_" + ipAddrStr;

        if( writeOperationalData(ipAddrStr, path, nodeIdStr) )
        {
            retval = true;
        }

        return retval ;
    }

    private boolean writeOperationalData(final String ipAddrStr, InstanceIdentifier<Nodes> path,
            String nodeIdStr) {
        NodeId nodeId = new NodeId( nodeIdStr );
        NodeKey nodeKey = new NodeKey(nodeId);

        IpAddress addr = IpAddressBuilder.getDefaultInstance( ipAddrStr );
        Node1 ipAddrNode = new Node1Builder().setIpaddress( addr ).build();
        Node node = new NodeBuilder()
                                .setId(nodeId)
                                .setKey(nodeKey)
                                .addAugmentation( Node1.class, ipAddrNode )
                                .build();

        Nodes nodes = new NodesBuilder().setNode(
                Collections.singletonList(node)).build();

        DataObject dObj = dataBrokerService.readConfigurationData(createPath(nodeIdStr));
        if (dObj == null) {

            DataModificationTransaction txn = dataBrokerService.beginTransaction();

            txn.putConfigurationData(path, nodes);
            txn.putOperationalData(path, nodes);

            try {
                Future<RpcResult<TransactionStatus>> commit = txn.commit();
                RpcResult<TransactionStatus> rpcResult = commit.get();
                TransactionStatus result = rpcResult.getResult();

                if( result.equals( TransactionStatus.FAILED ) ){
                    return false;
                }
            } catch (Exception e) {
                if( log.isErrorEnabled() )
                {
                    log.error( "Caught exception while trying to commit "
                            + "update to IP Device discovery.", e );
                }
                return false;
            }

            rpcProvider.mountIcmpDataNode( nodeIdStr );
        }

        return true;
    }

    private InstanceIdentifier<Node> createPath(String nodeIdStr) {
        return InstanceIdentifier.<Nodes>builder(Nodes.class)
                .<Node, NodeKey>child(Node.class, new NodeKey(new NodeId( nodeIdStr ))).toInstance();
    }

}
