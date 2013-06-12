
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import java.io.Serializable;

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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((secret == null) ? 0 : secret.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerConfig other = (ServerConfig) obj;
        if (ip == null) {
            if (other.ip != null)
                return false;
        } else if (!ip.equals(other.ip))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (secret == null) {
            if (other.secret != null)
                return false;
        } else if (!secret.equals(other.secret))
            return false;
        return true;
    }

    public boolean isValid() {
        return (ip != null && !ip.isEmpty() && secret != null && !secret
                .isEmpty());
    }
}
