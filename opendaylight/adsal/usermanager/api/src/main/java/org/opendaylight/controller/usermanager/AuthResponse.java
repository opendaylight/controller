
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;

/**
 * The class describes AAA response status and payload data
 */
public class AuthResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> data;
    private AuthResultEnum status;
    private AuthResultEnum authorStatus;

    public AuthResponse() {
        this.data = new LinkedList<String>();
        this.status = AuthResultEnum.AUTH_NONE;
        this.authorStatus = AuthResultEnum.AUTH_NONE;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public void addData(String data) {
        this.data.add(data);
    }

    public List<String> getData() {
        return data;
    }

    public void setStatus(AuthResultEnum status) {
        this.status = status;
    }

    public AuthResultEnum getStatus() {
        return status;
    }

    public void setAuthorizationStatus(AuthResultEnum authorStatus) {
        this.authorStatus = authorStatus;
    }

    public AuthResultEnum getAuthorizationStatus() {
        return authorStatus;
    }

    public String toString() {
        return ("\nReceived messages: " + data.toString() + "\nStatus: " + status);
    }

    public void resetData(String rolesData) {
        // TODO Auto-generated method stub

    }
}