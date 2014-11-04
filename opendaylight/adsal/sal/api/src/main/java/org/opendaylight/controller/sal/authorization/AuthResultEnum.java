
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Defines the enumerated values for possible results of an Authentication
 * request made from a AAA client to a AAA server.
 */

package org.opendaylight.controller.sal.authorization;

import java.io.Serializable;

public enum AuthResultEnum implements Serializable {
    AUTH_NONE("AUTH_NOT_ATTEMPTED"), AUTH_ACCEPT("AUTHENTICATION_ACCEPTED"), // request accepted
    AUTH_REJECT("AUTHENTICATION_REJECTED"), // request rejected
    AUTH_TIMEOUT("AUTHENTICATION_TIMEDOUT"), // request timeout
    AUTH_USERNAME_EMPTY("AUTHENTICATION_USERNAME_EMPTY"), // user name is empty
    AUTH_PASSWORD_EMPTY("AUTHENTICATION_PASSWORD_EMPTY"), // password is empty
    AUTH_SECRET_EMPTY("AUTHENTICATION_SECRET_EMPTY"), // secret is empty
    AUTH_COMM_ERROR("AUTHENTICATION_COMM_ERROR"), // communication channel problem
    AUTH_INVALID_ADDR("AUTHENTICATION_INVALID_ADDR"), // invalid network address
    AUTH_INVALID_PACKET("AUTHENTICATION_INVALID_PACKET"), // invalid packets or malformed attributes

    /*
     * Local AAA values
     */
    AUTH_ACCEPT_LOC("AUTHENTICATION_ACCEPTED"), // request accepted on local database
    AUTH_REJECT_LOC("AUTHENTICATION_REJECTED"), // request rejected on local database
    AUTH_INVALID_LOC_USER("INALID_LOCAL_USER"),

    /*
     * Authorization
     */
    AUTHOR_PASS("AUTHORIZATION_PASSED"), AUTHOR_FAIL("AUTHORIZATION_FAILED"), AUTHOR_ERROR(
            "AUTHORIZATION_SERVER_ERROR");

    private AuthResultEnum(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}