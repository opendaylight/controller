/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Strings;
import javax.ws.rs.core.UriInfo;

public class QueryParametersParser {

    private enum UriParameters {
        PRETTY_PRINT("prettyPrint"),
        DEPTH("depth");

        private String uriParameterName;

        UriParameters(final String uriParameterName) {
            this.uriParameterName = uriParameterName;
        }

        @Override
        public String toString() {
            return uriParameterName;
        }
    }

    public static WriterParameters parseKnownWriterParameters(final UriInfo info) {
        boolean prettyPrint;
        int depth;
        String param = info.getQueryParameters(false).getFirst(UriParameters.DEPTH.toString());
        if (Strings.isNullOrEmpty(param) || "unbounded".equals(param)) {
            depth = Integer.MAX_VALUE;
        } else {
            try {
                depth = Integer.valueOf(param);
                if (depth < 1) {
                    throw new RestconfDocumentedException(new RestconfError(RestconfError.ErrorType.PROTOCOL, RestconfError.ErrorTag.INVALID_VALUE,
                            "Invalid depth parameter: " + depth, null,
                            "The depth parameter must be an integer > 1 or \"unbounded\""));
                }
            } catch (final NumberFormatException e) {
                throw new RestconfDocumentedException(new RestconfError(RestconfError.ErrorType.PROTOCOL, RestconfError.ErrorTag.INVALID_VALUE,
                        "Invalid depth parameter: " + e.getMessage(), null,
                        "The depth parameter must be an integer > 1 or \"unbounded\""));
            }
        }
        param = info.getQueryParameters(false).getFirst(UriParameters.PRETTY_PRINT.toString());
        prettyPrint = "true".equals(param);
        return new WriterParameters(prettyPrint, depth);
    }

}
