/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

final class Util {
    static final String YANG_SUFFIX = "yang";

    // Cache for listed directories and found yang files. Typically yang files
    // are utilized twice. First: code is generated during generate-sources
    // phase Second: yang files are copied as resources during
    // generate-resources phase. This cache ensures that yang files are listed
    // only once.
    private static Map<File, Collection<File>> cache = Maps
            .newHashMapWithExpectedSize(10);

    /**
     * List files recursively and return as array of String paths. Use cache of
     * size 1.
     */
    static Collection<File> listFiles(File root) throws FileNotFoundException {
        if (cache.get(root) != null)
            return cache.get(root);

        if (!root.exists()) {
            throw new FileNotFoundException(root.toString());
        }

        Collection<File> yangFiles = FileUtils.listFiles(root,
                new String[] { YANG_SUFFIX }, true);

        toCache(root, yangFiles);
        return yangFiles;
    }

    static List<InputStream> listFilesAsStream(File rootDir)
            throws FileNotFoundException {
        List<InputStream> is = new ArrayList<InputStream>();

        Collection<File> files = listFiles(rootDir);
        for (File f : files) {
            is.add(new NamedFileInputStream(f));
        }

        return is;
    }

    static class NamedFileInputStream extends FileInputStream {
        private final File file;

        NamedFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + file + "}";
        }
    }

    static String[] listFilesAsArrayOfPaths(File rootDir)
            throws FileNotFoundException {
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

    private static void toCache(final File rootDir,
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

    public static List<File> getClassPath(MavenProject project) {
        List<File> dependencies = Lists.newArrayList();
        for (Artifact element : project.getArtifacts()) {
            File asFile = element.getFile();
            if (isJar(asFile) || asFile.isDirectory()) {
                dependencies.add(asFile);
            }
        }
        return dependencies;
    }

    private static final String JAR_SUFFIX = ".jar";

    private static boolean isJar(File element) {
        return (element.isFile() && element.getName().endsWith(JAR_SUFFIX)) ? true
                : false;
    }

    public static Collection<File> getFilesFromClasspath(
            List<File> jarsOnClasspath, List<String> classPathFilter)
            throws ZipException, IOException {
        List<File> yangs = Lists.newArrayList();

        for (File file : jarsOnClasspath) {
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(YANG_SUFFIX)) {
                    InputStream stream = zip.getInputStream(entry);
                }
            }
        }

        return yangs;
    }

    public static boolean acceptedFilter(String name, List<String> filter) {
        for (String f : filter) {
            if (name.endsWith(f)) {
                return true;
            }
        }
        return false;
    }

}
