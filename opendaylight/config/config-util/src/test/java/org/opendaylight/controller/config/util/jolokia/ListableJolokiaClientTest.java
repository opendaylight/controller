/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util.jolokia;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.ValidationException.ExceptionMessageWithStackTrace;

public class ListableJolokiaClientTest {

    private Map<String, Map<String, ExceptionMessageWithStackTrace>> failedValidations;

    private ValidationException val;

    private final static String ex = "{\"message\":null,"
            + "\"failedValidations\":"
            + "{\"ifc2\":{\"impl1\":{\"message\":\"abc\",\"trace\":\"vvv\"},"
            + "\"impl2\":{\"message\":\"abc2\",\"trace\":\"vvv2\"}},"
            + "\"ifc1\":"
            + "{\"impl1\":{\"message\":\"abc\",\"trace\":\"vvv\"},"
            + "\"impl2\":{\"message\":\"abc2\",\"trace\":\"vvv2\"}}},"
            + "\"localizedMessage\":null," + "\"cause\":null}";

    @Before
    public void setUp() {
        failedValidations = new HashMap<String, Map<String, ExceptionMessageWithStackTrace>>();
        Map<String, ExceptionMessageWithStackTrace> map1 = new HashMap<String, ValidationException.ExceptionMessageWithStackTrace>();
        map1.put("impl1", new ExceptionMessageWithStackTrace("abc", "vvv"));
        map1.put("impl2", new ExceptionMessageWithStackTrace("abc2", "vvv2"));
        failedValidations.put("ifc1", map1);
        failedValidations.put("ifc2", map1);
        val = new ValidationException(failedValidations);
    }

    @Test
    public void testParsing() {
        JSONObject e = (JSONObject) JSONValue.parse(ex);
        ValidationException val2 = ListableJolokiaClient
                .createValidationExceptionFromJSONObject(e);
        assertThat(val2.getMessage(), is(val.getMessage()));
        assertThat(val2.getFailedValidations(), is(val.getFailedValidations()));
    }

}
