/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.bundlescanner.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundleInfo holds information related to the bundle obtained during the
 * bundle scan process.
 */
/*package*/ class BundleInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleInfo.class);

    private final Bundle bundle;
    private final Map<String, Set<String>> annotatedClasses;
    private final Set<String> exportPkgs;
    private final Set<String> importPkgs;

    public BundleInfo(Bundle bundle, Map<String, Set<String>> classes) {
        this.bundle = bundle;
        this.annotatedClasses = classes;
        Dictionary<String, String> dict = bundle.getHeaders();
        this.importPkgs = parsePackages(dict.get(Constants.IMPORT_PACKAGE));
        this.exportPkgs = parsePackages(dict.get(Constants.EXPORT_PACKAGE));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("{name:").append(bundle.getSymbolicName())
          .append(" id:").append(getId())
          .append(" annotated-classes:").append(annotatedClasses)
          .append(" imports:").append(importPkgs)
          .append(" exports:").append(exportPkgs).append("}");
        return sb.toString();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public long getId() {
        return bundle.getBundleId();
    }

    public List<Class<?>> getAnnotatedClasses(Pattern pattern, Set<String> excludes) {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, Set<String>> entry : annotatedClasses.entrySet()) {
            if (matches(pattern, entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return BundleScanner.loadClasses(result, bundle, excludes);
    }

    private boolean matches(Pattern pattern, Set<String> values) {
        if (pattern == null) return true;
        //LOGGER.debug("Matching: {} {}", pattern.toString(), values);
        for (String s : values) {
            if (pattern.matcher(s).find()) return true;
        }
        return false;
    }

    /**
     * Get classes with annotations matching a pattern
     *
     * @param allbundles - all bundles
     * @param pattern - annotation pattern to match
     * @param initBundle - the bundle which initiated this call
     * @param excludes - set of class names to be excluded
     *
     * @return list of annotated classes matching the pattern
     */
    public List<Class<?>> getAnnotatedClasses(
            Collection<BundleInfo> allbundles,
            Pattern pattern, Bundle initBundle,
            Set<String> excludes)
    {
        List<Class<?>> classes = getAnnotatedClasses(pattern, excludes);
        processAnnotatedClassesInternal(this, allbundles, pattern,
                new HashSet<BundleInfo>(), classes, initBundle, excludes);
        return classes;
    }

    private List<String> getExportedAnnotatedClasses(Pattern pattern) {
        List<String> classes = new ArrayList<String>();
        for (Map.Entry<String, Set<String>> entry : annotatedClasses.entrySet()) {
            String cls = entry.getKey();
            int idx = cls.lastIndexOf(".");
            String pkg = (idx == -1 ? "" : cls.substring(0, idx));
            // for a class to match, the package has to be exported and
            // annotations should match the given pattern
            if (exportPkgs.contains(pkg) && matches(pattern, entry.getValue())) {
                classes.add(cls);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found in bundle:{} exported classes:[{}]",
                    getBundle().getSymbolicName(), classes);
        }
        return classes;
    }

    private static void processAnnotatedClassesInternal(
            BundleInfo target,
            Collection<BundleInfo> bundlesToScan,
            Pattern pattern,
            Collection<BundleInfo> visited,
            List<Class<?>> classes,
            Bundle initBundle, Set<String> excludes)
    {
        for (BundleInfo other : bundlesToScan) {
            if (other.getId() == target.getId()) continue;
            if (target.isDependantOn(other)) {
                if (!visited.contains(other)) {
                    classes.addAll(BundleScanner.loadClasses(
                            other.getExportedAnnotatedClasses(pattern),
                            initBundle, excludes));
                    visited.add(other);
                    processAnnotatedClassesInternal(other, bundlesToScan,
                            pattern, visited, classes, initBundle, excludes);
                }
            }
        }
    }

    private boolean isDependantOn(BundleInfo other) {
        for (String pkg : importPkgs) {
            if (other.exportPkgs.contains(pkg)) return true;
        }
        return false;
    }

    public List<BundleInfo> getDependencies(Collection<BundleInfo> bundles) {
        List<BundleInfo> result = new ArrayList<BundleInfo>();
        for(BundleInfo bundle : bundles) {
            if (isDependantOn(bundle)) result.add(bundle);
        }
        return result;
    }


    private static Set<String> parsePackages(String packageString) {
        if (packageString == null) return Collections.emptySet();
        String[] packages = packageString.split(",");
        Set<String> result = new HashSet<String>();
        for (int i=0; i<packages.length; i++) {
            String[] nameAndAttrs = packages[i].split(";");
            String packageName = nameAndAttrs[0].trim();
            result.add(packageName);
        }
        return result;
    }

}
