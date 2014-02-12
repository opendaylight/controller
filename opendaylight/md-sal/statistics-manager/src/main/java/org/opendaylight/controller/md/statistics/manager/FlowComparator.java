/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Utility class for comparing flows.
 */
final class FlowComparator {
    private final static Logger logger = LoggerFactory.getLogger(FlowComparator.class);

    private FlowComparator() {

    }

    public static boolean flowEquals(Flow statsFlow, Flow storedFlow) {
        if (statsFlow.getClass() != storedFlow.getClass()) {
            return false;
        }
        if (statsFlow.getContainerName()== null) {
            if (storedFlow.getContainerName()!= null) {
                return false;
            }
        } else if(!statsFlow.getContainerName().equals(storedFlow.getContainerName())) {
            return false;
        }
        if (statsFlow.getMatch()== null) {
            if (storedFlow.getMatch() != null) {
                return false;
            }
        } //else if(!statsFlow.getMatch().equals(storedFlow.getMatch())) {
        else if(!matchEquals(statsFlow.getMatch(), storedFlow.getMatch())) {
            return false;
        }
        if (storedFlow.getPriority() == null) {
            if (statsFlow.getPriority() != null && statsFlow.getPriority()!= 0x8000) {
                return false;
            }
        } else if(!statsFlow.getPriority().equals(storedFlow.getPriority())) {
            return false;
        }
        if (statsFlow.getTableId() == null) {
            if (storedFlow.getTableId() != null) {
                return false;
            }
        } else if(!statsFlow.getTableId().equals(storedFlow.getTableId())) {
            return false;
        }
        return true;
    }

