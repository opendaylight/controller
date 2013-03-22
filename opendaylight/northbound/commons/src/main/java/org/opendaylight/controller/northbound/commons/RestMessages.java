/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons;

public enum RestMessages {
    SUCCESS("Success"), NOCONTAINER("Container does not exist"), NOFLOWSPEC(
            "Flow Spec does not exist"), NOSUBNET("Subnet does not exist"), NOSTATICROUTE(
            "Static Route does not exist"), NOHOST("Host does not exist"), NOFLOW(
            "Flow does not exist"), NONODE("Node does not exist"), NOPOLICY(
            "Policy does not exist"), NORESOURCE("Resource does not exist"), RESOURCECONFLICT(
            "Operation failed due to Resource Conflict"), NODEFAULT(
            "Container default is not a custom container"), DEFAULTDISABLED(
            "Container(s) are configured. Container default is not operational"), NOTALLOWEDONDEFAULT(
            "Container default is a static resource, no modification allowed on it"), UNKNOWNACTION(
            "Unknown action"), INVALIDJSON("JSON message is invalid"), INVALIDADDRESS(
            "invalid InetAddress"), AVAILABLESOON(
            "Resource is not implemented yet"), INTERNALERROR("Internal Error"), SERVICEUNAVAILABLE(
            "Service is not available. Could be down for maintanence"), INVALIDDATA(
            "Data is invalid or conflicts with URI");

    private String message;

    private RestMessages(String msg) {
        message = msg;
    }

    public String toString() {
        return message;
    }
}
