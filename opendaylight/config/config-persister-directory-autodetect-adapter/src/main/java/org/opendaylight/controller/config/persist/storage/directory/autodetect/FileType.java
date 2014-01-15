/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.autodetect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.config.persist.storage.directory.DirectoryPersister;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

enum FileType {

    plaintext, xml;

    public static final String XML_STORAGE_FIRST_LINE = "<" + ConfigSnapshot.SNAPSHOT_ROOT_ELEMENT_NAME + ">";
    private static final String XML_FILE_DEFINITION_LINE = "<?xml";

    static FileType getFileType(File file) {
        String firstLine = readFirstLine(file);
        if(isPlaintextStorage(firstLine)) {
            return plaintext;
        } else if(isXmlStorage(firstLine))
            return xml;

        throw new IllegalArgumentException("File " + file + " is not of permitted storage type: " + Arrays.toString(FileType.values()));
    }

    private static boolean isXmlStorage(String firstLine) {
        boolean isXml = false;
        isXml |= firstLine.startsWith(XML_STORAGE_FIRST_LINE);
        isXml |= firstLine.startsWith(XML_FILE_DEFINITION_LINE);
        return isXml;
    }

    private static boolean isPlaintextStorage(String firstLine) {
        return firstLine.startsWith(DirectoryPersister.MODULES_START);

    }

    @VisibleForTesting
    static String readFirstLine(File file) {
        FirstLineReadingProcessor callback = new FirstLineReadingProcessor();
        try {
            return Files.readLines(file, Charsets.UTF_8, callback);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to detect file type of file " + file, e);
        }
    }


    private static class FirstLineReadingProcessor implements com.google.common.io.LineProcessor<String> {
        private String firstNonBlankLine;

        @Override
        public boolean processLine(String line) throws IOException {
            if(isEmptyLine(line)) {
                return true;
            } else {
                firstNonBlankLine = line.trim();
                return false;
            }
        }

        private boolean isEmptyLine(String line) {
            return StringUtils.isBlank(line);
        }

        @Override
        public String getResult() {
            return firstNonBlankLine;
        }
    }
}
