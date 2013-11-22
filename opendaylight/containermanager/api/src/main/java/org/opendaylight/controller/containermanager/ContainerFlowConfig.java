
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow Specification Java Object for Container Manager
 * Represents a container flow configuration information for Container Manager.
 *
 * Objects of this class are serialized to and de-serialized from binary files through
 * java serialization API when saving to/reading from Container manager startup
 * configuration file.
 */
@XmlRootElement (name = "flow-spec-config")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerFlowConfig implements Serializable {
    private static Logger log = LoggerFactory.getLogger(ContainerFlowConfig.class);

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The Constant regexName. */
    private static final String regexName = "^[\\w-+.@]+$";

    /** Flow Spec name. */
    @XmlElement
    private String name;

    /** The vlan. */
    @XmlElement
    private String dlVlan;

    /** The network Source. */
    @XmlElement
    private String nwSrc;

    /** The network Destination */
    @XmlElement
    private String nwDst;

    /** The protocol. */
    @XmlElement
    private String protocol;

    /** The transport source */
    @XmlElement
    private String tpSrc;

    /** The transport destination */
    @XmlElement
    private String tpDst;

    /* unidirectional flag
    do not include this flag in equality check
    @XmlElement */
    private static boolean unidirectional = false;


    /**
     * Instantiates a new container flow config.
     */
    public ContainerFlowConfig() {
    }

    /**
     * Instantiates a new container flow config.
     *
     * @param name Flow Spec configuration name
     * @param container Container Name
     * @param srcIP Source IP Address
     * @param dstIP Destination IP Address
     * @param proto Protocol
     * @param srcPort Source Layer4 Port
     * @param dstPort Destination Layer4 Port
     */
    public ContainerFlowConfig(String name, String srcIP, String dstIP, String proto, String srcPort,
            String dstPort) {
        this.name = name;
        this.dlVlan = null;
        this.nwSrc = srcIP;
        this.nwDst = dstIP;
        this.protocol = proto;
        this.tpSrc = srcPort;
        this.tpDst = dstPort;
        //this.unidirectional = false;
    }

    public ContainerFlowConfig(String name, String dlVlan, String srcIP, String dstIP, String proto, String srcPort,
            String dstPort) {
        this.name = name;
        this.dlVlan = dlVlan;
        this.nwSrc = srcIP;
        this.nwDst = dstIP;
        this.protocol = proto;
        this.tpSrc = srcPort;
        this.tpDst = dstPort;
    }


    public ContainerFlowConfig(ContainerFlowConfig containerFlowConfig) {
        this.name = containerFlowConfig.name;
        this.dlVlan = containerFlowConfig.dlVlan;
        this.nwSrc = containerFlowConfig.nwSrc;
        this.nwDst = containerFlowConfig.nwDst;
        this.protocol = containerFlowConfig.protocol;
        this.tpSrc = containerFlowConfig.tpSrc;
        this.tpDst = containerFlowConfig.tpDst;
        //this.unidirectional = containerFlowConfig.unidirectional;
    }

    /**
     * Returns the name of this Flow Specification
     *
     * @return the name of the Flow Specification
     */
    public String getName() {
        // mandatory field
        return name;
    }

    /**
     * Returns the vlan id.
     *
     * @return the Vlan Id
     */
    public String getVlan() {
        return (dlVlan == null || dlVlan.isEmpty()) ? null : dlVlan;
    }

    /**
     * Returns the Source IP Address.
     *
     * @return the Source IP Address
     */
    public String getSrcIP() {
        return (nwSrc == null || nwSrc.isEmpty()) ? null : nwSrc;
    }

    /**
     * Returns the Destination IP Address.
     *
     * @return the Destination IP Address
     */
    public String getDstIP() {
        return (nwDst == null || nwDst.isEmpty()) ? null : nwDst;
    }

    /**
     * Returns the protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns Source Layer4 Port.
     *
     * @return Source Layer4 Port
     */
    public String getSrcPort() {
        return (tpSrc == null || tpSrc.isEmpty()) ? null : tpSrc;
    }

    /**
     * Returns Destination Layer4 Port.
     *
     * @return Destination Layer4 Port
     */
    public String getDstPort() {
        return (tpDst == null || tpDst.isEmpty()) ? null : tpDst;
    }

    /*
     * @return the unidirectional flag
     */
    public boolean isUnidirectional() {
        return unidirectional;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((dlVlan == null) ? 0 : dlVlan.hashCode());
        result = prime * result + ((nwDst == null) ? 0 : nwDst.hashCode());
        result = prime * result + ((tpDst == null) ? 0 : tpDst.hashCode());
        result = prime * result + ((nwSrc == null) ? 0 : nwSrc.hashCode());
        result = prime * result + ((tpSrc == null) ? 0 : tpSrc.hashCode());
        return result;
    }

    /*
     * For comparison, consider that container flow can have empty fields
     */
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        /*
         * Configuration will be stored in collection only if it is valid
         * Hence we don't check here for uninitialized fields
         */
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerFlowConfig other = (ContainerFlowConfig) obj;
        if (matchName(other) && matchDlVlan(other) && matchSrcIP(other)
                && matchDstIP(other) && matchProtocol(other)
                && matchSrcPort(other) && matchDstPort(other)) {
            return true;
        }
        return false;
    }

    /**
     * Equals by Flow Spec name.
     *
     * @param name flow spec name for comparison
     * @return true, if successful
     */
    public boolean equalsByName(String name) {
        return this.name.equals(name);
    }

    /**
     * equalsByMatch
     *
     * @param that ContainerFlowConfig for comparison
     * @return true, if any match is equal
     */
    public boolean equalsByMatch(ContainerFlowConfig that) {
        // get both matches
        // the match is equal if any of the match is equal
        List<Match> thisMatch = this.getMatches();
        List<Match> otherMatch = that.getMatches();
        // both the lists cannot be null
        for(Match m1 : thisMatch) {
            for(Match m2 : otherMatch) {
                  if(m1.equals(m2)) {
                    return true;
                }
            }
        }
        // if you have reached here without a match
        // being found
        // return false
        return false;
    }

    /**
     * Matches the name of this flow spec with that of ContainerFlowConfig parameter's flow spec.
     *
     * @param o the ContainerFlowConfig parameter
     * @return true, if successful
     */
    private boolean matchName(ContainerFlowConfig flowSpec) {
        if (name == flowSpec.name) {
            return true;
        }
        if (name == null || flowSpec.name == null) {
            return false;
        }
        return name.equals(flowSpec.name);
    }

    /**
     * Match Source IP Address.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchDlVlan(ContainerFlowConfig flowSpec) {
        if (dlVlan == flowSpec.dlVlan) {
            return true;
        }
        if (dlVlan == null || flowSpec.dlVlan == null) {
            return false;
        }
        return dlVlan.equals(flowSpec.dlVlan);
    }

    /**
     * Match Source IP Address.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchSrcIP(ContainerFlowConfig flowSpec) {
        if (nwSrc == flowSpec.nwSrc) {
            return true;
        }
        if (nwSrc == null || flowSpec.nwSrc == null) {
            return false;
        }
        return nwSrc.equals(flowSpec.nwSrc);
    }

    /**
     * Match Destination IP Address.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchDstIP(ContainerFlowConfig flowSpec) {
        if (nwDst == flowSpec.nwDst) {
            return true;
        }
        if (nwDst == null || flowSpec.nwDst == null) {
            return false;
        }
        return this.nwDst.equals(flowSpec.nwDst);
    }

    /**
     * Match protocol.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchProtocol(ContainerFlowConfig flowSpec) {
        if (protocol == flowSpec.protocol) {
            return true;
        }
        if (protocol == null || flowSpec.protocol == null) {
            return false;
        }
        return this.protocol.equals(flowSpec.protocol);
    }

    /**
     * Match Source Layer4 Port.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchSrcPort(ContainerFlowConfig flowSpec) {
        if (tpSrc == flowSpec.tpSrc) {
            return true;
        }
        if (tpSrc == null || flowSpec.tpSrc == null) {
            return false;
        }
        return tpSrc.equals(flowSpec.tpSrc);
    }

    /**
     * Match Destination Layer4 Port.
     *
     * @param flowSpec Flow Specification
     * @return true, if successful
     */
    private boolean matchDstPort(ContainerFlowConfig flowSpec) {
        if (tpDst == flowSpec.tpDst) {
            return true;
        }
        if (tpDst == null || flowSpec.tpDst == null) {
            return false;
        }
        return this.tpDst.equals(flowSpec.tpDst);
    }

    /**
     * Returns the vlan id number
     *
     * @return the vlan id number
     */
    public Short getVlanId() {
        Short vlan = 0;
        try {
            vlan = Short.parseShort(dlVlan);
        } catch (NumberFormatException e) {

        }
        return vlan;
    }

    /**
     * Returns the Source IP Address mask length.
     *
     * @return the Source IP Address mask length
     */
    public Short getSrcIPMaskLen() {
        Short maskLen = 0;

        if (nwSrc != null && !nwSrc.isEmpty()) {
            String[] s = nwSrc.split("/");
            if (s.length == 2) {
                try {
                    maskLen = Short.valueOf(s[1]);
                } catch (Exception e) {
                    // no mask or bad mask
                }
            } else {
                InetAddress ip = this.getSrcIPNum();
                maskLen = (short) ((ip instanceof Inet4Address) ? 32 : 128);
            }
        }
        return maskLen;
    }

    /**
     * Returns the Destination IP Address mask length.
     *
     * @return the Destination IP Address mask length
     */
    public Short getDstIPMaskLen() {
        Short maskLen = 0;
        if (nwDst != null && !nwDst.isEmpty()) {
            String[] s = nwDst.split("/");
            if (s.length == 2) {
                try {
                    maskLen = Short.valueOf(s[1]);
                } catch (Exception e) {
                    // no mask or bad mask
                }
            } else {
                InetAddress ip = this.getDstIPNum();
                maskLen = (short) ((ip instanceof Inet4Address) ? 32 : 128);
            }
        }
        return maskLen;
    }

    /**
     * Returns the Source IP Address.
     *
     * @return the Source IP Address
     */
    public InetAddress getSrcIPNum() {
        InetAddress ip = null;
        if (nwSrc == null || nwSrc.isEmpty()) {
            try {
                ip = InetAddress.getByAddress(new byte[16]);
                return ip;
            } catch (UnknownHostException e) {
                log.error("", e);
                return null;
            }
        }
        try {
            ip = InetAddress.getByName(nwSrc.split("/")[0]);
        } catch (UnknownHostException e1) {
            log.error("", e1);
            return null;
        }
        return ip;
    }

    /**
     * Returns the Destination IP Address.
     *
     * @return the Destination IP Address
     */
    public InetAddress getDstIPNum() {
        InetAddress ip = null;
        if (nwDst == null || nwDst.isEmpty()) {
            try {
                ip = InetAddress.getByAddress(new byte[16]);
                return ip;
            } catch (UnknownHostException e) {
                log.error("",e);
                return null;
            }
        }
        try {
            ip = InetAddress.getByName(nwDst.split("/")[0]);
        } catch (UnknownHostException e1) {
            log.error("", e1);
            return null;
        }
        return ip;
    }

    /**
     * Returns Source Layer4 Port number.
     *
     * @return Source Layer4 Port number
     */
    public Short getSrcPortNum() {
        return (tpSrc == null || tpSrc.isEmpty()) ? Short.valueOf((short) 0)
                : Short.valueOf(tpSrc);
    }

    /**
     * Returns Destination Layer4 Port number.
     *
     * @return Destination Layer4 Port number
     */
    public Short getDstPortNum() {
        return (tpDst == null || tpDst.isEmpty()) ? Short.valueOf((short) 0)
                : Short.valueOf(tpDst);
    }

    /**
     * Get the IP protocol value
     *
     * @return the protocol
     */
    public Short getProtoNum() {
        return protocol == null ? null : IPProtocols.getProtocolNumberShort(protocol);
    }

    /**
     * Returns whether this container flow overlap with the passed one This is
     * true when any two of the resulting matches for the two container flow
     * configurations intersect.
     *
     * @param other
     *            the other container flow config with which checking the
     *            overlap
     * @return true if the two configurations overlap, false otherwise
     */
    public boolean overlap(ContainerFlowConfig other) {
        if (other == null) {
            return false;
        }
        List<Match> myMathes = this.getMatches();
        List<Match> hisMatches = other.getMatches();
        for (Match mine : myMathes) {
            for (Match his : hisMatches) {
                if (mine.intersetcs(his)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if this flow specification configuration is valid.
     *
     * @return true, if is valid
     */
    public Status validate() {
        if (!hasValidName()) {
            return new Status(StatusCode.BADREQUEST, "Invalid name");
        }
        Status status = validateVlan();
        if (!status.isSuccess()) {
            return status;
        }
        status = validateIPs();
        if (!status.isSuccess()) {
            return status;
        }
        if(!hasValidProtocol()) {
            return new Status(StatusCode.BADREQUEST, "Invalid IP protocol");
        }
        if (!hasValidPorts()) {
            return new Status(StatusCode.BADREQUEST, "Invalid Source or Destination Port");
        }
        if (this.getMatches().get(0).getMatches() == 0) {
            return new Status(StatusCode.BADREQUEST, "Flow Spec is empty");
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Checks if this flow specification configuration has a valid name.
     *
     * @return true, if successful
     */
    private boolean hasValidName() {
        return (name != null && !name.isEmpty() && name.matches(regexName));
    }

    /**
     * Validates the vlan number
     *
     * @return the result of the check as Status object
     */
    private Status validateVlan() {
        if (dlVlan != null) {
            short vlanId = 0;
            try {
                vlanId = Short.parseShort(dlVlan);
            } catch (NumberFormatException e) {
                return new Status(StatusCode.BADREQUEST, "Invalid vlan id");
            }
            if (vlanId < 0 || vlanId > 0xfff) {
                return new Status(StatusCode.BADREQUEST, "Invalid vlan id");
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Validates the network addresses, checks syntax and semantic
     *
     * @return the result of the check as Status object, if successful
     */
    private Status validateIPs() {
        if (nwSrc != null) {
            if (!NetUtils.isIPAddressValid(nwSrc)) {
                return new Status(StatusCode.BADREQUEST, "Invalid network source address");
            }
            byte[] bytePrefix = NetUtils.getSubnetPrefix(this.getSrcIPNum(), this.getSrcIPMaskLen()).getAddress();
            long prefix = BitBufferHelper.getLong(bytePrefix);
            if (prefix == 0) {
                return new Status(StatusCode.BADREQUEST, "Invalid network source address: subnet zero");
            }
        }
        if (nwDst != null) {
            if (!NetUtils.isIPAddressValid(nwDst)) {
                return new Status(StatusCode.BADREQUEST, "Invalid network destination address");
            }
            byte[] bytePrefix = NetUtils.getSubnetPrefix(this.getDstIPNum(), this.getDstIPMaskLen()).getAddress();
            long prefix = BitBufferHelper.getLong(bytePrefix);
            if (prefix == 0) {
                return new Status(StatusCode.BADREQUEST, "Invalid network destination address: subnet zero");
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Validate the protocol field. Either it can be a enum defined in IPProtocols.java
     * or a valid IP proto value between 0 and 255, see:
     * http://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
     * for more details.
     *
     * @return true if a valid protocol value
     */
    private boolean hasValidProtocol() {
        IPProtocols p = IPProtocols.fromString(protocol);
        return p != null;
    }

    /**
     *
     * @param tpPort
     *               String representing the transport protocol port number
     * @return true if tpPort contains a decimal value between 0 and 65535
     */
    private boolean hasValidPort(String tpPort) {
        try {
            int port = Integer.decode(tpPort);
            return ((port >= 0) && (port <= 0xffff));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate the transport protocol source and destination ports as
     * entered by users.
     *
     * @return true if ports are defined and are in valid range
     */
    private boolean hasValidPorts() {
        if (tpSrc !=null && !tpSrc.isEmpty()) {
            if (!hasValidPort(tpSrc)) {
                return false;
            }
        }

        if (tpDst !=null && !tpDst.isEmpty()) {
            return hasValidPort(tpDst);
        }
        return true;
    }

    /**
     * Returns the matches.
     * If unidirectional flag is set, there will be only one match in the list
     * If unidirectional flag is unset there will be two matches in the list,
     * only if the specified flow has an intrinsic direction.
     * For Ex. if the cFlow only has the protocol field configured, no matter
     * if unidirectional flag is set or not, only one match will be returned
     * The client just has to iterate over the returned list
     * @return the matches
     */
    public List<Match> getMatches() {
        List<Match> matches = new ArrayList<Match>();
        Match match = new Match();

        if (this.dlVlan != null && !this.dlVlan.isEmpty()) {
            match.setField(MatchType.DL_VLAN, this.getVlanId());
        }
        if (this.nwSrc != null && !this.nwSrc.trim().isEmpty()) {
            String parts[] = this.nwSrc.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            int maskLen = 0;
            if (parts.length > 1) {
                maskLen = Integer.parseInt(parts[1]);
            } else {
                maskLen = (ip instanceof Inet6Address) ? 128 : 32;
            }
            mask = NetUtils.getInetNetworkMask(maskLen, ip instanceof Inet6Address);
            match.setField(MatchType.NW_SRC, ip, mask);
        }
        if (this.nwDst != null && !this.nwDst.trim().isEmpty()) {
            String parts[] = this.nwDst.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            int maskLen = 0;
            if (parts.length > 1) {
                maskLen = Integer.parseInt(parts[1]);
            } else {
                maskLen = (ip instanceof Inet6Address) ? 128 : 32;
            }
            mask = NetUtils.getInetNetworkMask(maskLen, ip instanceof Inet6Address);
            match.setField(MatchType.NW_DST, ip, mask);
        }
        if (IPProtocols.fromString(this.protocol) != IPProtocols.ANY) {
            match.setField(MatchType.NW_PROTO, IPProtocols.getProtocolNumberByte(this.protocol));
        }
        if (this.tpSrc != null && !this.tpSrc.trim().isEmpty()) {
            match.setField(MatchType.TP_SRC, Integer.valueOf(tpSrc).shortValue());
        }
        if (this.tpDst != null && !this.tpDst.trim().isEmpty()) {
            match.setField(MatchType.TP_DST, Integer.valueOf(tpDst).shortValue());
        }

        matches.add(match);
        if(!ContainerFlowConfig.unidirectional) {
            Match reverse = match.reverse();
            if (!match.equals(reverse)) {
                matches.add(reverse);
            }
        }
        return matches;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Container Flow={name:" + name + " dlVlan:" + dlVlan + " nwSrc:" + nwSrc + " nwDst:" + nwDst + " " + "protocol:" + protocol
                + " tpSrc:" + tpSrc + " tpDst:" + tpDst + "}";
    }
}
