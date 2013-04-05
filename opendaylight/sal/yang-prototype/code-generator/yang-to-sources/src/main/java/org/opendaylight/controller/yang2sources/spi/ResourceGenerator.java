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

public interface ResourceGenerator {

    void generateResourceFiles(Collection<File> resources, File outputBaseDir);
}
