/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;

/**
 * Unchecked exception to communicate error information, as defined in the ietf restcong draft, to be sent to the
 * client.
 *
 * @author Devin Avery
 * @author Thomas Pantelis
 * @see {@link https://tools.ietf.org/html/draft-bierman-netconf-restconf-02}
 */
public class RestconfDocumentedException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    private final List<RestconfError> errors;
    private final Status status;

    /**
     * Constructs an instance with an error message. The error type defaults to APPLICATION and the error tag defaults
     * to OPERATION_FAILED.
     *
     * @param message
     *            A string which provides a plain text string describing the error.
     */
    public RestconfDocumentedException(String message) {
        this(message, RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED);
    }

    /**
     * Constructs an instance with an error message, error type, and error tag.
     *
     * @param message
     *            A string which provides a plain text string describing the error.
     * @param errorType
     *            The enumerated type indicating the layer where the error occurred.
     * @param errorTag
     *            The enumerated tag representing a more specific error cause.
     */
    public RestconfDocumentedException(String message, ErrorType errorType, ErrorTag errorTag) {
        this(null, new RestconfError(errorType, errorTag, message));
    }

    /**
     * Constructs an instance with an error message and exception cause.
     * The stack trace of the exception is included in the error info.
     *
     * @param message
     *            A string which provides a plain text string describing the error.
     * @param cause
     *            The underlying exception cause.
     */
    public RestconfDocumentedException(String message, Throwable cause) {
        this(cause, new RestconfError(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED,
                message, null, RestconfError.toErrorInfo(cause)));
    }

    /**
     * Constructs an instance with the given error.
     */
    public RestconfDocumentedException(RestconfError error) {
        this(null, error);
    }

    /**
     * Constructs an instance with the given errors.
     */
    public RestconfDocumentedException(String message, Throwable cause, List<RestconfError> errors) {
        // FIXME: We override getMessage so supplied message is lost for any public access
        // this was lost also in original code.
        super(cause);
        if(!errors.isEmpty()) {
            this.errors = ImmutableList.copyOf(errors);
        } else {
            this.errors = ImmutableList.of(new RestconfError(RestconfError.ErrorType.APPLICATION,
                    RestconfError.ErrorTag.OPERATION_FAILED, message));
        }

        status = null;
    }

    /**
     * Constructs an instance with the given RpcErrors.
     */
    public RestconfDocumentedException(String message, Throwable cause, Collection<RpcError> rpcErrors) {
        this(message, cause, convertToRestconfErrors(rpcErrors));
    }

    /**
     * Constructs an instance with an HTTP status and no error information.
     *
     * @param status
     *            the HTTP status.
     */
    public RestconfDocumentedException(Status status) {
        Preconditions.checkNotNull(status, "Status can't be null");
        errors = ImmutableList.of();
        this.status = status;
    }

    private RestconfDocumentedException(Throwable cause, RestconfError error) {
        super(cause);
        Preconditions.checkNotNull(error, "RestconfError can't be null");
        errors = ImmutableList.of(error);
        status = null;
    }

    private static List<RestconfError> convertToRestconfErrors(Collection<RpcError> rpcErrors) {
        List<RestconfError> errorList = Lists.newArrayList();
        if(rpcErrors != null) {
            for (RpcError rpcError : rpcErrors) {
                errorList.add(new RestconfError(rpcError));
            }
        }

        return errorList;
    }


    public List<RestconfError> getErrors() {
        return errors;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return "errors: " + errors + (status != null ? ", status: " + status : "");
    }
}