    /**
     * Explicit equals method to compare the 'match' for flows stored in the data-stores and flow fetched from the switch.
     * Flow installation process has three steps
     * 1) Store flow in config data store
     * 2) and send it to plugin for installation
     * 3) Flow gets installed in switch
     *
     * The flow user wants to install and what finally gets installed in switch can be slightly different.
     * E.g, If user installs flow with src/dst ip=10.0.0.1/24, when it get installed in the switch
     * src/dst ip will be changes to 10.0.0.0/24 because of netmask of 24. When statistics manager fetch
     * stats it gets 10.0.0.0/24 rather then 10.0.0.1/24. Custom match takes care of by using masked ip
     * while comparing two ip addresses.
     *
     * Sometimes when user don't provide few values that is required by flow installation request, like
     * priority,hard timeout, idle timeout, cookies etc, plugin usages default values before sending
     * request to the switch. So when statistics manager gets flow statistics, it gets the default value.
     * But the flow stored in config data store don't have those defaults value. I included those checks
     * in the customer flow/match equal function.
     *
     *
     * @param statsFlow
     * @param storedFlow
     * @return
     */
    public static boolean matchEquals(Match statsFlow, Match storedFlow) {
        if (statsFlow == storedFlow) {
            return true;
        }
        if (storedFlow.getClass() != statsFlow.getClass()) {
            return false;
        }
        if (storedFlow.getEthernetMatch() == null) {
            if (statsFlow.getEthernetMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getEthernetMatch().equals(statsFlow.getEthernetMatch())) {
            return false;
        }
        if (storedFlow.getIcmpv4Match()== null) {
            if (statsFlow.getIcmpv4Match() != null) {
                return false;
            }
        } else if(!storedFlow.getIcmpv4Match().equals(statsFlow.getIcmpv4Match())) {
            return false;
        }
        if (storedFlow.getIcmpv6Match() == null) {
            if (statsFlow.getIcmpv6Match() != null) {
                return false;
            }
        } else if(!storedFlow.getIcmpv6Match().equals(statsFlow.getIcmpv6Match())) {
            return false;
        }
        if (storedFlow.getInPhyPort() == null) {
            if (statsFlow.getInPhyPort() != null) {
                return false;
            }
        } else if(!storedFlow.getInPhyPort().equals(statsFlow.getInPhyPort())) {
            return false;
        }
        if (storedFlow.getInPort()== null) {
            if (statsFlow.getInPort() != null) {
                return false;
            }
        } else if(!storedFlow.getInPort().equals(statsFlow.getInPort())) {
            return false;
        }
        if (storedFlow.getIpMatch()== null) {
            if (statsFlow.getIpMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getIpMatch().equals(statsFlow.getIpMatch())) {
            return false;
        }
        if (storedFlow.getLayer3Match()== null) {
            if (statsFlow.getLayer3Match() != null) {
                    return false;
            }
        } else if(!layer3MatchEquals(statsFlow.getLayer3Match(),storedFlow.getLayer3Match())) {
            return false;
        }
        if (storedFlow.getLayer4Match()== null) {
            if (statsFlow.getLayer4Match() != null) {
                return false;
            }
        } else if(!storedFlow.getLayer4Match().equals(statsFlow.getLayer4Match())) {
            return false;
        }
        if (storedFlow.getMetadata() == null) {
            if (statsFlow.getMetadata() != null) {
                return false;
            }
        } else if(!storedFlow.getMetadata().equals(statsFlow.getMetadata())) {
            return false;
        }
        if (storedFlow.getProtocolMatchFields() == null) {
            if (statsFlow.getProtocolMatchFields() != null) {
                return false;
            }
        } else if(!storedFlow.getProtocolMatchFields().equals(statsFlow.getProtocolMatchFields())) {
            return false;
        }
        if (storedFlow.getTunnel()== null) {
            if (statsFlow.getTunnel() != null) {
                return false;
            }
        } else if(!storedFlow.getTunnel().equals(statsFlow.getTunnel())) {
            return false;
        }
        if (storedFlow.getVlanMatch()== null) {
            if (statsFlow.getVlanMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getVlanMatch().equals(statsFlow.getVlanMatch())) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static boolean layer3MatchEquals(Layer3Match statsLayer3Match, Layer3Match storedLayer3Match){
        boolean verdict = true;
        if(statsLayer3Match instanceof Ipv4Match && storedLayer3Match instanceof Ipv4Match){
            Ipv4Match statsIpv4Match = (Ipv4Match)statsLayer3Match;
            Ipv4Match storedIpv4Match = (Ipv4Match)storedLayer3Match;

            if (verdict) {
                verdict = compareNullSafe(
                        storedIpv4Match.getIpv4Destination(), statsIpv4Match.getIpv4Destination());
            }
            if (verdict) {
                verdict = compareNullSafe(
                        statsIpv4Match.getIpv4Source(), storedIpv4Match.getIpv4Source());
            }
        } else {
            Boolean nullCheckOut = checkNullValues(storedLayer3Match, statsLayer3Match);
            if (nullCheckOut != null) {
                verdict = nullCheckOut;
            } else {
                verdict = storedLayer3Match.equals(statsLayer3Match);
            }
        }

        return verdict;
    }

    private static boolean compareNullSafe(Ipv4Prefix statsIpv4, Ipv4Prefix storedIpv4) {
        boolean verdict = true;
        Boolean checkDestNullValuesOut = checkNullValues(storedIpv4, statsIpv4);
        if (checkDestNullValuesOut != null) {
            verdict = checkDestNullValuesOut;
        } else if(!IpAddressEquals(statsIpv4, storedIpv4)){
            verdict = false;
        }

        return verdict;
    }

    private static Boolean checkNullValues(Object v1, Object v2) {
        Boolean verdict = null;
        if (v1 == null && v2 != null) {
            verdict = Boolean.FALSE;
        } else if (v1 != null && v2 == null) {
            verdict = Boolean.FALSE;
        } else if (v1 == null && v2 == null) {
            verdict = Boolean.TRUE;
        }

        return verdict;
    }

    /**
     * TODO: why don't we use the default Ipv4Prefix.equals()?
     *
     * @param statsIpAddress
     * @param storedIpAddress
     * @return true if IPv4prefixes equals
     */
    private static boolean IpAddressEquals(Ipv4Prefix statsIpAddress, Ipv4Prefix storedIpAddress) {
        IntegerIpAddress statsIpAddressInt = StrIpToIntIp(statsIpAddress.getValue());
        IntegerIpAddress storedIpAddressInt = StrIpToIntIp(storedIpAddress.getValue());

        if(IpAndMaskBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }
        if(IpBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }
        return false;
    }

    private static boolean IpAndMaskBasedMatch(IntegerIpAddress statsIpAddressInt,IntegerIpAddress storedIpAddressInt){
        return ((statsIpAddressInt.getIp() & statsIpAddressInt.getMask()) ==  (storedIpAddressInt.getIp() & storedIpAddressInt.getMask()));
    }

    private static boolean IpBasedMatch(IntegerIpAddress statsIpAddressInt,IntegerIpAddress storedIpAddressInt){
        return (statsIpAddressInt.getIp() == storedIpAddressInt.getIp());
    }

    /**
     * Method return integer version of ip address. Converted int will be mask if
     * mask specified
     */
    private static IntegerIpAddress StrIpToIntIp(String ipAddresss){

        String[] parts = ipAddresss.split("/");
        String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 32;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        IntegerIpAddress integerIpAddress = null;
        try {
            Inet4Address addr = (Inet4Address) InetAddress.getByName(ip);
            byte[] addrBytes = addr.getAddress();
            int ipInt = ((addrBytes[0] & 0xFF) << 24) |
                    ((addrBytes[1] & 0xFF) << 16) |
                    ((addrBytes[2] & 0xFF) << 8)  |
                    ((addrBytes[3] & 0xFF) << 0);

            int mask = 0xffffffff << 32 - prefix;

            integerIpAddress = new IntegerIpAddress(ipInt, mask);
        } catch (UnknownHostException e){
            logger.error("Failed to determine host IP address by name: {}", e.getMessage(), e);
        }

        return integerIpAddress;
    }

    private static class IntegerIpAddress{
        int ip;
        int mask;
        public IntegerIpAddress(int ip, int mask) {
            this.ip = ip;
            this.mask = mask;
        }
        public int getIp() {
            return ip;
        }
        public int getMask() {
            return mask;
        }
    }
}
