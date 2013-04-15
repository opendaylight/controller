/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.spi;

import java.io.File;
import java.util.Collection;

/**
 * Classes implementing this interface can be submitted to maven-yang-plugin's
 * generate-resources goal.
 */
public interface ResourceGenerator {

    /**
     * Generate resources (e.g. copy files into resources folder) from provided
     * list of yang files
     * 
     * @param resources
     *            list of parsed yang files
     * @param outputBaseDir
     *            expected output directory for resources configured by user
     */
    void generateResourceFiles(Collection<File> resources, File outputBaseDir);
}
