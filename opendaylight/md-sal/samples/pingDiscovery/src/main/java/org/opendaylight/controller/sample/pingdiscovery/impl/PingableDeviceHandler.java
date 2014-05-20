/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.pingdiscovery.impl;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sample.pingdiscovery.PingService;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.device.ip.rev140515.Node1;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.Icmpdata;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.IcmpdataBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.SendPingNowOutput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.SendPingNowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class PingableDeviceHandler implements RpcImplementation,
        DataReader<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {
    private final Logger log = LoggerFactory.getLogger(PingableDeviceHandler.class);

    private DataBrokerService dataBrokerService;

    private final InstanceIdentifier<Node> nodePath;

    private BindingIndependentMappingService mappingService;

    public PingableDeviceHandler(String nodeId) {
        nodePath = createPath(nodeId);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return Collections.emptySet(); // why does this work?
    }

    public void setDataBrokerService(DataBrokerService dataBrokerService) {
        this.dataBrokerService = dataBrokerService;
    }

    @Override
    public CompositeNode readConfigurationData(
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path) {

        // This is where you put your configuration data handling!
        return null;
    }

    @Override
    public CompositeNode readOperationalData(
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path) {
        PathArgument pathArgument = path.getPath().get(0);
        QName nodeType = pathArgument.getNodeType();
        if (nodeType.getNamespace().toString().equals("http://opendaylight.org/samples/icmp/data")
                && nodeType.getLocalName().equals("icmpdata")) {
            double pingTime = readIPAndPing();
            Icmpdata output = new IcmpdataBuilder().setIsAvailable(
                    (pingTime != PingService.NOT_FOUND)).build();
            return mappingService.toDataDom(output);
        }
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
        log.info("Invoke RPC: " + rpc + " on nodePath " + nodePath);

        double pingTime = readIPAndPing();
        SendPingNowOutput output = new SendPingNowOutputBuilder().setRtt(Math.round(pingTime))
                .build();

        return Futures.immediateFuture(Rpcs.getRpcResult(true, mappingService.toDataDom(output),
                Collections.<RpcError> emptyList()));
    }

    private double readIPAndPing() {
        String ip = null;
        double pingTime = PingService.NOT_FOUND;
        try {
            ip = readIPAddress();
            pingTime = PingService.ping(ip);
        } catch (Exception e) {
            log.error("Exception while pinging ip: " + ip, e);
        }
        return pingTime;
    }

    private String readIPAddress() {
        String ip;
        Node operationalData = (Node) dataBrokerService.readOperationalData(nodePath);
        Node1 ipData = operationalData.getAugmentation(Node1.class);
        IpAddress ipaddress = ipData.getIpaddress();

        ip = PingService.getIPAddressAsString(ipaddress);
        return ip;
    }

    private InstanceIdentifier<Node> createPath(String nodeIdStr) {
        return InstanceIdentifier.<Nodes> builder(Nodes.class)
                .<Node, NodeKey> child(Node.class, new NodeKey(new NodeId(nodeIdStr))).toInstance();
    }

    public void setMappingService(BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }

}