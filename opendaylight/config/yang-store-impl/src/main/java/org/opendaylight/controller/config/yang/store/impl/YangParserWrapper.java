/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class YangParserWrapper {

    /**
     * throw IllegalStateException if it is unable to parse yang files
     */
    public static SchemaContext parseYangFiles(Collection<? extends InputStream> yangFilesAsInputStreams) {
        YangParserImpl parser = getYangParserInstance();
        Map<InputStream, Module> mappedYangModules = null;
        try {
            mappedYangModules = parseYangFiles(parser, yangFilesAsInputStreams);
        } catch (YangStoreException e) {
            throw new IllegalStateException("Unable to parse yang files", e);
        }
        return getSchemaContextFromModules(parser, mappedYangModules);
    }

    static YangParserImpl getYangParserInstance() {
        return new YangParserImpl();
    }

    static SchemaContext getSchemaContextFromModules(YangModelParser parser, Map<InputStream, Module> allYangModules) {
        return parser.resolveSchemaContext(Sets
                .newHashSet(allYangModules.values()));
    }

    static Map<InputStream, Module> parseYangFiles(YangModelParser parser, Collection<? extends InputStream> allInput) throws YangStoreException {
        List<InputStream> bufferedInputStreams = new ArrayList<>();
        for (InputStream is : allInput) {
            String content;
            try {
                content = IOUtils.toString(is);
            } catch (IOException e) {
                throw new YangStoreException("Can not get yang as String from "
                        + is, e);
            }
            InputStream buf = new ByteArrayInputStream(content.getBytes());
            bufferedInputStreams.add(buf);
        }

        return parser
                .parseYangModelsFromStreamsMapped(bufferedInputStreams);
    }
}
