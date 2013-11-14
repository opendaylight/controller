/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util.jolokia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pListRequest;
import org.jolokia.client.request.J4pQueryParameter;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.ValidationException.ExceptionMessageWithStackTrace;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.AttributeEntry;

abstract class ListableJolokiaClient {
    protected final J4pClient j4pClient;
    protected final String url;
    protected final ObjectName objectName;

    public ListableJolokiaClient(String url, ObjectName objectName) {
        if (url == null) {
            throw new NullPointerException("Parameter 'url' is null");
        }
        if (!url.endsWith("/")) {
            throw new IllegalArgumentException(
                    "Parameter 'url' must end with '/'");
        }

        this.url = url;
        this.j4pClient = new J4pClient(url);
        this.objectName = objectName;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    protected <R extends J4pResponse<T>, T extends J4pRequest> R execute(
            T pRequest) {
        try {
            Map<J4pQueryParameter, String> pProcessingOptions = new HashMap<J4pQueryParameter, String>();
            pProcessingOptions
                    .put(J4pQueryParameter.INCLUDE_STACKTRACE, "true");
            pProcessingOptions.put(J4pQueryParameter.SERIALIZE_EXCEPTION,
                    "true");
            return j4pClient.execute(pRequest, "POST", pProcessingOptions);
        } catch (J4pRemoteException e) {
            tryToConvertException(e.getRemoteStackTrace(), e.getErrorValue());
            throw new RuntimeException(e.getRemoteStackTrace(), e);
        } catch (J4pException e) {
            throw new RuntimeException(e);
        }
    }

    protected void tryToConvertException(String remoteStackTrace,
            JSONObject errorValue) {
        String conflictPrefix = ConflictingVersionException.class.getName()
                + ": ";
        if (remoteStackTrace.startsWith(conflictPrefix)) {
            remoteStackTrace = remoteStackTrace.substring(conflictPrefix
                    .length());
            Pattern p = Pattern.compile("\r?\n");
            remoteStackTrace = Arrays.asList(p.split(remoteStackTrace))
                    .iterator().next();
            throw new ConflictingVersionException(remoteStackTrace);
        }
        String validationExceptionPrefix = ValidationException.class.getName();
        if (remoteStackTrace.startsWith(validationExceptionPrefix)) {
            throw createValidationExceptionFromJSONObject(errorValue);
        }
    }

    static ValidationException createValidationExceptionFromJSONObject(
            JSONObject errorValue) {
        String fValsKey = "failedValidations";
        JSONObject failedVals = (JSONObject) errorValue.get(fValsKey);

        checkArgument(
                !failedVals.isEmpty(),
                fValsKey + " was not present in received JSON: "
                        + errorValue.toJSONString());
        Map<String, Map<String, ExceptionMessageWithStackTrace>> failedValsMap = new HashMap<String, Map<String, ExceptionMessageWithStackTrace>>();

        for (Object key : failedVals.keySet()) {
            checkArgument(key instanceof String, "Unexpected key " + key
                    + ", expected instance of String");
            Map<String, ExceptionMessageWithStackTrace> innerMap = new HashMap<String, ValidationException.ExceptionMessageWithStackTrace>();
            for (Object innerKey : ((JSONObject) failedVals.get(key)).keySet()) {
                checkArgument(innerKey instanceof String, "Unexpected key "
                        + innerKey + ", expected instance of String");
                JSONObject exWithStackTraceVal = (JSONObject) (((JSONObject) failedVals
                        .get(key)).get(innerKey));
                Object mess = exWithStackTraceVal.get("message");
                Object stack = exWithStackTraceVal.get("trace");
                checkArgument(mess != null && stack != null,
                        "\"Message\" and \"trace\" elements expected in received json: "
                                + errorValue.toJSONString());
                innerMap.put(innerKey.toString(),
                        new ExceptionMessageWithStackTrace((String) mess,
                                (String) stack));
            }
            failedValsMap.put((String) key, innerMap);
        }
        return new ValidationException(failedValsMap);
    }

    private static void checkArgument(boolean b, String string) {
        if (b == false)
            throw new IllegalArgumentException(string);
    }

    public String getUrl() {
        return url;
    }

    public Map<String, AttributeEntry> getAttributes(ObjectName on) {
        J4pListRequest req = new J4pListRequest(on);
        J4pResponse<J4pListRequest> response = execute(req);
        JSONObject listJSONResponse = response.getValue();
        JSONObject attributes = (JSONObject) listJSONResponse.get("attr");

        // Empty attributes list
        if(attributes == null)
            return Collections.emptyMap();

        Map<String, JSONObject> listMap = new HashMap<>();

        for (Object entryObject : attributes.entrySet()) {
            Entry<String, Object> entry = (Entry<String, Object>) entryObject;
            JSONObject entryVal = (JSONObject) entry.getValue();

            // read value
            listMap.put(entry.getKey(), entryVal);
        }
        J4pReadRequest j4pReadRequest = new J4pReadRequest(on, listMap.keySet()
                .toArray(new String[0]));
        J4pResponse<J4pReadRequest> readResponse = execute(j4pReadRequest);
        Object readResponseValue = readResponse.getValue();
        // readResponseValue can be String if there is just one attribute or
        // JSONObject
        Map<String, Object> attribsToValues = new HashMap<String, Object>();
        if (readResponseValue instanceof JSONObject) {
            JSONObject readJSONResponse = (JSONObject) readResponseValue;
            for (Object entryObject : readJSONResponse.entrySet()) {
                Entry<String, Object> entry = (Entry<String, Object>) entryObject;
                String key = entry.getKey();
                Object value = entry.getValue();
                attribsToValues.put(key, value);
            }
        }

        Map<String, AttributeEntry> resultMap = new HashMap<String, AttributeEntry>();
        for (Entry<String, JSONObject> entry : listMap.entrySet()) {
            String key = entry.getKey();
            Object value = attribsToValues.size() > 0 ? attribsToValues
                    .get(key) : readResponseValue;
            JSONObject listJSON = entry.getValue();
            String description = (String) listJSON.get("desc");
            String type = (String) listJSON.get("type");
            boolean rw = (Boolean) listJSON.get("rw");
            AttributeEntry attributeEntry = new AttributeEntry(key,
                    description, value, type, rw);
            resultMap.put(key, attributeEntry);
        }

        return resultMap;
    }

    public String getConfigBeanDescripton(ObjectName on) {
        J4pListRequest req = new J4pListRequest(on);
        J4pResponse<J4pListRequest> response = execute(req);
        JSONObject jsonDesc = response.getValue();
        Object description = jsonDesc.get("desc");
        return description == null ? null : description.toString();
    }

    protected List<ObjectName> jsonArrayToObjectNames(JSONArray jsonArray) {
        List<ObjectName> result = new ArrayList<>(jsonArray.size());
        for (Object entry : jsonArray) {
            JSONObject jsonObject = (JSONObject) entry;
            String objectNameString = (String) jsonObject.get("objectName");
            try {
                result.add(new ObjectName(objectNameString));
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException("Cannot convert "
                        + objectNameString + " to ObjectName", e);
            }
        }
        return result;
    }

    protected ObjectName extractObjectName(J4pResponse<J4pExecRequest> resp) {
        JSONObject jsonResponse = resp.getValue();
        return extractObjectName(jsonResponse);
    }

    protected ObjectName extractObjectName(JSONObject jsonResponse) {
        String result = jsonResponse.get("objectName").toString();
        return ObjectNameUtil.createON(result);
    }

    protected Set<ObjectName> lookupSomething(String signature,
            Object[] parameters) {
        J4pExecRequest req = new J4pExecRequest(objectName, signature,
                parameters);
        JSONArray jsonArray = execute(req).getValue();
        return new HashSet<>(jsonArrayToObjectNames(jsonArray));
    }

    public Set<ObjectName> lookupConfigBeans() {
        return lookupSomething("lookupConfigBeans()", new Object[0]);
    }

    public Set<ObjectName> lookupConfigBeans(String ifcName) {
        return lookupSomething("lookupConfigBeans(java.lang.String)",
                new Object[] { ifcName });
    }

    public Set<ObjectName> lookupConfigBeans(String ifcName, String instanceName) {
        return lookupSomething(
                "lookupConfigBeans(java.lang.String,java.lang.String)",
                new Object[] { ifcName, instanceName });
    }

    public ObjectName lookupConfigBean(String ifcName, String instanceName)
            throws InstanceNotFoundException {
        J4pExecRequest req = new J4pExecRequest(objectName,
                "lookupConfigBean(java.lang.String,java.lang.String)",
                new Object[] { ifcName, instanceName });
        try {
            J4pResponse<J4pExecRequest> resp = execute(req);
            return extractObjectName(resp);
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().startsWith(
                            InstanceNotFoundException.class.getName()))
                throw new InstanceNotFoundException();
            throw e;
        }
    }

    public Set<String> getAvailableModuleNames() {
        J4pReadRequest req = new J4pReadRequest(objectName,
                "AvailableModuleNames");
        List<String> value = execute(req).getValue();
        return new HashSet<>(value);
    }
}
