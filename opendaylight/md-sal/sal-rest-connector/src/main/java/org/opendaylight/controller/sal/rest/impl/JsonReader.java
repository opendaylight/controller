package org.opendaylight.controller.sal.rest.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.EmptyNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

class JsonReader {

    public CompositeNodeWrapper read(InputStream entityStream) throws UnsupportedFormatException {
        JsonParser parser = new JsonParser();

        JsonElement rootElement = parser.parse(new InputStreamReader(entityStream));
        if (!rootElement.isJsonObject()) {
            throw new UnsupportedFormatException("Root element of Json has to be Object");
        }

        Set<Entry<String, JsonElement>> entrySetsOfRootJsonObject = rootElement.getAsJsonObject().entrySet();
        if (entrySetsOfRootJsonObject.size() != 1) {
            throw new UnsupportedFormatException("Json Object should contain one element");
        } else {
            Entry<String, JsonElement> childEntry = Lists.newArrayList(entrySetsOfRootJsonObject).get(0);
            String firstElementName = childEntry.getKey();
            JsonElement firstElementType = childEntry.getValue();
            if (firstElementType.isJsonObject()) { // container in yang
                return createStructureWithRoot(firstElementName, firstElementType.getAsJsonObject());
            }
            if (firstElementType.isJsonArray()) { // list in yang
                if (firstElementType.getAsJsonArray().size() == 1) {
                    JsonElement firstElementInArray = firstElementType.getAsJsonArray().get(0);
                    if (firstElementInArray.isJsonObject()) {
                        return createStructureWithRoot(firstElementName, firstElementInArray.getAsJsonObject());
                    }
                    throw new UnsupportedFormatException(
                            "Array as the first element in Json Object can have only Object element");
                }
            }
            throw new UnsupportedFormatException(
                    "First element in Json Object has to be \"Object\" or \"Array with one Object element\". Other scenarios are not supported yet.");
        }
    }

    private CompositeNodeWrapper createStructureWithRoot(String rootObjectName, JsonObject rootObject) {
        CompositeNodeWrapper firstNode = new CompositeNodeWrapper(getNamespaceFor(rootObjectName),
                getLocalNameFor(rootObjectName));
        for (Entry<String, JsonElement> childOfFirstNode : rootObject.entrySet()) {
            addChildToParent(childOfFirstNode.getKey(), childOfFirstNode.getValue(), firstNode);
        }
        return firstNode;
    }

    private void addChildToParent(String childName, JsonElement childType, CompositeNodeWrapper parent) {
        if (childType.isJsonObject()) {
            CompositeNodeWrapper child = new CompositeNodeWrapper(getNamespaceFor(childName),
                    getLocalNameFor(childName));
            parent.addValue(child);
            for (Entry<String, JsonElement> childOfChild : childType.getAsJsonObject().entrySet()) {
                addChildToParent(childOfChild.getKey(), childOfChild.getValue(), child);
            }
        } else if (childType.isJsonArray()) {
            if (childType.getAsJsonArray().size() == 1 && childType.getAsJsonArray().get(0).isJsonNull()) {
                parent.addValue(new EmptyNodeWrapper(getNamespaceFor(childName), getLocalNameFor(childName)));

            } else {
                for (JsonElement childOfChildType : childType.getAsJsonArray()) {
                    addChildToParent(childName, childOfChildType, parent);
                }
            }
        } else if (childType.isJsonPrimitive()) {
            JsonPrimitive childPrimitive = childType.getAsJsonPrimitive();
            String value = childPrimitive.getAsString();
            parent.addValue(new SimpleNodeWrapper(getNamespaceFor(childName), getLocalNameFor(childName),
                    resolveValueOfElement(value)));
        }
    }

    private URI getNamespaceFor(String jsonElementName) {
        String[] moduleNameAndLocalName = jsonElementName.split(":");
        if (moduleNameAndLocalName.length != 2) { // it is not "moduleName:localName"
            return null;
        }
        return URI.create(moduleNameAndLocalName[0]);
    }

    private String getLocalNameFor(String jsonElementName) {
        String[] moduleNameAndLocalName = jsonElementName.split(":");
        if (moduleNameAndLocalName.length != 2) { // it is not "moduleName:localName"
            return jsonElementName;
        }
        return moduleNameAndLocalName[1];
    }

    /**
     * @param value
     *            value of json element
     * @return if value is "moduleName:localName" then {@link IdentityValuesDTO} else
     *         the same string as parameter "value"
     */
    private Object resolveValueOfElement(String value) {
        URI namespace = getNamespaceFor(value);
        return namespace == null ? value : new IdentityValuesDTO(namespace.toString(), getLocalNameFor(value), null);
    }

}
