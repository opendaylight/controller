/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.action;

import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNextHop extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_NEXT_HOP";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private InetAddress address;

    public SetNextHop() {
        super(NAME);
    }

    public SetNextHop(InetAddress address) {
        super(NAME);
        this.address = address;
    }

    public InetAddress getAddress() {
        return address;
    }

    @XmlElement
    public String getAddressAsString() {
        return address.getHostAddress();
    }

    @Override
    public String toString() {
        return NAME + "=" + getAddressAsString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SetNextHop)) {
            return false;
        }
        SetNextHop other = (SetNextHop) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public SetNextHop fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            return new SetNextHop(NetUtils.parseInetAddress(matcher.group(1)));
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return address != null;
    }
}