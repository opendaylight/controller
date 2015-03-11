/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager.impl.helper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;

import java.net.Inet4Address;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.MacAddressFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import java.math.BigInteger;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for comparing flows.
 */
public final class FlowComparator {
    private final static Logger LOG = LoggerFactory.getLogger(FlowComparator.class);
    private static final BigInteger MINUS_ONE = BigInteger.valueOf(-1);

    private FlowComparator() {
        throw new UnsupportedOperationException("Utilities class should not be instantiated");
    }

    public static boolean flowEquals(final Flow statsFlow, final Flow storedFlow) {
        if (statsFlow == null || storedFlow == null) {
            return false;
        }
        if (statsFlow.getContainerName()== null) {
            if (storedFlow.getContainerName()!= null) {
                return false;
            }
        } else if(!statsFlow.getContainerName().equals(storedFlow.getContainerName())) {
            return false;
        }
        if (storedFlow.getPriority() == null) {
            if (statsFlow.getPriority() != null && statsFlow.getPriority()!= 0x8000) {
                return false;
            }
        } else if(!statsFlow.getPriority().equals(storedFlow.getPriority())) {
            return false;
        }
        if (statsFlow.getMatch()== null) {
            if (storedFlow.getMatch() != null) {
                return false;
            }
        } else if(!matchEquals(statsFlow.getMatch(), storedFlow.getMatch())) {
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
    public static boolean matchEquals(final Match statsFlow, final Match storedFlow) {
        if (statsFlow == storedFlow) {
            return true;
        }
        if (storedFlow == null && statsFlow != null) {
            return false;
        }
        if (statsFlow == null && storedFlow != null) {
            return false;
        }
        if (storedFlow.getEthernetMatch() == null) {
            if (statsFlow.getEthernetMatch() != null) {
                return false;
            }
        } else if(!ethernetMatchEquals(statsFlow.getEthernetMatch(),storedFlow.getEthernetMatch())) {
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
        }
        else if(!storedFlow.getIpMatch().equals(statsFlow.getIpMatch())) {
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

    /*
     * Custom EthernetMatch is required because mac address string provided by user in EthernetMatch can be in
     * any case (upper or lower or mix). Ethernet Match which controller receives from switch is always
     * an upper case string. Default EthernetMatch equals doesn't use equalsIgnoreCase() and hence it fails.
     * E.g User provided mac address string in flow match is aa:bb:cc:dd:ee:ff and when controller fetch
     * statistic data, openflow driver library returns AA:BB:CC:DD:EE:FF and default eqauls fails here.
     */
    @VisibleForTesting
    static boolean ethernetMatchEquals(final EthernetMatch statsEthernetMatch, final EthernetMatch storedEthernetMatch){
        boolean verdict = true;
        final Boolean checkNullValues = checkNullValues(statsEthernetMatch, storedEthernetMatch);
        if (checkNullValues != null) {
            verdict = checkNullValues;
        } else {
            if(verdict){
                verdict = ethernetMatchFieldsEquals(statsEthernetMatch.getEthernetSource(),storedEthernetMatch.getEthernetSource());
            }
            if(verdict){
                verdict = ethernetMatchFieldsEquals(statsEthernetMatch.getEthernetDestination(),storedEthernetMatch.getEthernetDestination());
            }
            if(verdict){
                if(statsEthernetMatch.getEthernetType() == null){
                    if(storedEthernetMatch.getEthernetType() != null){
                        verdict = false;
                    }
                }else{
                    verdict = statsEthernetMatch.getEthernetType().equals(storedEthernetMatch.getEthernetType());
                }
            }
        }
        return verdict;
    }

    private static boolean ethernetMatchFieldsEquals(final MacAddressFilter statsEthernetMatchFields,
                                                        final MacAddressFilter storedEthernetMatchFields){
        boolean verdict = true;
        final Boolean checkNullValues = checkNullValues(statsEthernetMatchFields, storedEthernetMatchFields);
        if (checkNullValues != null) {
            verdict = checkNullValues;
        } else {
            if(verdict){
                verdict = macAddressEquals(statsEthernetMatchFields.getAddress(), storedEthernetMatchFields.getAddress());
            }
            if(verdict){
                verdict = macAddressEquals(statsEthernetMatchFields.getMask(),storedEthernetMatchFields.getMask());
            }
        }
        return verdict;
    }

    private static boolean macAddressEquals(final MacAddress statsMacAddress, final MacAddress storedMacAddress){
        boolean verdict = true;
        final Boolean checkNullValues = checkNullValues(statsMacAddress, storedMacAddress);
        if (checkNullValues != null) {
            verdict = checkNullValues;
        } else {
            verdict = statsMacAddress.getValue().equalsIgnoreCase(storedMacAddress.getValue());
        }
        return verdict;
    }

    @VisibleForTesting
    static boolean layer3MatchEquals(final Layer3Match statsLayer3Match, final Layer3Match storedLayer3Match){
        boolean verdict = true;
        if(statsLayer3Match instanceof Ipv4Match && storedLayer3Match instanceof Ipv4Match){
            final Ipv4Match statsIpv4Match = (Ipv4Match)statsLayer3Match;
            final Ipv4Match storedIpv4Match = (Ipv4Match)storedLayer3Match;

            if (verdict) {
                verdict = compareNullSafe(
                        storedIpv4Match.getIpv4Destination(), statsIpv4Match.getIpv4Destination());
            }
            if (verdict) {
                verdict = compareNullSafe(
                        statsIpv4Match.getIpv4Source(), storedIpv4Match.getIpv4Source());
            }
        }else if (statsLayer3Match instanceof Ipv6Match && storedLayer3Match instanceof Ipv6Match) {
            final Ipv6Match statsIpv6Match = (Ipv6Match)statsLayer3Match;
            final Ipv6Match storedIpv6Match = (Ipv6Match)storedLayer3Match;


            if (verdict) {
                verdict = compareNullSafe(storedIpv6Match.getIpv6Destination(), statsIpv6Match.getIpv6Destination());
            }
            if (verdict) {
                verdict = compareNullSafe(statsIpv6Match.getIpv6Source(), storedIpv6Match.getIpv6Source());
            }


        }else{
            final Boolean nullCheckOut = checkNullValues(storedLayer3Match, statsLayer3Match);
            if (nullCheckOut != null) {
                verdict = nullCheckOut;
            } else {
                verdict = storedLayer3Match.equals(statsLayer3Match);
            }
        }

        return verdict;
    }

    private static boolean compareNullSafe(final Ipv4Prefix statsIpv4, final Ipv4Prefix storedIpv4) {
        boolean verdict = true;
        final Boolean checkDestNullValuesOut = checkNullValues(storedIpv4, statsIpv4);
        if (checkDestNullValuesOut != null) {
            verdict = checkDestNullValuesOut;
        } else if(!IpAddressEquals(statsIpv4, storedIpv4)){
            verdict = false;
        }

        return verdict;
    }

    private static boolean compareNullSafe(final Ipv6Prefix statsIpv6, final Ipv6Prefix storedIpv6) {
        boolean verdict = true;
        final Boolean checkDestNullValuesOut = checkNullValues(storedIpv6, statsIpv6);
        if (checkDestNullValuesOut != null) {
            verdict = checkDestNullValuesOut;
        } else if(!IpAddressEquals(statsIpv6, storedIpv6)){
            verdict = false;
        }

        return verdict;
    }

    private static Boolean checkNullValues(final Object v1, final Object v2) {
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
     *
     * @param statsIpAddress
     * @param storedIpAddress
     * @return true if IPv6prefixes equals
     */
    private static boolean IpAddressEquals(final Ipv6Prefix statsIpAddress, final Ipv6Prefix storedIpAddress) {

        final IntegerIpAddress statsIpAddressInt = StrIpToIntIpIpv6(statsIpAddress.getValue());
        final IntegerIpAddress storedIpAddressInt =  StrIpToIntIpIpv6(storedIpAddress.getValue());

        if(IpAndMaskBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }
        if(IpBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }

        return false;
    }

    /**
     * TODO: why don't we use the default Ipv4Prefix.equals()?
     *
     * @param statsIpAddress
     * @param storedIpAddress
     * @return true if IPv4prefixes equals
     */
    private static boolean IpAddressEquals(final Ipv4Prefix statsIpAddress, final Ipv4Prefix storedIpAddress) {
        final IntegerIpAddress statsIpAddressInt = StrIpToIntIp(statsIpAddress.getValue());
        final IntegerIpAddress storedIpAddressInt = StrIpToIntIp(storedIpAddress.getValue());

        if(IpAndMaskBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }
        if(IpBasedMatch(statsIpAddressInt,storedIpAddressInt)){
            return true;
        }
        return false;
    }

    private static boolean IpAndMaskBasedMatch(final IntegerIpAddress statsIpAddressInt,final IntegerIpAddress storedIpAddressInt){
        return ((statsIpAddressInt.getIp() & statsIpAddressInt.getMask()) ==  (storedIpAddressInt.getIp() & storedIpAddressInt.getMask()));
    }

    private static boolean IpBasedMatch(final IntegerIpAddress statsIpAddressInt,final IntegerIpAddress storedIpAddressInt){
        return (statsIpAddressInt.getIp() == storedIpAddressInt.getIp());
    }

    /**
     * Method return integer version of ip address. Converted int will be mask if
     * mask specified
     */
    private static IntegerIpAddress StrIpToIntIpIpv6(final String ipAddress){

        final String[] parts = ipAddress.split("/");
        final String ipString = parts[0];
        int prefix = 128;

        if (parts.length == 2) prefix = Integer.parseInt(parts[1]);
        final InetAddress a = InetAddresses.forString(ipString);
        Preconditions.checkArgument(a instanceof Inet6Address);
        final byte[] ipByte = a.getAddress();
        int ip = ByteBuffer.wrap(ipByte).getInt();
        final int mask = 0xffffffff << 128 - prefix;
        return new IntegerIpAddress(ip, mask);
    }

    /**
     * Method return integer version of ip address. Converted int will be mask if
     * mask specified
     */
    private static IntegerIpAddress StrIpToIntIp(final String ipAddresss){

        final String[] parts = ipAddresss.split("/");
        final String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 32;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        IntegerIpAddress integerIpAddress = null;

            final Inet4Address addr = ((Inet4Address) InetAddresses.forString(ip));
            final byte[] addrBytes = addr.getAddress();
            final int ipInt = ((addrBytes[0] & 0xFF) << 24) |
                    ((addrBytes[1] & 0xFF) << 16) |
                    ((addrBytes[2] & 0xFF) << 8)  |
                    ((addrBytes[3] & 0xFF) << 0);

            // FIXME: Is this valid?
            final int mask = 0xffffffff << 32 - prefix;

            integerIpAddress = new IntegerIpAddress(ipInt, mask);


        return integerIpAddress;
    }

    private static class IntegerIpAddress{
        int ip;
        int mask;
        public IntegerIpAddress(final int ip, final int mask) {
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
