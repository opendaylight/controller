/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 5, 2015
 */
public class RestconfUriUtils {

    private RestconfUriUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Map<String, String> resolveValuesFromUri(final String uri) {
        final Map<String, String> result = new HashMap<>();
        final String[] tokens = uri.split("/");
        for (int i = 1; i < tokens.length; i++) {
            final String[] parameterTokens = tokens[i].split("=");
            if (parameterTokens.length == 2) {
                result.put(parameterTokens[0], parameterTokens[1]);
            }
        }
        return result;
    }

    public static boolean parsePrettyPrintParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false)
                .getFirst(RestconfInternalConstants.URI_PARAM_PRETTY_PRINT);
        return Boolean.parseBoolean(param);
    }

    public Integer parseDepthParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false).getFirst(RestconfInternalConstants.URI_PARAM_DEPTH);
        if (Strings.isNullOrEmpty(param) || "unbounded".equals(param)) {
            return null;
        }

        try {
            final Integer depth = Integer.valueOf(param);
            RestconfValidationUtils.checkDocumentedError(depth > 1, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "The depth parameter must be an integer > 1 or \"unbounded\"");
            return depth;
        } catch (final NumberFormatException e) {
            throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "Invalid depth parameter: " + e.getMessage(), null,
                    "The depth parameter must be an integer > 1 or \"unbounded\""));
        }
    }

    /**
     * Checks whether {@code value} is one of the string representation of enumeration {@code classDescriptor}
     *
     * @return enum object if string value of {@code classDescriptor} enumeration is equal to {@code value}. Other cases
     *         null.
     */
    public static <T> T parserURIEnumParameter(final Class<T> classDescriptor, final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        return resolveAsEnum(classDescriptor, value);
    }

    private static <T> T resolveAsEnum(final Class<T> classDescriptor, final String value) {
        final T[] enumConstants = classDescriptor.getEnumConstants();
        if (enumConstants != null) {
            for (final T enm : classDescriptor.getEnumConstants()) {
                if (((Enum<?>) enm).name().equals(value)) {
                    return enm;
                }
            }
        }
        return null;
    }
}
