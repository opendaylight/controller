/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

final class Util {

    static final String YANG_SUFFIX = "yang";

    // Cache for listed directories and found yang files. Typically yang files
    // are utilized twice. First: code is generated during generate-sources
    // phase Second: yang files are copied as resources during
    // generate-resources phase. This cache ensures that yang files are listed
    // only once.
    private static Map<String, Collection<File>> cache = Maps
            .newHashMapWithExpectedSize(10);

    /**
     * List files recursively and return as array of String paths. Use cache of
     * size 1.
     */
    static Collection<File> listFiles(String rootDir) {

        if (cache.get(rootDir) != null)
            return cache.get(rootDir);

        Collection<File> yangFiles = FileUtils.listFiles(new File(rootDir),
                new String[] { YANG_SUFFIX }, true);

        toCache(rootDir, yangFiles);
        return yangFiles;
    }

    static String[] listFilesAsArrayOfPaths(String rootDir) {
        String[] filesArray = new String[] {};
        Collection<File> yangFiles = listFiles(rootDir);

        // If collection is empty, return empty array [] rather then [null]
        // array, that is created by default
        return yangFiles.isEmpty() ? filesArray : Collections2.transform(
                yangFiles, new Function<File, String>() {

                    @Override
                    public String apply(File input) {
                        return input.getPath();
                    }
                }).toArray(filesArray);
    }

    private static void toCache(final String rootDir,
            final Collection<File> yangFiles) {
        cache.put(rootDir, yangFiles);
    }

    /**
     * Instantiate object from fully qualified class name
     */
    static <T> T getInstance(String codeGeneratorClass, Class<T> baseType)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        return baseType.cast(resolveClass(codeGeneratorClass, baseType)
                .newInstance());
    }

    private static Class<?> resolveClass(String codeGeneratorClass,
            Class<?> baseType) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(codeGeneratorClass);

        if (!isImplemented(baseType, clazz))
            throw new IllegalArgumentException("Code generator " + clazz
                    + " has to implement " + baseType);
        return clazz;
    }

    private static boolean isImplemented(Class<?> expectedIface,
            Class<?> byClazz) {
        for (Class<?> iface : byClazz.getInterfaces()) {
            if (iface.equals(expectedIface))
                return true;
        }
        return false;
    }

    static String message(String message, String logPrefix, Object... args) {
        String innerMessage = String.format(message, args);
        return String.format("%s %s", logPrefix, innerMessage);
    }
}
