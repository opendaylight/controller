/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.Serializable;

/**
 * Represents the return object of the osgi service interfaces function calls.
 * It contains a code {@code StatusCode} representing the result of the call and
 * a string which describes a failure reason (if any) in human readable form.
 */
@Deprecated
public class Status implements Serializable {
    private static final long serialVersionUID = 0L;
    private StatusCode code;
    private String description;
    private long requestId;

    /**
     * Generates an instance of the Status class. This is used as return code
     * for internal API2 function calls. This constructor allows to specify,
     * beside the Status Code, a custom human readable description to add more
     * information about the status.
     *
     * @param errorCode
     *            The status code. If passed as null, code will be stored as
     *            {@code StatusCode.UNDEFINED}
     * @param description
     *            The human readable description of the status. If passed as
     *            null, description will be inferred by the code
     */
    public Status(StatusCode errorCode, String description) {
        this.code = (errorCode != null) ? errorCode : StatusCode.UNDEFINED;
        this.description = (description != null) ? description : this.code
                .toString();
        this.requestId = 0;
    }

    /**
     * Generates an instance of the Status class based on the passed StatusCode
     * only. The description field of the Status object will be inferred by the
     * status code.
     *
     * @param errorCode
     *            The status code. If passed as null, code will be stored as
     *            {@code StatusCode.UNDEFINED}
     */
    public Status(StatusCode errorCode) {
        this.code = (errorCode != null) ? errorCode : StatusCode.UNDEFINED;
        this.description = (description != null) ? description : this.code
                .toString();
        this.requestId = 0;
    }

    /**
     * Generates an instance of the Status class to be used in case of
     * asynchronous call. It is supposed to be created by the underlying
     * infrastructure only when it was successful in allocating the asynchronous
     * request id, hence caller should expect StatusCode to be successful.
     *
     * @param errorCode
     *            The status code. If passed as null, code will be stored as
     *            {@code StatusCode.UNDEFINED}
     * @param requestId
     *            The request id set by underlying infrastructure for this
     *            request
     */
    public Status(StatusCode errorCode, long requestId) {
        this.code = (errorCode != null) ? errorCode : StatusCode.UNDEFINED;
        this.description = (description != null) ? description : this.code
                .toString();
        this.requestId = requestId;
    }

    /**
     * Returns the status code
     *
     * @return the {@code StatusCode} representing the status code
     */
    public StatusCode getCode() {
        return code;
    }

    /**
     * Returns a human readable description of the failure if any
     *
     * @return a string representing the reason of failure
     */
    public String getDescription() {
        return description;
    }

    /**
     * Tells whether the status is successful
     *
     * @return true if the Status code is {@code StatusCode.SUCCESS}
     */
    public boolean isSuccess() {
        return code == StatusCode.SUCCESS || code == StatusCode.CREATED;
    }

    /**
     * Return the request id assigned by underlying infrastructure in case of
     * asynchronous request. In case of synchronous requests, the returned id
     * is expected to be 0
     *
     * @return The request id assigned for this asynchronous request
     */
    public long getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return code + ": " + description + " (" + requestId + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.calculateConsistentHashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        Status other = (Status) obj;
        if (code != other.code) {
            return false;
        }
        return true;
    }
}
