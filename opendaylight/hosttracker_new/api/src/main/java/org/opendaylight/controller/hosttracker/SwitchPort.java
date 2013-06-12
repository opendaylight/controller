/*
 * Copyright (c) 2012 Big Switch Networks, Inc.
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *    Originally created by David Erickson, Stanford University 
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the
 *    License. You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an "AS
 *    IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *    express or implied. See the License for the specific language
 *    governing permissions and limitations under the License. 
 */

package org.opendaylight.controller.hosttracker;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * A simple switch DPID/port pair This class is immutable
 * 
 * @author readams
 * 
 */
public class SwitchPort {
    public enum ErrorStatus {
        DUPLICATE_DEVICE("duplicate-device");

        private String value;

        ErrorStatus(String v) {
            value = v;
        }

        @Override
        public String toString() {
            return value;
        }

        public static ErrorStatus fromString(String str) {
            for (ErrorStatus m : ErrorStatus.values()) {
                if (m.value.equals(str)) {
                    return m;
                }
            }
            return null;
        }
    }

    private final NodeConnector port;
    private final ErrorStatus errorStatus;

    /**
     * Simple constructor
     * 
     * @param switchDPID
     *            the dpid
     * @param port
     *            the port
     * @param errorStatus
     *            any error status for the switch port
     */
    public SwitchPort(NodeConnector port, ErrorStatus errorStatus) {
        super();
        this.port = port;
        this.errorStatus = errorStatus;
    }

    /**
     * Simple constructor
     * 
     * @param switchDPID
     *            the dpid
     * @param port
     *            the port
     */
    public SwitchPort(NodeConnector port) {
        super();
        this.port = port;
        this.errorStatus = null;
    }

    // ***************
    // Getters/Setters
    // ***************

    public NodeConnector getPort() {
        return port;
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }

    // ******
    // Object
    // ******

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((errorStatus == null) ? 0 : errorStatus.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        SwitchPort other = (SwitchPort) obj;
        if (errorStatus != other.errorStatus)
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SwitchPort [port=" + port + ", errorStatus=" + errorStatus
                + "]";
    }
}