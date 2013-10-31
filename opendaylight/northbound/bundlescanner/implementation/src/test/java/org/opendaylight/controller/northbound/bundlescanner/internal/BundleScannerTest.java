package org.opendaylight.controller.northbound.bundlescanner.internal;



import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockBundleContext;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BundleScannerTest {

    private static BundleScanner bundleScanner;
    private static List<Bundle> bundles;
    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void init() throws Exception {
        bundles = makeMockBundles();
        bundleScanner = new BundleScanner(bundles.toArray(new Bundle[bundles.size()]));
    }

    @AfterClass
    public static void destroy() throws Exception {
    }

    @Before
    public void setup() {
        System.out.println("==== " + testName.getMethodName());
    }

    @Test
    public void testValidateBundles() {
        assertNotNull(bundleScanner);
        BundleContext context = bundles.get(0).getBundleContext();
        assertNotNull(context.getBundle());
        assertNotNull(context.getBundles());
        assertNotNull(context.getBundles().length >= 4);
    }

    @Test
    public void testBundleEvents() throws Exception {
        MockBundle newBundle = new TestMockBundle("misc", "", "bundle_misc");
        assertTrue(bundleScanner.getAnnotatedClasses(
                newBundle.getBundleContext(), null, null, false).size() == 0);
        BundleEvent event = new BundleEvent(BundleEvent.RESOLVED, newBundle);
        bundleScanner.bundleChanged(event);
        assertTrue(bundleScanner.getAnnotatedClasses(
                newBundle.getBundleContext(), null, null, false).size() == 1);
    }

    @Test
    public void testAnnotatedClassesWithDependencies() throws Exception {
        for (Bundle bundle : bundles) {
            List<Class<?>> classes = bundleScanner.getAnnotatedClasses(
                    bundle.getBundleContext(), null, null, true);
            String name = bundle.getSymbolicName();
            System.out.println("name:" + name + " classes:" + classes.size());
            if ("misc".equals(name)) {
                assertTrue(classes.size() == 1);
            } else if ("base".equals(name)) {
                assertTrue(classes.size() == 5);
            } else if ("sub1".equals(name)) {
                assertTrue(classes.size() == 6);
            } else if ("sub2".equals(name)) {
                assertTrue(classes.size() == 7);
            }
        }
    }

    @Test
    public void testExactFiltering() {
        Bundle bundle = findBundle("sub1");
        String[] annos = { "javax.xml.bind.annotation.XmlTransient" };
        List<Class<?>> classes = bundleScanner.getAnnotatedClasses(
                bundle.getBundleContext(), annos, null, true);
        assertTrue(classes.size() == 1);
    }

    @Test
    public void testNonExactFiltering() {
        Bundle bundle = findBundle("sub1");
        String[] annos = { "javax.xml.bind.annotation.*" };
        List<Class<?>> classes = bundleScanner.getAnnotatedClasses(
                bundle.getBundleContext(), annos, null, true);
        assertTrue(classes.size() == 6);
    }

    @Test
    public void testFilteringUnmatched() {
        Bundle bundle = findBundle("sub1");
        String[] annos = { "non.existent.pkg" };
        List<Class<?>> classes = bundleScanner.getAnnotatedClasses(
                bundle.getBundleContext(), annos, null, true);
        assertTrue(classes.size() == 0);
    }

    @Test
    public void testRegexMerge() {
        Pattern pattern = BundleScanner.mergePatterns(
                new String[] {
                        "javax.xml.bind.annotation.*",
                        "javax.ws.rs.Path"
                    },
                true
            );
        assertTrue(pattern.matcher("Ljavax/xml/bind/annotation/FOO;").find());
        assertFalse(pattern.matcher("Ljavax/servlet/FOO;").find());
    }

    @Test
    public void testExclude() {
        Set<String> excludes = new HashSet<String>();
        excludes.add("bundle_base.Animal");
        Bundle bundle = findBundle("sub1");
        String[] annos = { "javax.xml.bind.annotation.*" };
        List<Class<?>> classes = bundleScanner.getAnnotatedClasses(
                bundle.getBundleContext(), annos, excludes, true);
        assertTrue(classes.size() == 5);
    }

    private static Bundle findBundle(String symName) {
        for (Bundle bundle : bundles) {
            if (bundle.getSymbolicName().equals(symName)) return bundle;
        }
        return null;
    }

    private static List<Bundle> makeMockBundles() throws Exception {
        List<Bundle> result = new ArrayList<Bundle>();
        result.add(new MockBundle());
        result.add(new TestMockBundle("base", "", "bundle_base"));
        result.add(new TestMockBundle("sub1", "bundle_base", "bundle_sub1"));
        result.add(new TestMockBundle("sub2", "bundle_base", "bundle_sub2"));
        return result;
    }

    private static List<URL> findClasses(String pkg) throws URISyntaxException {
        if (pkg == null) return Collections.EMPTY_LIST;
        String npkg = pkg.replaceAll("\\.", "/");
        URL dirUrl = BundleScannerTest.class.getClassLoader().getResource(npkg);
        final List<URL> result = new ArrayList<URL>();
        File dir = new File(dirUrl.toURI());
        dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    try {
                        result.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return false;
            }

        });
        return result;
    }

    public static class TestMockBundle extends MockBundle {
        List<URL> classes;
        public TestMockBundle(String name, String imports, String exports) throws Exception {
            super(name, makeHeaders(name, imports, exports), new MockBundleContext() {
                @Override
                public Bundle[] getBundles() {
                    return bundles.toArray(new Bundle[bundles.size()]);
                }
            });
            MockBundleContext ctx = (MockBundleContext) this.getBundleContext();
            ctx.setBundle(this);
            this.classes = findClasses(exports);
        }

        private static Dictionary<String,String> makeHeaders(
                String name, String imports, String exports)
        {
            Dictionary<String,String> headers = new Hashtable<String,String>();
            headers.put(Constants.IMPORT_PACKAGE, imports);
            headers.put(Constants.EXPORT_PACKAGE, exports);
            headers.put(Constants.BUNDLE_SYMBOLICNAME, name);
            return headers;
        }

        @Override
        public Enumeration findEntries(String path, String filePattern, boolean recurse) {
            return Collections.enumeration(classes);
        }

        @Override
        public long getBundleId() {
            return hashCode();
        }
    }
}
