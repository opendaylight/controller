/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FullyQualifiedName {

    private final String packageName;
    private final String typeName;

    public FullyQualifiedName(String packageName, String typeName) {
        this.packageName = checkNotNull(packageName);
        this.typeName = checkNotNull(typeName);
    }

    public FullyQualifiedName(Class<?> clazz) {
        this(clazz.getPackage().getName(), clazz.getSimpleName());
    }

    public static FullyQualifiedName fromString(String fqn) {
        Matcher m = Pattern.compile("(.*)\\.([^\\.]+)$").matcher(fqn);
        if (m.matches()) {
            return new FullyQualifiedName(m.group(1), m.group(2));
        } else {
            return new FullyQualifiedName("", fqn);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public File toFile(File srcDirectory) {
        String directory = packageName.replace(".", File.separator);
        return new File(srcDirectory, directory + File.separator + typeName + ".java");
    }


    @Override
    public String toString() {
        if (packageName.isEmpty()){
            return typeName;
        }
        return packageName + "." + typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FullyQualifiedName that = (FullyQualifiedName) o;

        if (!packageName.equals(that.packageName)) {
            return false;
        }
        if (!typeName.equals(that.typeName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = packageName.hashCode();
        result = 31 * result + typeName.hashCode();
        return result;
    }
}
