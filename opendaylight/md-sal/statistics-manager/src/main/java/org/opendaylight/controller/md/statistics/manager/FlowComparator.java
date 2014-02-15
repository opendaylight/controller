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
