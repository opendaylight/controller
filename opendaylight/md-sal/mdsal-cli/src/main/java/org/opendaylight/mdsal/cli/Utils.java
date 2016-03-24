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
import java.util.ArrayList;
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

    public static void printBeanAttribute(MBeanServer server, ObjectName objName, String title, String attrName,
            String valueToStart) throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        System.out.println("::" + title + "::");
        MBeanInfo beanInfo = server.getMBeanInfo(objName);
        MBeanAttributeInfo[] attrInfo = beanInfo.getAttributes();
        for (MBeanAttributeInfo att : attrInfo) {
            if(att.getName().equals(attrName)) {
                if (valueToStart.isEmpty()) {
                    System.out.println(server.getAttribute(objName, att.getName()));
                } else {
                    Object value = server.getAttribute(objName, att.getName());
                    if (value instanceof ArrayList) {
                        ArrayList<Object> arrayList = (ArrayList<Object>) value;
                        for (Object objValue : arrayList) {
                            int idx = objValue.toString().indexOf(valueToStart);
                            String objValueStr = objValue.toString().substring(idx + valueToStart.length());
                            System.out.println(objValueStr);
                        }
                    } else {
                        String valueStr = value.toString();
                        if (valueStr.contains(valueToStart)) {
                            int idx = valueStr.indexOf(valueToStart);
                            valueStr = valueStr.substring(idx + valueToStart.length());
                            System.out.println(valueStr);
                        } else {
                            System.out.println("Attribute value does not contain " + valueToStart);
                        }
                    }
                }
            }
        }
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
                    // Check for topology or inventory data tree as they are exist by default in the data store
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
            String currentMemberName = "";
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
                            peer = peer.trim();
                            int idx = peer.indexOf(":");
                            String memberName = peer.substring(0, idx);
                            if (peer.contains("@")) {
                                idx = peer.indexOf("@");
                                String memberTmp = peer.substring(idx);
                                idx = memberTmp.indexOf(":");
                                String memberIP = memberTmp.substring(1, idx);
                                peersMap.put(memberName, memberIP);
                            }
                        }
                    }
                }
                if (att.getName().trim().equals("ShardName")) {
                    String name = server.getAttribute(shardModule, att.getName()).toString();
                    int idxShard = name.indexOf("shard");
                    currentMemberName = name.substring(0, (idxShard -1));
                }
            }
            String leaderIP = peersMap.get(leaderName);
            ShellTable table = new ShellTable();
            table.column("Node Name");
            table.column("Node IP-Address");
            table.column("Raft State");
            if (leaderIP == null || leaderIP.isEmpty()) {
                int idx = leaderName.indexOf("shard");
                table.addRow().addContent(currentMemberName, getHostIp(), "Leader");
                for (String peer : peersMap.keySet()) {
                    table.addRow().addContent(peer.substring(0, idx-1), peersMap.get(peer), "Follower");
                }
            }
            else {
                int idx = leaderName.indexOf("shard");
                table.addRow().addContent(leaderName.substring(0, idx-1), leaderIP, "Leader");
                table.addRow().addContent(currentMemberName, getHostIp(), "Follower");
                for (String peer : peersMap.keySet()) {
                    if (!peer.equals(leaderName))
                        table.addRow().addContent(peer.substring(0, idx-1), peersMap.get(peer), "Follower");
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
