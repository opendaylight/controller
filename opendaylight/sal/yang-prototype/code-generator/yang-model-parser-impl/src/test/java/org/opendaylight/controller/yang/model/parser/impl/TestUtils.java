/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;

final class TestUtils {

    private TestUtils() {
    }

    public static Set<Module> loadModules(String resourceDirectory) {
        YangModelParser parser = new YangModelParserImpl();
        File testDir = new File(resourceDirectory);
        String[] fileList = testDir.list();
        String[] testFiles = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            File file = new File(testDir, fileName);
            testFiles[i] = file.getAbsolutePath();
        }
        return parser.parseYangModels(testFiles);
    }

    public static Module findModule(Set<Module> modules, String moduleName) {
        Module result = null;
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                result = module;
                break;
            }
        }
        return result;
    }

    public static ModuleImport findImport(Set<ModuleImport> imports,
            String prefix) {
        ModuleImport result = null;
        for (ModuleImport moduleImport : imports) {
            if (moduleImport.getPrefix().equals(prefix)) {
                result = moduleImport;
                break;
            }
        }
        return result;
    }

    public static TypeDefinition<?> findTypedef(
            Set<TypeDefinition<?>> typedefs, String name) {
        TypeDefinition<?> result = null;
        for (TypeDefinition<?> td : typedefs) {
            if (td.getQName().getLocalName().equals(name)) {
                result = td;
                break;
            }
        }
        return result;
    }

    public static SchemaPath createPath(boolean absolute, URI namespace,
            Date revision, String prefix, String... names) {
        List<QName> path = new ArrayList<QName>();
        for (String name : names) {
            path.add(new QName(namespace, revision, prefix, name));
        }
        return new SchemaPath(path, absolute);
    }

    public static Date createDate(String date) {
        Date result;
        final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            result = simpleDateFormat.parse(date);
        } catch (ParseException e) {
            result = null;
        }
        return result;
    }

}
