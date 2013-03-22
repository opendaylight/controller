
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.action.OFAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * This class is used to create IPv6 Vendor Extension messages. Specfically, It
 * defines the methods used in creation of Vendor specific IPv6 Flow Mod message.
 * 
 *
 */
public class V6FlowMod extends OFVendor implements Cloneable {
    private static final Logger logger = LoggerFactory
            .getLogger(V6FlowMod.class);
    private static final long serialVersionUID = 1L;
    protected V6Match match;
    protected long cookie;
    protected short command;
    protected short idleTimeout;
    protected short hardTimeout;
    protected short priority;
    protected int bufferId;
    protected short outPort;
    protected short flags;
    protected List<OFAction> actions;
    short match_len;
    short actions_len;
    short pad_size;

    private static int IPV6EXT_ADD_FLOW_MSG_TYPE = 13;
    private static int IPV6_EXT_MIN_HDR_LEN = 36;

    /**
     * Constructor for the V6FlowMod class. Initializes OFVendor (parent class) 
     * fields by calling the parent class' constructor.
     */
    public V6FlowMod() {
        super();
    }

    /**
     * This method sets the match fields of V6FlowMod object
     * @param match		V6Match object for this V6FlowMod message
     */
    public void setMatch(V6Match match) {
        this.match = match;
    }

    /**
     * Sets the list of actions V6FlowMod message
     * @param actions 	a list of ordered OFAction objects
     */
    public void setActions(List<OFAction> actions) {
        this.actions = actions;
    }

    /**
     * Sets the priority field of V6FlowMod message
     * @param priority 	Priority of the message
     */
    public void setPriority(short priority) {
        this.priority = priority;
    }

    /**
     * Sets the cookie field of V6FlowMod message
     * @param cookie 	Cookie of the message
     */
    public void setCookie(long cookie) {
        this.cookie = cookie;
    }

    /**
     * Sets the command field of V6FlowMod message
     * @param command 	Command type of the message (ADD or DELETE)
     */
    public V6FlowMod setCommand(short command) {
        this.command = command;
        return this;
    }

    /**
     * Sets the outPort field of V6FlowMod message
     * @param outPort 	outPort of the message
     */
    public V6FlowMod setOutPort(OFPort port) {
        this.outPort = port.getValue();
        return this;
    }

    /**
     * Sets the idle_timeout of V6FlowMod message
     * @param idleTimeout	idle timeout for this message
     */
    public void setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Sets the hardTimeout field of V6FlowMod message
     * @param hardTimeout 	hard timeout of the message
     */
    public void setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    /**
     * Returns the Flow Mod message subtype for V6FlowMod message
     * @return			message subtype
     */
    private int getIPv6ExtensionFlowModAddSubType() {
        return IPV6EXT_ADD_FLOW_MSG_TYPE;
    }
    
    /**
     * Returns the minimum header size for V6Flow Message type
     * @return		minimum header size
     */

    public int getV6FlowModMinHdrSize() {
        return IPV6_EXT_MIN_HDR_LEN;
    }
    
    /**
     * Sets the Vendor type in OFVendor message
     */

    public void setVendor() {
        super.setVendor(V6StatsRequest.NICIRA_VENDOR_ID);
    }
    
    /**
     * This method forms the Vendor extension IPv6 Flow Mod message.It uses the
     * fields in V6FlowMod class, and writes the data according to vendor 
     * extension format. The fields include flow properties (cookie, timeout,
     * priority, etc), flow match, and action list. It also takes care of 
     * required padding.
     */

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(getIPv6ExtensionFlowModAddSubType());
        data.putLong(this.cookie);
        data.putShort(command); /* should be OFPFC_ADD, OFPFC_DELETE_STRICT, etc*/
        data.putShort(this.idleTimeout);
        data.putShort(this.hardTimeout);
        data.putShort(this.priority);
        data.putInt(OFPacketOut.BUFFER_ID_NONE);
        data.putShort(outPort); /* output_port */
        data.putShort((short) 0); /* flags */
        match_len = this.match.getIPv6MatchLen();
        data.putShort(match_len);
        byte[] pad = new byte[6];
        data.put(pad);
        this.match.writeTo(data);

        pad_size = (short) (((match_len + 7) / 8) * 8 - match_len);

        /*
         * action list should be preceded by a padding of 0 to 7 bytes based upon
         * above formula.
         */

        byte[] pad2 = new byte[pad_size];
        data.put(pad2);
        if (actions != null) {
            for (OFAction action : actions) {
                actions_len += action.getLength();
                action.writeTo(data);
            }
        }
        logger.trace("{}", this.toString());
    }

    /**
     * Forms the clone of V6FlowMod Object. If Object is returned
     * successfully, then returns the cloned object. Throws an 
     * exception if cloning is not supported.
     */
    @Override
    public V6FlowMod clone() {
        try {
            V6Match neoMatch = match.clone();
            V6FlowMod v6flowMod = (V6FlowMod) super.clone();
            v6flowMod.setMatch(neoMatch);
            List<OFAction> neoActions = new LinkedList<OFAction>();
            for (OFAction action : this.actions)
                neoActions.add((OFAction) action.clone());
            v6flowMod.setActions(neoActions);
            return v6flowMod;
        } catch (CloneNotSupportedException e) {
            // Won't happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints the contents of V6FlowMod in a string format.
     */
    @Override
    public String toString() {
        return "V6FlowMod[" + ReflectionToStringBuilder.toString(this) + "]";
    }

}
