/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronSubnet_IPAllocationPool {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="start")
    String poolStart;

    @XmlElement(name="end")
    String poolEnd;

    public NeutronSubnet_IPAllocationPool() { }

    public NeutronSubnet_IPAllocationPool(String lowAddress, String highAddress) {
        poolStart = lowAddress;
        poolEnd = highAddress;
    }

    public String getPoolStart() {
        return poolStart;
    }

    public void setPoolStart(String poolStart) {
        this.poolStart = poolStart;
    }

    public String getPoolEnd() {
        return poolEnd;
    }

    public void setPoolEnd(String poolEnd) {
        this.poolEnd = poolEnd;
    }

    /**
     * This method determines if this allocation pool contains the
     * input IPv4 address
     *
     * @param inputString
     *            IPv4 address in dotted decimal format
     * @returns a boolean on whether the pool contains the address or not
     */

    public boolean contains(String inputString) {
        long inputIP = convert(inputString);
        long startIP = convert(poolStart);
        long endIP = convert(poolEnd);
        return (inputIP >= startIP && inputIP <= endIP);
    }

    /**
     * This static method converts the supplied IPv4 address to a long
     * integer for comparison
     *
     * @param inputString
     *            IPv4 address in dotted decimal format
     * @returns high-endian representation of the IPv4 address as a long
     */

    static long convert(String inputString) {
        long ans = 0;
        String[] parts = inputString.split("\\.");
        for (String part: parts) {
            ans <<= 8;
            ans |= Integer.parseInt(part);
        }
        return ans;
    }

    /**
     * This static method converts the supplied high-ending long back
     * into a dotted decimal representation of an IPv4 address
     *
     * @param l
     *            high-endian representation of the IPv4 address as a long
     * @returns IPv4 address in dotted decimal format
     */
    static String longtoIP(long l) {
        int i;
        String[] parts = new String[4];
        for (i=0; i<4; i++) {
            parts[3-i] = String.valueOf(l & 255);
            l >>= 8;
        }
        return join(parts,".");
    }

    /*
     * helper routine used by longtoIP
     */
    public static String join(String r[],String d)
    {
        if (r.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i;
        for(i=0;i<r.length-1;i++) {
            sb.append(r[i]+d);
        }
        return sb.toString()+r[i];
    }

    /*
     * This method splits the current instance by removing the supplied
     * parameter.
     *
     * If the parameter is either the low or high address,
     * then that member is adjusted and a list containing just this instance
     * is returned.
     *
     * If the parameter is in the middle of the pool, then
     * create two new instances, one ranging from low to parameter-1
     * the other ranging from parameter+1 to high
     */
    public List<NeutronSubnet_IPAllocationPool> splitPool(String ipAddress) {
        List<NeutronSubnet_IPAllocationPool> ans = new ArrayList<NeutronSubnet_IPAllocationPool>();
        long gIP = NeutronSubnet_IPAllocationPool.convert(ipAddress);
        long sIP = NeutronSubnet_IPAllocationPool.convert(poolStart);
        long eIP = NeutronSubnet_IPAllocationPool.convert(poolEnd);
        long i;
        NeutronSubnet_IPAllocationPool p = new NeutronSubnet_IPAllocationPool();
        boolean poolStarted = false;
        for (i=sIP; i<=eIP; i++) {
            if (i == sIP) {
                if (i != gIP) {
                    p.setPoolStart(poolStart);
                    poolStarted = true;
                }
            }
            if (i == eIP) {
                if (i != gIP) {
                    p.setPoolEnd(poolEnd);
                } else {
                    p.setPoolEnd(NeutronSubnet_IPAllocationPool.longtoIP(i-1));
                }
                ans.add(p);
            }
            if (i != sIP && i != eIP) {
                if (i != gIP) {
                    if (!poolStarted) {
                        p.setPoolStart(NeutronSubnet_IPAllocationPool.longtoIP(i));
                        poolStarted = true;
                    }
                } else {
                    p.setPoolEnd(NeutronSubnet_IPAllocationPool.longtoIP(i-1));
                    poolStarted = false;
                    ans.add(p);
                    p = new NeutronSubnet_IPAllocationPool();
                }
            }
        }
        return ans;
    }
}
