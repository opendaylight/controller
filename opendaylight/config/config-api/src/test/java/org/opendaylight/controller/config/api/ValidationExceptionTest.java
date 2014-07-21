package org.opendaylight.controller.config.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Test;

public class ValidationExceptionTest {

    private String instance = "instance";
    private final ModuleIdentifier mi = new ModuleIdentifier("module", instance);
    private String instance2 = "instance2";
    private final ModuleIdentifier mi2 = new ModuleIdentifier("module", instance2);

    @Test
    public void testCreateFromCollectedValidationExceptions() throws Exception {
        String message = "ex message";
        final Exception e = new IllegalStateException(message);

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
}