/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller;

import java.io.File;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.parser.impl.YangModelParserImpl;

public class Demo {

    public static void main(String[] args) throws Exception {

        String yangFilesDir;
        if (args.length > 0) {
            yangFilesDir = args[0];
        } else {
            yangFilesDir = "src/main/resources/demo";
        }

        File resourceDir = new File(yangFilesDir);
        if (!resourceDir.exists()) {
            throw new IllegalArgumentException(
                    "Specified resource directory does not exists: "
                            + resourceDir.getAbsolutePath());
        }

        String[] dirList = resourceDir.list();
        String[] absFiles = new String[dirList.length];

        int i = 0;
        for (String fileName : dirList) {
            File abs = new File(resourceDir, fileName);
            absFiles[i] = abs.getAbsolutePath();
            i++;
        }

        YangModelParserImpl parser = new YangModelParserImpl();
        Set<Module> builtModules = parser.parseYangModels(absFiles);

        System.out.println("Modules built: " + builtModules.size());
    }

}
