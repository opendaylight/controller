/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * logback config utils
 */
public final class LogbackConfigUtil {

    /** logback config file pattern (*.xml) */
    protected static final String LOGBACK_CONFIG_FILE_REGEX_SEED = ".+\\.xml";
    private static final Logger LOG = LoggerFactory
            .getLogger(LogbackConfigUtil.class);

    /**
     *  forbidden ctor
     */
    private LogbackConfigUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param logConfigRoot folder containing configuration files
     * @return sorted list of found files
     */
    public static List<File> harvestSortedConfigFiles(File logConfigRoot) {
        final Pattern xmlFilePattern = Pattern.compile(LOGBACK_CONFIG_FILE_REGEX_SEED);
        File[] configs = logConfigRoot.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && xmlFilePattern.matcher(pathname.getName()).find();
            }
        });

        List<File> sortedConfigFiles = new ArrayList<File>(configs.length);
        for (File cfgItem : configs) {
            LOG.trace("config: {}", cfgItem.toURI());
            sortedConfigFiles.add(cfgItem);
        }
        Collections.sort(sortedConfigFiles);

        return sortedConfigFiles;
    }

}
