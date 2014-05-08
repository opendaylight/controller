/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sal.restconf.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response.Status;

import org.opendaylight.yangtools.yang.common.RpcError;

import com.google.common.base.Preconditions;

/**
 * Encapsulates a restconf error as defined in the ietf restconf draft.
 *
 * <br><br><b>Note:</b> Enumerations defined within are provided by the ietf restconf draft.
 *
 * @author Devin Avery
 * @see {@link https://tools.ietf.org/html/draft-bierman-netconf-restconf-02}
 */
public class RestconfError {

    public static enum ErrorType {
        /** Errors relating to the transport layer */
        TRANSPORT,
        /** Errors relating to the RPC or notification layer */
        RPC,
        /** Errors relating to the protocol operation layer. */
        PROTOCOL,
        /** Errors relating to the server application layer. */
        APPLICATION;

        public String getErrorTypeTag() {
            return name().toLowerCase();
        }

        public static ErrorType valueOfCaseInsensitive( String value )
        {
            try {
                return ErrorType.valueOf( ErrorType.class, value.toUpperCase() );
            }
            catch( IllegalArgumentException e ) {
                return APPLICATION;
            }
        }
    }

    public static enum ErrorTag {
        IN_USE( "in-use", Status.fromStatusCode(409)),
        INVALID_VALUE( "invalid-value", Status.fromStatusCode(400)),
        TOO_BIG( "too-big", Status.fromStatusCode(413)),
        MISSING_ATTRIBUTE( "missing-attribute", Status.fromStatusCode(400)),
        BAD_ATTRIBUTE( "bad-attribute", Status.fromStatusCode(400)),
        UNKNOWN_ATTRIBUTE( "unknown-attribute", Status.fromStatusCode(400)),
        BAD_ELEMENT( "bad-element", Status.fromStatusCode(400)),
        UNKNOWN_ELEMENT( "unknown-element", Status.fromStatusCode(400)),
        UNKNOWN_NAMESPACE( "unknown-namespace", Status.fromStatusCode(400)),
        ACCESS_DENIED( "access-denied", Status.fromStatusCode(403)),
        LOCK_DENIED( "lock-denied", Status.fromStatusCode(409)),
        RESOURCE_DENIED( "resource-denied", Status.fromStatusCode(409)),
        ROLLBACK_FAILED( "rollback-failed", Status.fromStatusCode(500)),
        DATA_EXISTS( "data-exists", Status.fromStatusCode(409)),
        DATA_MISSING( "data-missing", Status.fromStatusCode(409)),
        OPERATION_NOT_SUPPORTED( "operation-not-supported", Status.fromStatusCode(501)),
        OPERATION_FAILED( "operation-failed", Status.fromStatusCode(500)),
        PARTIAL_OPERATION( "partial-operation", Status.fromStatusCode(500)),
        MALFORMED_MESSAGE( "malformed-message", Status.fromStatusCode(400));

        private final String tagValue;
        private final Status statusCode;

        ErrorTag(final String tagValue, final Status statusCode) {
            this.tagValue = tagValue;
            this.statusCode = statusCode;
        }

        public String getTagValue() {
            return this.tagValue.toLowerCase();
        }

        public static ErrorTag valueOfCaseInsensitive( String value )
        {
            try {
                return ErrorTag.valueOf( ErrorTag.class, value.toUpperCase().replaceAll( "-","_" ) );
            }
            catch( IllegalArgumentException e ) {
                return OPERATION_FAILED;
            }
        }

        public Status getStatusCode() {
            return statusCode;
        }
    }

    private final ErrorType errorType;
    private final ErrorTag errorTag;
    private final String errorInfo;
    private final String errorAppTag;
    private final String errorMessage;
    //TODO: Add in the error-path concept as defined in the ietf draft.

    static String toErrorInfo( Throwable cause ) {
        StringWriter writer = new StringWriter();
        cause.printStackTrace( new PrintWriter( writer ) );
        return writer.toString();
    }

    /**
     * Constructs a RestConfError
     *
     * @param errorType The enumerated type indicating the layer where the error occurred.
     * @param errorTag The enumerated tag representing a more specific error cause.
     * @param errorMessage A string which provides a plain text string describing the error.
     */
    public RestconfError(ErrorType errorType, ErrorTag errorTag, String errorMessage) {
        this( errorType, errorTag, errorMessage, null );
    }

    /**
     * Constructs a RestConfError object.
     *
     * @param errorType The enumerated type indicating the layer where the error occurred.
     * @param errorTag The enumerated tag representing a more specific error cause.
     * @param errorMessage A string which provides a plain text string describing the error.
     * @param errorAppTag A string which represents an application-specific error tag that further
     *                    specifies the error cause.
     */
    public RestconfError(ErrorType errorType, ErrorTag errorTag, String errorMessage,
                         String errorAppTag) {
        this( errorType, errorTag, errorMessage, errorAppTag, null );
    }

    /**
     * Constructs a RestConfError object.
     *
     * @param errorType The enumerated type indicating the layer where the error occurred.
     * @param errorTag The enumerated tag representing a more specific error cause.
     * @param errorMessage A string which provides a plain text string describing the error.
     * @param errorAppTag A string which represents an application-specific error tag that further
     *                    specifies the error cause.
     * @param errorInfo A string, <b>formatted as XML</b>, which contains additional error information.
     */
    public RestconfError(ErrorType errorType, ErrorTag errorTag, String errorMessage,
                         String errorAppTag, String errorInfo) {
        Preconditions.checkNotNull( errorType, "Error type is required for RestConfError" );
        Preconditions.checkNotNull( errorTag, "Error tag is required for RestConfError");
        this.errorType = errorType;
        this.errorTag = errorTag;
        this.errorMessage = errorMessage;
        this.errorAppTag = errorAppTag;
        this.errorInfo = errorInfo;
    }

    /**
     * Constructs a RestConfError object from an RpcError.
     */
    public RestconfError( RpcError rpcError ) {

        this.errorType = rpcError.getErrorType() == null ? ErrorType.APPLICATION :
                               ErrorType.valueOfCaseInsensitive( rpcError.getErrorType().name() );

        this.errorTag = rpcError.getTag() == null ? ErrorTag.OPERATION_FAILED :
                                    ErrorTag.valueOfCaseInsensitive( rpcError.getTag().toString() );

        this.errorMessage = rpcError.getMessage();
        this.errorAppTag = rpcError.getApplicationTag();

        String errorInfo = null;
        if( rpcError.getInfo() == null ) {
            if( rpcError.getCause() != null ) {
                errorInfo = toErrorInfo( rpcError.getCause() );
            }
            else if( rpcError.getSeverity() != null ) {
                errorInfo = "<severity>" + rpcError.getSeverity().toString().toLowerCase() +
                            "</severity>";
            }
        }
        else {
            errorInfo = rpcError.getInfo();
        }

        this.errorInfo = errorInfo;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public ErrorTag getErrorTag() {
        return errorTag;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public String getErrorAppTag() {
        return errorAppTag;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "error-type: " + errorType.getErrorTypeTag()
                + ", error-tag: " + errorTag.getTagValue() + ", "
                + (errorAppTag != null ? "error-app-tag: " + errorAppTag + ", " : "")
                + (errorMessage != null ? "error-message: " + errorMessage : "")
                + (errorInfo != null ? "error-info: " + errorInfo + ", " : "") + "]";
    }

}