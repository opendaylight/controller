/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class JsonParser {
    public JsonElement parse(JsonReader reader) throws JsonIOException, JsonSyntaxException {
        // code copied from gson's JsonParser and Stream classes

        boolean lenient = reader.isLenient();
        reader.setLenient(true);
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return read(reader);
        } catch (EOFException e) {
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            // The stream ended prematurely so it is likely a syntax error.
            throw new JsonSyntaxException(e);
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (StackOverflowError | OutOfMemoryError e) {
            throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
        } finally {
            reader.setLenient(lenient);
        }
    }

    public JsonElement read(JsonReader in) throws IOException {
        final Set<String> equalNames = new HashSet<>();
        switch (in.peek()) {
        case STRING:
            return new JsonPrimitive(in.nextString());
        case NUMBER:
            String number = in.nextString();
            return new JsonPrimitive(new LazilyParsedNumber(number));
        case BOOLEAN:
            return new JsonPrimitive(in.nextBoolean());
        case NULL:
            in.nextNull();
            return JsonNull.INSTANCE;
        case BEGIN_ARRAY:
            JsonArray array = new JsonArray();
            in.beginArray();
            while (in.hasNext()) {
                array.add(read(in));
            }
            in.endArray();
            return array;
        case BEGIN_OBJECT:
            JsonObject object = new JsonObject();
            in.beginObject();
            while (in.hasNext()) {
                final String name = in.nextName();
                object.add(name, read(in));
                if (!equalNames.add(name)) {
                    throw new IllegalStateException("Duplicate name " + name + " in JSON input.");
                }
            }
            in.endObject();
            return object;
        case END_DOCUMENT:
        case NAME:
        case END_OBJECT:
        case END_ARRAY:
        default:
            throw new IllegalArgumentException();
        }
    }
}
