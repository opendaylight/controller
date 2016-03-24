/*
 * Copyright (c) 2016 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.cli;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.karaf.shell.table.ShellTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static void printBeanTable(MBeanServer server, ObjectName objName, String title) throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        ShellTable table = new ShellTable();
        System.out.println("::" + title + "::");
        table.column("Name");
        table.column("Value");
        MBeanInfo beanInfo = server.getMBeanInfo(objName);
        MBeanAttributeInfo[] attrInfo = beanInfo.getAttributes();
        for (MBeanAttributeInfo att : attrInfo) {
            table.addRow().addContent(splitCapitalLetter(att.getName()), server.getAttribute(objName, att.getName()));
        }
        table.print(System.out);
        System.out.println("");
    }

    public static void printClusterNodes(MBeanServer server, ObjectName objName) throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException, MalformedObjectNameException {
        MBeanInfo beanInfo = server.getMBeanInfo(objName);
        MBeanAttributeInfo[] attrInfo = beanInfo.getAttributes();
        String shardName = "";
        for (MBeanAttributeInfo att : attrInfo) {
            if (att.getName().trim().equals("LocalShards")) {
                String localShard = server.getAttribute(objName, att.getName()).toString();
                String[] shards = localShard.split(",");
                for (String shard : shards) {
                    if (shard.contains("shard-topology-operational") || shard.contains("shard-inventory-operational")) {
                        shardName = shard;
                        break;
                    }
                }
                break;
            }
        }
        if (!shardName.isEmpty()) {
            Map<String, String> peersMap = new HashMap<String, String>();
            String leaderName = "";
            ObjectName shardModule = new ObjectName("org.opendaylight.controller:type=DistributedOperationalDatastore,Category=Shards,name=" + shardName.trim());
            MBeanInfo shardModuleBeanInfo = server.getMBeanInfo(shardModule);
            MBeanAttributeInfo[] shardModuleAttrInfo = shardModuleBeanInfo.getAttributes();
            for (MBeanAttributeInfo att : shardModuleAttrInfo) {
                if (att.getName().trim().equals("Leader")) {
                    leaderName = server.getAttribute(shardModule, att.getName()).toString();
                }
                if (att.getName().trim().equals("PeerAddresses")) {
                    String peerAddresses = server.getAttribute(shardModule, att.getName()).toString();
                    if (peerAddresses != null && !peerAddresses.isEmpty()) {
                        String[] peers = peerAddresses.split(",");
                        for (String peer : peers) {
                            int idx = peer.indexOf(":");
                            String memberName = peer.substring(0, idx);
                            idx = peer.indexOf("@");
                            String memberTmp = peer.substring(idx);
                            idx = memberTmp.indexOf(":");
                            String memberIP = memberTmp.substring(1, idx);
                            peersMap.put(memberName, memberIP);
                        }
                    }
                }
            }
            String leaderIP = peersMap.get(leaderName);
            ShellTable table = new ShellTable();
            table.column("Node IP-Address");
            table.column("Raft State");
            if (leaderIP == null || leaderIP.isEmpty()) {
                table.addRow().addContent(getHostIp(), "Leader");
                for (String peer : peersMap.values()) {
                    table.addRow().addContent(peer, "Follower");
                }
            }
            else {
                table.addRow().addContent(leaderIP, "Leader");
                table.addRow().addContent(getHostIp(), "Follower");
                for (String peer : peersMap.values()) {
                    if (!peer.equals(leaderIP))
                        table.addRow().addContent(peer, "Follower");
                }
            }
            table.print(System.out);
        } else {
            System.out.println("There is no Cluster Nodes, Single Opendaylight Controller.");
        }
    }

    public static String getHostIp() {
        try {
            String hostIP = InetAddress.getLocalHost().getHostAddress();
            if (!(hostIP.equals("127.0.0.1") || hostIP.equals("127.0.1.1"))) {
                return hostIP;
            }
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while(nias.hasMoreElements()) {
                    InetAddress ia= (InetAddress) nias.nextElement();
                    if (ia instanceof Inet4Address) {
                        hostIP = ia.getHostAddress();
                        if (!(hostIP.equals("127.0.0.1") || hostIP.equals("127.0.1.1"))) {
                            return hostIP;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("unable to get current IP ", e);
        }
        return "127.0.0.1";
    }

    private static String splitCapitalLetter(String field) {
        String[] strArr = field.split("(?=\\p{Lu})");
        String temp = "";
        for (String str : strArr) {
            temp += str + " ";
        }
        return temp;
    }
}
