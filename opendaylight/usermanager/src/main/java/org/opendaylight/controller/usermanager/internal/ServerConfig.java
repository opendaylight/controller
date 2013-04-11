
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Configuration Java Object which represents a Remote AAA server configuration
 * information for User Manager.
 */
public class ServerConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    // Order matters: JSP file expects following fields in the following order
    private String ip;
    private String secret;
    private String protocol;

    public ServerConfig() {
    }

    public ServerConfig(String ip, String secret, String protocol) {
    	this.ip = ip;
    	this.secret = secret;
    	this.protocol = protocol;
    }
    
    public String getAddress() {
        return ip;
    }

    public String getSecret() {
        return secret;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public boolean isValid() {
        return (ip != null && !ip.isEmpty() && secret != null && !secret
                .isEmpty());
    }
}
