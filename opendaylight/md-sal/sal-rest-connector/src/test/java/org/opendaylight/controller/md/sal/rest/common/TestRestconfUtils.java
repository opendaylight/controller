/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 7, 2015
 */
public class TestRestconfUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestRestconfUtils.class);

    private final static YangContextParser parser = new YangParserImpl();

    private TestRestconfUtils () {
        throw new UnsupportedOperationException("Test utility class");
    }

    public static SchemaContext loadSchemaContext(final String yangPath, final SchemaContext schemaContext) {
        try {
            Preconditions.checkArgument(yangPath != null, "Path can not be null.");
            Preconditions.checkArgument(( ! yangPath.isEmpty()), "Path can not be empty.");
            if (schemaContext == null) {
                return loadSchemaContext(yangPath);
            } else {
                return addSchemaContext(yangPath, schemaContext);
            }
        }
        catch (final Exception e) {
            LOG.error("Yang files at path: " + yangPath + " weren't loaded.");
        }
        return schemaContext;
    }

    private static Collection<File> loadFiles(final String resourceDirectory) throws FileNotFoundException {
        final String path = TestRestconfUtils.class.getResource(resourceDirectory).getPath();
        final File testDir = new File(path);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            final String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return testFiles;
    }

    private static SchemaContext loadSchemaContext(final String resourceDirectory) throws IOException {
        final Collection<File> testFiles = loadFiles(resourceDirectory);
        return parser.parseFiles(testFiles);
    }

    private static SchemaContext addSchemaContext(final String resourceDirectory,
            final SchemaContext schemaContext) throws IOException, YangSyntaxErrorException {
        final Collection<File> testFiles = loadFiles(resourceDirectory);
        return parser.parseFiles(testFiles, schemaContext);
    }
}
