/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * logback config utils
 */
public abstract class LogbackConfigUtil {

    private static Logger LOG = LoggerFactory
            .getLogger(LogbackConfigUtil.class);

    /**
     * @param logConfigRoot folder containing configuration files
     * @return sorted list of found files
     */
    public static List<File> harvestSortedConfigFiles(File logConfigRoot) {
        File[] configs = logConfigRoot.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && pathname.getName().matches(".+\\.xml");
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
