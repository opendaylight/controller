/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 8, 2015
 */
public class RestconfParsingUtils {

    private static final String URI_ENCODING_CHAR_SET = RestconfInternalConstants.URI_ENCODING_CHAR_SET;

    private RestconfParsingUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> urlPathArgsDecode(final Iterable<String> strings) {
        try {
            final List<String> decodedPathArgs = new ArrayList<>();
            for (final String pathArg : strings) {
                final String _decode = URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(_decode);
            }
            return decodedPathArgs;
        } catch (final UnsupportedEncodingException e) {
            final String errMsg = "Invalid URL path '" + strings + "': " + e.getMessage();
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    public static String urlPathArgDecode(final String pathArg) {
        if (pathArg == null) {
            return null;
        }
        try {
            return URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
        }
        catch (final UnsupportedEncodingException e) {
            final String errMsg = "Invalid URL path arg '" + pathArg + "': " + e.getMessage();
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    public static String toModuleName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return null;
        }
        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return null;
        }
        return str.substring(0, idx);
    }

    public static String toNodeName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return str;
        }
        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return str;
        }
        return str.substring(idx + 1);
    }

    public static List<String> omitFirstAndLastEmptyString(final List<String> list) {
        if (list.isEmpty()) {
            return list;
        }
        final String head = list.iterator().next();
        if (head.isEmpty()) {
            list.remove(0);
        }
        if (list.isEmpty()) {
            return list;
        }
        final String last = list.get(list.size() - 1);
        if (last.isEmpty()) {
            list.remove(list.size() - 1);
        }
        return list;
    }

    public static QName toQName(final String name) {
        final String module = RestconfParsingUtils.toModuleName(name);
        final String node = RestconfParsingUtils.toNodeName(name);
        final Module m = ControllerContext.getInstance().findModuleByName(module);
        return m == null ? null : QName.create(m.getQNameModule(), node);
    }
}
