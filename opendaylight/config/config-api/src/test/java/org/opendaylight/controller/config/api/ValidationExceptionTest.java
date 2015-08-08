/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ValidationExceptionTest {

    private final String instance = "instance";
    private final ModuleIdentifier mi = new ModuleIdentifier("module", instance);
    private final String instance2 = "instance2";
    private final ModuleIdentifier mi2 = new ModuleIdentifier("module", instance2);
    private final String message = "ex message";
    private final Exception e = new IllegalStateException(message);

    @Test
    public void testCreateFromCollectedValidationExceptions() throws Exception {
        ValidationException single = ValidationException.createForSingleException(mi, e);
        ValidationException single2 = ValidationException.createForSingleException(mi2, e);

        ValidationException collected = ValidationException.createFromCollectedValidationExceptions(Lists.newArrayList(single, single2));

        Map<String, Map<String, ValidationException.ExceptionMessageWithStackTrace>> failedMap = collected.getFailedValidations();
        assertEquals(1, failedMap.size());
        assertTrue(failedMap.containsKey("module"));

        Map<String, ValidationException.ExceptionMessageWithStackTrace> failedModule = failedMap.get("module");
        assertEquals(2, failedModule.size());
        assertTrue(failedModule.containsKey(instance));
        assertEquals(message, failedModule.get(instance).getMessage());
        assertEquals(message, failedModule.get(instance2).getMessage());
        assertEquals(failedModule.get(instance), failedModule.get(instance2));
    }

    @Test
    public void testCreateFromCollectedValidationExceptionsWithDuplicate() throws Exception {
        ValidationException single = ValidationException.createForSingleException(mi, e);
        ValidationException single2 = ValidationException.createForSingleException(mi, e);
        try {
            ValidationException.createFromCollectedValidationExceptions(Lists.newArrayList(single, single2));
        } catch (IllegalArgumentException ex) {
            // Duplicate exception
            assertThat(ex.getMessage(), containsString("Cannot merge"));
            return;
        }
        fail("Duplicate exception should have failed");
    }

    @Test
    public void testGetTrace() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp = new ValidationException.ExceptionMessageWithStackTrace();
        exp.setTrace("trace");
        Assert.assertEquals(exp.getTrace(), "trace");
    }

    @Test
    public void testSetMessage() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp = new ValidationException.ExceptionMessageWithStackTrace();
        exp.setMessage("message");
        Assert.assertEquals(exp.getMessage(), "message");
    }

    @Test
    public void testHashCode() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp = new ValidationException.ExceptionMessageWithStackTrace();
        Assert.assertEquals(exp.hashCode(), new ValidationException.ExceptionMessageWithStackTrace().hashCode());
    }

    @Test
    public void testExceptionMessageWithStackTraceConstructor() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertEquals(exp, exp);
    }

    @Test
    public void testExceptionMessageWithStackTraceConstructor2() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertNotEquals(exp, null);
    }

    @Test
    public void testExceptionMessageWithStackTraceConstructor3() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertNotEquals(exp, new Exception());
    }

    @Test
    public void testExceptionMessageWithStackTraceConstructor4() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertEquals(exp, new ValidationException.ExceptionMessageWithStackTrace("string1", "string2"));
    }

    @Test
    public void testEqual() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace(null, "string2");
        Assert.assertNotEquals(exp, exp2);
    }

    @Test
    public void testEqual2() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace("different", "string2");
        Assert.assertNotEquals(exp, exp2);
    }


    @Test
    public void testEqual3() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace("string1", null);
        Assert.assertNotEquals(exp, exp2);
    }

    @Test
    public void testEqual4() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "different");
        Assert.assertNotEquals(exp, exp2);
    }

    @Test
    public void testEqual5() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace(null, "string2");
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertNotEquals(exp, exp2);
    }

    @Test
    public void testEqual6() throws Exception {
        ValidationException.ExceptionMessageWithStackTrace exp =
                new ValidationException.ExceptionMessageWithStackTrace("string1", null);
        ValidationException.ExceptionMessageWithStackTrace exp2 =
                new ValidationException.ExceptionMessageWithStackTrace("string1", "string2");
        Assert.assertNotEquals(exp, exp2);
    }
}