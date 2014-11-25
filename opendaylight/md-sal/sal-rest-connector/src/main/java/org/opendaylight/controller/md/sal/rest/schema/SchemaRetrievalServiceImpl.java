/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.rest.schema;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class SchemaRetrievalServiceImpl implements SchemaRetrievalService {

    private final ControllerContext salContext;

    private static final Splitter SLASH_SPLITTER = Splitter.on("/");
    private static final Splitter AT_SPLITTER = Splitter.on("@");
    private static final String MOUNT_ARG = ControllerContext.MOUNT;

    public SchemaRetrievalServiceImpl(final ControllerContext controllerContext) {
        salContext = controllerContext;
    }


    @Override
    public SchemaExportContext getSchema(final String mountAndModule) {
        final SchemaContext schemaContext;
        final Iterable<String> pathComponents = SLASH_SPLITTER.split(mountAndModule);
        final Iterator<String> componentIter = pathComponents.iterator();
        if(!Iterables.contains(pathComponents, MOUNT_ARG)) {
            schemaContext = salContext.getGlobalSchema();
        } else {
            final StringBuilder pathBuilder = new StringBuilder();
            while(componentIter.hasNext()) {
                final String current = componentIter.next();
                // It is argument, not last element.
                if(pathBuilder.length() != 0) {
                        pathBuilder.append("/");
                }
                pathBuilder.append(current);
                if(MOUNT_ARG.equals(current)) {
                    // We stop right at mountpoint, last two arguments should
                    // be module name and revision
                    break;
                }
            }
            schemaContext = getMountSchemaContext(pathBuilder.toString());

        }

        checkDocumentedError(componentIter.hasNext(),ErrorType.PROTOCOL,ErrorTag.INVALID_VALUE, "Module name must be supplied.");
        final String moduleName = componentIter.next();
        checkDocumentedError(componentIter.hasNext(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "Revision date must be supplied.");
        final String revisionString = componentIter.next();
        return getExportUsingNameAndRevision(schemaContext, moduleName, revisionString);
    }

    private SchemaExportContext getExportUsingNameAndRevision(final SchemaContext schemaContext, final String moduleName,
            final String revisionStr) {
        try {
            final Date revision = SimpleDateFormatUtil.getRevisionFormat().parse(revisionStr);
            final Module module = schemaContext.findModuleByName(moduleName, revision);
            return new SchemaExportContext(schemaContext, checkNotNullDocumented(module, moduleName));
        } catch (final ParseException e) {
            throw new RestconfDocumentedException("Supplied revision is not in expected date format YYYY-mm-dd", e);
        }
    }

    private SchemaContext getMountSchemaContext(final String identifier) {
        final InstanceIdentifierContext mountContext = salContext.toMountPointIdentifier(identifier);
        return mountContext.getSchemaContext();
    }
    //FIXME: Should be moved to common utility class
    private static void checkDocumentedError(final boolean condition, final ErrorType type, final ErrorTag tag, final String message) {
        if(!condition) {
            throw new RestconfDocumentedException(message, type, tag);
        }
    }

    //FIXME: Should be moved to common utility class
    private static <T> T checkNotNullDocumented(final T value, final String moduleName) {
        if(value == null) {
            throw new RestconfDocumentedException("Module " + moduleName + "was not found.", ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
        }
        return value;
    }





}
