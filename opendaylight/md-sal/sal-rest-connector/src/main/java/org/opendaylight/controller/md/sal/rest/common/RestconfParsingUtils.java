/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
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
 * Utility class for parsing Strings inputs to specific path arguments.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 8, 2015
 */
public class RestconfParsingUtils {

    private RestconfParsingUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> urlPathArgsDecode(final Iterable<String> strings) {
        try {
            final List<String> decodedPathArgs = new ArrayList<>();
            for (final String pathArg : strings) {
                final String _decode = URLDecoder.decode(pathArg, RestconfInternalConstants.URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(_decode);
            }
            return omitFirstAndLastEmptyString(decodedPathArgs);
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
            return URLDecoder.decode(pathArg, RestconfInternalConstants.URI_ENCODING_CHAR_SET);
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

    public static QName getModuleNameAndRevision(final String identifier) {
        final int mountIndex = identifier.indexOf(RestconfInternalConstants.MOUNT);
        String moduleNameAndRevision = "";
        if (mountIndex >= 0) {
            moduleNameAndRevision = identifier.substring(mountIndex + RestconfInternalConstants.MOUNT.length());
        } else {
            moduleNameAndRevision = identifier;
        }

        final Splitter splitter = Splitter.on("/").omitEmptyStrings();
        final Iterable<String> split = splitter.split(moduleNameAndRevision);
        final List<String> pathArgs = Lists.<String> newArrayList(split);
        RestconfValidationUtils.checkDocumentedError((pathArgs.size() >= 2), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'");

        try {
            final String moduleName = pathArgs.get(0);
            final String revision = pathArgs.get(1);
            final Date moduleRevision = RestconfInternalConstants.REVISION_FORMAT.parse(revision);
            return QName.create(null, moduleRevision, moduleName);
        } catch (final ParseException e) {
            throw new RestconfDocumentedException("URI has bad format. It should be \'moduleName/yyyy-MM-dd\'",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }
}
