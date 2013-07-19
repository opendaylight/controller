/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
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

    static List<File> getClassPath(MavenProject project) {
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

    static <T> T checkNotNull(T obj, String paramName) {
        return Preconditions.checkNotNull(obj, "Parameter " + paramName
                + " is null");
    }

    final static class YangsInZipsResult implements Closeable {
        final List<InputStream> yangStreams;
        private final List<Closeable> zipInputStreams;

        private YangsInZipsResult(List<InputStream> yangStreams,
                List<Closeable> zipInputStreams) {
            this.yangStreams = yangStreams;
            this.zipInputStreams = zipInputStreams;
        }

        @Override
        public void close() throws IOException {
            for (InputStream is : yangStreams) {
                is.close();
            }
            for (Closeable is : zipInputStreams) {
                is.close();
            }
        }
    }

    static YangsInZipsResult findYangFilesInDependenciesAsStream(Log log,
            MavenProject project)
            throws MojoFailureException {
        List<InputStream> yangsFromDependencies = new ArrayList<>();
        List<Closeable> zips = new ArrayList<>();
        try {
            List<File> filesOnCp = Util.getClassPath(project);
            log.info(Util.message(
                    "Searching for yang files in following dependencies: %s",
                    YangToSourcesProcessor.LOG_PREFIX, filesOnCp));

            for (File file : filesOnCp) {
                List<String> foundFilesForReporting = new ArrayList<>();
                // is it jar file or directory?
                if (file.isDirectory()) {
                    File yangDir = new File(file,
                            YangToSourcesProcessor.META_INF_YANG_STRING);
                    if (yangDir.exists() && yangDir.isDirectory()) {
                        File[] yangFiles = yangDir
                                .listFiles(new FilenameFilter() {
                                    @Override
                                    public boolean accept(File dir, String name) {
                                        return name.endsWith(".yang")
                                                && new File(dir, name).isFile();
                                    }
                                });
                        for (File yangFile : yangFiles) {
                            yangsFromDependencies.add(new NamedFileInputStream(
                                    yangFile));
                        }
                    }

                } else {
                    ZipFile zip = new ZipFile(file);
                    zips.add(zip);

                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName
                                .startsWith(YangToSourcesProcessor.META_INF_YANG_STRING_JAR)) {
                            if (entry.isDirectory() == false
                                    && entryName.endsWith(".yang")) {
                                foundFilesForReporting.add(entryName);
                                // This will be closed after all strams are
                                // parsed.
                                InputStream entryStream = zip
                                        .getInputStream(entry);
                                yangsFromDependencies.add(entryStream);
                            }
                        }
                    }
                }
                if (foundFilesForReporting.size() > 0) {
                    log.info(Util.message("Found %d yang files in %s: %s",
                            YangToSourcesProcessor.LOG_PREFIX,
                            foundFilesForReporting.size(), file,
                            foundFilesForReporting));
                }

            }
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return new YangsInZipsResult(yangsFromDependencies, zips);
    }

    final static class ContextHolder {
        private final SchemaContext context;
        private final Set<Module> yangModules;

        ContextHolder(SchemaContext context, Set<Module> yangModules) {
            this.context = context;
            this.yangModules = yangModules;
        }

        SchemaContext getContext() {
            return context;
        }

        Set<Module> getYangModules() {
            return yangModules;
        }
    }

}
