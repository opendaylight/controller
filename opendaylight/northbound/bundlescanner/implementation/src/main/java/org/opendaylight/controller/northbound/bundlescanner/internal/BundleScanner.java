/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.bundlescanner.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.opendaylight.controller.northbound.bundlescanner.IBundleScanService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The custom bundle scanner scans annotations on bundles and is used for
 * constructing JAXBContext instances. It listens for bundle events and updates
 * the metadata in realtime.
 */
/*package*/ class BundleScanner implements SynchronousBundleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleScanner.class);
    private static BundleScanner INSTANCE; // singleton

    private final Pattern annotationPattern;
    private final Map<Long,BundleInfo> bundleAnnotations =
            new HashMap<Long, BundleInfo>();

    public static synchronized BundleScanner getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundleScanner();
        }
        return INSTANCE;
    }

    /*package*/ BundleScanner(Bundle[] bundles) {
        annotationPattern = mergePatterns(IBundleScanService.ANNOTATIONS_TO_SCAN, true);
        init(bundles);
    }

    /*package*/ BundleScanner() {
        this(FrameworkUtil.getBundle(BundleScanner.class).getBundleContext().getBundles());
    }

    public List<Class<?>> getAnnotatedClasses(BundleContext context,
            String[] annotations,
            boolean includeDependentBundleClasses)
    {
        BundleInfo info = bundleAnnotations.get(context.getBundle().getBundleId());
        if (info == null) return Collections.emptyList();
        Pattern pattern = mergePatterns(annotations, false);
        List<Class<?>> result = null;
        if (includeDependentBundleClasses) {
            result = info.getAnnotatedClasses(bundleAnnotations.values(), pattern);
        } else {
            result = info.getAnnotatedClasses(pattern);
        }
        LOGGER.debug("Annotated classes detected: {} matching: {}", result, pattern);
        return result;
    }

    ////////////////////////////////////////////////////////////////
    // SynchronousBundleListener implementation
    ////////////////////////////////////////////////////////////////

    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        long id = bundle.getBundleId();
        switch(event.getType()) {
            case BundleEvent.RESOLVED :
                scan(bundle);
                return;
            case BundleEvent.UNRESOLVED :
            case BundleEvent.UNINSTALLED :
                bundleAnnotations.remove(id);
                return;
        }
    }


    ////////////////////////////////////////////////////////////////
    //  ClassVisitor implementation
    ////////////////////////////////////////////////////////////////

    private static class AnnotationDetector extends ClassVisitor {
        private final Map<String, Set<String>> matchedClasses =
                new HashMap<String, Set<String>>();

        private final Pattern annotationsPattern;
        private Set<String> annotations;
        private String className;
        private boolean accessible;
        private boolean matchedAnnotation;

        public AnnotationDetector(Pattern pattern) {
            super(Opcodes.ASM4);
            this.annotationsPattern = pattern;
        }

        public Map<String, Set<String>> getMatchedClasses() {
            return new HashMap<String, Set<String>>(matchedClasses);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces)
        {
            //LOGGER.debug("Visiting class:" + name);
            className = name;
            accessible = ((access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC);
            matchedAnnotation = false;
            annotations = new HashSet<String>();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            //LOGGER.debug("Visiting annotation:" + desc);
            annotations.add(signature2class(desc));
            if (!matchedAnnotation) {
                matchedAnnotation = (annotationsPattern == null ||
                        annotationsPattern.matcher(desc).find());
            }
            return null;
        }

        @Override
        public void visitEnd() {
            if (matchedAnnotation && accessible) {
                className = path2class(className);
                matchedClasses.put(className, new HashSet<String>(annotations));
            }
        }
    }

    ////////////////////////////////////////////////////////////////
    // Helpers
    ////////////////////////////////////////////////////////////////

    private synchronized void init(Bundle[] bundles) {
        for (Bundle bundle : bundles) {
            int state = bundle.getState();
            if (state == Bundle.RESOLVED ||
                state == Bundle.STARTING ||
                state == Bundle.ACTIVE)
            {
                scan(bundle);
            }
        }
    }

    private static String path2class(String path) {
        return path.replace(".class", "").replaceAll("/", ".");
    }

    private static String class2path(String clz) {
        return clz.replaceAll("\\.", "/");
    }

    @SuppressWarnings("unused")
    private static String class2signature(String clz) {
        return "L" + class2path(clz) + ";";
    }

    private static String signature2class(String sig) {
        if (sig.startsWith("L") && sig.endsWith(";")) {
            sig = sig.substring(1, sig.length()-1);
        }
        return path2class(sig);
    }

   private static List<URL> getBundleClasses(Bundle bundle, String[] pkgs) {
        List<URL> result = new ArrayList<URL>();
        boolean recurse = false;
        if (pkgs == null) {
            recurse = true;
            pkgs = new String[] { "/" } ;
        }
        for (String pkg : pkgs) {
            pkg = class2path(pkg);
            final Enumeration<URL> e = bundle.findEntries(pkg, "*.class", recurse);
            if (e != null) {
                while (e.hasMoreElements()) {
                    URL url = e.nextElement();
                    result.add(url);
                }
            }
        }
        return result;
    }

    private synchronized void scan(Bundle bundle) {
        AnnotationDetector detector = new AnnotationDetector(annotationPattern);
        try {
            for (URL u : getBundleClasses(bundle, null)) {
                InputStream is = u.openStream();
                new ClassReader(is).accept(detector, 0);
                is.close();
            }
        } catch (IOException ioe) {
            LOGGER.error("Error scanning classes in bundle: {}", bundle.getSymbolicName(), ioe);
        }
        Map<String, Set<String>> classes = detector.getMatchedClasses();
        if (classes != null && classes.size() > 0) {
            BundleInfo info = new BundleInfo(bundle, classes);
            bundleAnnotations.put(bundle.getBundleId(),info);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bindings found in bundle: {}[{}] " +
                        "dependencies {} classes {}", bundle.getSymbolicName(),
                        bundle.getBundleId(),
                        info.getDependencies(bundleAnnotations.values()),
                        classes);
            }
        }
        // find bundle dependencies
    }

    public static List<Class<?>> loadClasses(Bundle bundle,
            Collection<String> annotatedClasses)
    {
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (String name : annotatedClasses) {
            try {
                result.add(bundle.loadClass(name));
            } catch (Exception e) {
                LOGGER.error("Unable to load class: {}", name, e);
            }
        }
        return result;
    }

    public static Pattern mergePatterns(String[] patterns, boolean convert2signature) {
        if (patterns == null || patterns.length == 0) {
            return null;
        }
        StringBuilder regex = new StringBuilder();
        for (String c : patterns) {
            if (c.endsWith("*")) {
                c = c.substring(0, c.length() - 1);
            }
            if (regex.length() > 0) regex.append("|");
            regex.append("^");
            if (convert2signature) {
                regex.append("L").append(c.replaceAll("\\.", "/"));
            } else {
                regex.append(c);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Merged regex: [{}]", regex.toString());
        }
        return Pattern.compile(regex.toString());
    }

}
