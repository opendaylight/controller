/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;

/**
 * Unit tests for RestconfError.
 *
 * @author Devin Avery
 *
 */
public class RestconfErrorTest {

    @Test
    public void testErrorTagValueOf()
    {
        assertEquals( ErrorTag.IN_USE,
                ErrorTag.valueOfCaseInsensitive( ErrorTag.IN_USE.getTagValue() ) );
    }

    @Test
    public void testErrorTagValueOfIsLowercase()
    {
        assertEquals( "in-use",
                ErrorTag.IN_USE.getTagValue() );
    }

    @Test
    public void testErrorTypeGetErrorTypeTagIsLowerCase()
    {
       assertEquals( ErrorType.APPLICATION.name().toLowerCase(),
               ErrorType.APPLICATION.getErrorTypeTag() );
    }

    @Test
    public void testErrorTypeValueOf()
    {
       assertEquals( ErrorType.APPLICATION,
                     ErrorType.valueOfCaseInsensitive( ErrorType.APPLICATION.getErrorTypeTag() ) );
    }

    @Test
    public void testErrorTagStatusCodes()
    {
        Map<String,Status> lookUpMap = new HashMap<String,Status>();

        lookUpMap.put( "in-use", Status.fromStatusCode(409));
        lookUpMap.put( "invalid-value", Status.fromStatusCode(400));
        lookUpMap.put( "too-big", Status.fromStatusCode(413));
        lookUpMap.put( "missing-attribute", Status.fromStatusCode(400));
        lookUpMap.put( "bad-attribute", Status.fromStatusCode(400));
        lookUpMap.put( "unknown-attribute", Status.fromStatusCode(400));
        lookUpMap.put( "bad-element", Status.fromStatusCode(400));
        lookUpMap.put( "unknown-element", Status.fromStatusCode(400));
        lookUpMap.put( "unknown-namespace", Status.fromStatusCode(400));
        lookUpMap.put( "access-denied", Status.fromStatusCode(403));
        lookUpMap.put( "lock-denied", Status.fromStatusCode(409));
        lookUpMap.put( "resource-denied", Status.fromStatusCode(409));
        lookUpMap.put( "rollback-failed", Status.fromStatusCode(500));
        lookUpMap.put( "data-exists", Status.fromStatusCode(409));
        lookUpMap.put( "data-missing", Status.fromStatusCode(409));
        lookUpMap.put( "operation-not-supported", Status.fromStatusCode(501));
        lookUpMap.put( "operation-failed", Status.fromStatusCode(500));
        lookUpMap.put( "partial-operation", Status.fromStatusCode(500));
        lookUpMap.put( "malformed-message", Status.fromStatusCode(400));

        for( ErrorTag tag : ErrorTag.values() )
        {
            Status expectedStatusCode = lookUpMap.get( tag.getTagValue() );
            assertNotNull( "Failed to find " + tag.getTagValue(), expectedStatusCode );
            assertEquals( "Status Code does not match", expectedStatusCode, tag.getStatusCode() );
        }
    }

    @Test
    public void testRestConfDocumentedException_NoCause()
    {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        RestconfError e =
                new RestconfError( expectedErrorType,
                                                 expectedErrorTag, expectedMessage );

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag,
                null, null, e);
    }

    @Test
    public void testRestConfDocumentedException_WithAppTag()
    {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        String expectedErrorAppTag = "application.tag";

        RestconfError e =
                new RestconfError( expectedErrorType,
                                                 expectedErrorTag, expectedMessage, expectedErrorAppTag );

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag,
                expectedErrorAppTag, null, e);
    }

    @Test
    public void testRestConfDocumentedException_WithAppTagErrorInfo()
    {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        String expectedErrorAppTag = "application.tag";
        String errorInfo = "<extra><sessionid>session.id</sessionid></extra>";

        RestconfError e = new RestconfError( expectedErrorType,
                                             expectedErrorTag,
                                             expectedMessage,
                                             expectedErrorAppTag,
                                             errorInfo );

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag,
                expectedErrorAppTag, errorInfo, e);
    }

    private void validateRestConfError(String expectedMessage, ErrorType expectedErrorType,
            ErrorTag expectedErrorTag, String expectedErrorAppTag, String errorInfo, RestconfError e) {
        assertEquals( expectedMessage, e.getErrorMessage() );
        assertEquals( expectedErrorType, e.getErrorType() );
        assertEquals( expectedErrorTag, e.getErrorTag() );
        assertEquals( expectedErrorAppTag, e.getErrorAppTag() );
        assertEquals( errorInfo, e.getErrorInfo() );
        e.toString(); // really just checking for NPE etc. Don't care about contents.
    }
}
