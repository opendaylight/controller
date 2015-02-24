/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.sal.rest.gson.JsonParser;
import org.opendaylight.controller.sal.rest.impl.RestUtil.PrefixMapingFromJson;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.EmptyNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated class will be removed in Lithium release
 */
@Deprecated
class JsonToCompositeNodeReader {
    private static final Logger LOG = LoggerFactory.getLogger(JsonToCompositeNodeReader.class);
    private static final Splitter COLON_SPLITTER = Splitter.on(':');

    private JsonToCompositeNodeReader() {

    }

    public static CompositeNodeWrapper read(final InputStream entityStream) throws UnsupportedFormatException {
        final JsonParser parser = new JsonParser();

        final JsonElement rootElement = parser.parse(new JsonReader(new InputStreamReader(entityStream)));
        if (rootElement.isJsonNull()) {
            // no content, so return null to indicate no input
            return null;
        }

        if (!rootElement.isJsonObject()) {
            throw new UnsupportedFormatException("Root element of Json has to be Object");
        }

        final Set<Entry<String, JsonElement>> entrySetsOfRootJsonObject = rootElement.getAsJsonObject().entrySet();
        if (entrySetsOfRootJsonObject.size() != 1) {
            throw new UnsupportedFormatException("Json Object should contain one element");
        }

        final Entry<String, JsonElement> childEntry = entrySetsOfRootJsonObject.iterator().next();
        final String firstElementName = childEntry.getKey();
        final JsonElement firstElementType = childEntry.getValue();
        if (firstElementType.isJsonObject()) {
            // container in yang
            return createStructureWithRoot(firstElementName, firstElementType.getAsJsonObject());
        }
        if (firstElementType.isJsonArray()) {
            // list in yang
            if (firstElementType.getAsJsonArray().size() == 1) {
                final JsonElement firstElementInArray = firstElementType.getAsJsonArray().get(0);
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

    private static CompositeNodeWrapper createStructureWithRoot(final String rootObjectName, final JsonObject rootObject) {
        final CompositeNodeWrapper firstNode = new CompositeNodeWrapper(getNamespaceFor(rootObjectName),
                getLocalNameFor(rootObjectName));
        for (final Entry<String, JsonElement> childOfFirstNode : rootObject.entrySet()) {
            addChildToParent(childOfFirstNode.getKey(), childOfFirstNode.getValue(), firstNode);
        }
        return firstNode;
    }

    private static void addChildToParent(final String childName, final JsonElement childType,
            final CompositeNodeWrapper parent) {
        if (childType.isJsonObject()) {
            final CompositeNodeWrapper child = new CompositeNodeWrapper(getNamespaceFor(childName),
                    getLocalNameFor(childName));
            parent.addValue(child);
            for (final Entry<String, JsonElement> childOfChild : childType.getAsJsonObject().entrySet()) {
                addChildToParent(childOfChild.getKey(), childOfChild.getValue(), child);
            }
        } else if (childType.isJsonArray()) {
            if (childType.getAsJsonArray().size() == 1 && childType.getAsJsonArray().get(0).isJsonNull()) {
                parent.addValue(new EmptyNodeWrapper(getNamespaceFor(childName), getLocalNameFor(childName)));

            } else {
                for (final JsonElement childOfChildType : childType.getAsJsonArray()) {
                    addChildToParent(childName, childOfChildType, parent);
                }
            }
        } else if (childType.isJsonPrimitive()) {
            final JsonPrimitive childPrimitive = childType.getAsJsonPrimitive();
            final String value = childPrimitive.getAsString().trim();
            parent.addValue(new SimpleNodeWrapper(getNamespaceFor(childName), getLocalNameFor(childName),
                    resolveValueOfElement(value)));
        } else {
            LOG.debug("Ignoring unhandled child type {}", childType);
        }
    }

    /**
     * Transform input value to URI instance.
     *
     * Input string has to be in format moduleName:localName. moduleName part is then transformed to URI instance.
     * If moduleName part contains character like "<" or ">" then null value is returned because they
     * aren't valid URI characters.
     *
     * @param jsonElementName
     *  value in format moduleName:localName
     * @return
     */
    private static URI getNamespaceFor(final String jsonElementName) {
        final Iterator<String> it = COLON_SPLITTER.split(jsonElementName).iterator();

        // The string needs to be in form "moduleName:localName"
        if (it.hasNext()) {
            final String maybeURI = it.next();
            if (Iterators.size(it) == 1) {
                try {
                    return URI.create(maybeURI);
                } catch (final IllegalArgumentException e) {
                    LOG.debug("Value {} couldn't be interpreted as URI.", maybeURI);
                }
            }
        }

        return null;
    }

    private static String getLocalNameFor(final String jsonElementName) {
        final Iterator<String> it = COLON_SPLITTER.split(jsonElementName).iterator();

        // The string needs to me in form "moduleName:localName"
        final String ret = Iterators.get(it, 1, null);
        return ret != null && !it.hasNext() ? ret : jsonElementName;
    }

    private static Object resolveValueOfElement(final String value) {
        // it could be instance-identifier Built-In Type
        if (!value.isEmpty() && value.charAt(0) == '/') {
            final IdentityValuesDTO resolvedValue = RestUtil.asInstanceIdentifier(value, new PrefixMapingFromJson());
            if (resolvedValue != null) {
                return resolvedValue;
            }
        }

        // it could be identityref Built-In Type therefore it is necessary to look at value as module_name:local_name
        final URI namespace = getNamespaceFor(value);
        if (namespace != null) {
            return new IdentityValuesDTO(namespace.toString(), getLocalNameFor(value), null, value);
        }

        // it is not "prefix:value" but just "value"
        return value;
    }

}
