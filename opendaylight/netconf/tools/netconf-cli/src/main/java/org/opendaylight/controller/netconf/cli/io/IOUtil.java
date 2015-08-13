/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class IOUtil {

    public static final String SKIP = "skip";
    public static final String PROMPT_SUFIX = ">";
    public static final String PATH_SEPARATOR = "/";

    private IOUtil() {
    }

    public static boolean isQName(final String qName) {
        final Matcher matcher = patternNew.matcher(qName);
        return matcher.matches();
    }

    public static Date parseDate(final String revision) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(revision);
        } catch (final ParseException e) {
            throw new IllegalArgumentException("Date not valid", e);
        }
    }

    public static String listType(final SchemaNode schemaNode) {
        if (schemaNode instanceof LeafListSchemaNode) {
            return "Leaf-list";
        } else if (schemaNode instanceof ListSchemaNode) {
            return "List";
        } else if (schemaNode instanceof LeafSchemaNode) {
            return "Leaf";
        }
        // FIXME throw exception on unexpected state, not null/emptyString
        return "";
    }

    public static String qNameToKeyString(final QName qName, final String moduleName) {
        return String.format("%s(%s)", qName.getLocalName(), moduleName);
    }

    // TODO test and check regex + review format of string for QName
    final static Pattern patternNew = Pattern.compile("([^\\)]+)\\(([^\\)]+)\\)");

    public static QName qNameFromKeyString(final String qName, final Map<String, QName> mappedModules)
            throws ReadingException {
        final Matcher matcher = patternNew.matcher(qName);
        if (!matcher.matches()) {
            final String message = String.format("QName in wrong format: %s should be: %s", qName, patternNew);
            throw new ReadingException(message);
        }
        final QName base = mappedModules.get(matcher.group(2));
        if (base == null) {
            final String message = String.format("Module %s cannot be found", matcher.group(2));
            throw new ReadingException(message);
        }
        return QName.create(base, matcher.group(1));
    }

    public static boolean isSkipInput(final String rawValue) {
        return rawValue.equals(SKIP);
    }

}
