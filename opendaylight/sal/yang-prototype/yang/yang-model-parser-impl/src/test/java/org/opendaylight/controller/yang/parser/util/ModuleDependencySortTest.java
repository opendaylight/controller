/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.impl.YangParserListenerImpl;
import org.opendaylight.controller.yang.parser.util.ModuleDependencySort.ModuleNodeImpl;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Edge;

import com.google.common.collect.Sets;

public class ModuleDependencySortTest {

    private ModuleBuilder a = mockModuleBuilder("a", null);
    private ModuleBuilder b = mockModuleBuilder("b", null);
    private ModuleBuilder c = mockModuleBuilder("c", null);
    private ModuleBuilder d = mockModuleBuilder("d", null);

    @Test
    public void testValid() throws Exception {

        mockDependency(a, b);
        mockDependency(b, c);
        mockDependency(b, d);

        ModuleBuilder[] builders = new ModuleBuilder[] { d, b, c, a };

        List<ModuleBuilder> l = ModuleDependencySort.sort(builders);

        assertDependencyGraph(ModuleDependencySort.createModuleGraph(Arrays.asList(builders)));

        @SuppressWarnings("unchecked")
        Matcher<String> cOrD = anyOf(is(c.getName()), is(d.getName()));

        assertThat(l.get(0).getName(), cOrD);
        assertThat(l.get(1).getName(), cOrD);
        assertThat(l.get(2).getName(), is(b.getName()));
        assertThat(l.get(3).getName(), is(a.getName()));
    }

    @Test
    public void testValidModule() throws Exception {

        Date rev = new Date();
        Module a = mockModule("a", rev);
        Module b = mockModule("b", rev);
        Module c = mockModule("c", rev);

        mockDependency(a, b);
        mockDependency(b, c);
        mockDependency(a, c);

        Module[] builders = new Module[] { a, b, c };

        List<Module> l = ModuleDependencySort.sort(builders);

        assertThat(l.get(0).getName(), is(c.getName()));
        assertThat(l.get(1).getName(), is(b.getName()));
        assertThat(l.get(2).getName(), is(a.getName()));
    }

    @Test(expected = YangValidationException.class)
    public void testModuleTwice() throws Exception {
        ModuleBuilder a2 = mockModuleBuilder("a", null);

        ModuleBuilder[] builders = new ModuleBuilder[] { a, a2 };
        try {
            ModuleDependencySort.sort(builders);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(), containsString("Module:a with revision:default declared twice"));
            throw e;
        }
    }

    @Test(expected = YangValidationException.class)
    public void testImportNotExistingModule() throws Exception {
        mockDependency(a, b);

        ModuleBuilder[] builders = new ModuleBuilder[] { a };
        try {
            ModuleDependencySort.sort(builders);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(), containsString("Not existing module imported:b:default by:a:default"));
            throw e;
        }
    }

    @Test
    public void testImportTwice() throws Exception {
        mockDependency(a, b);
        mockDependency(c, b);

        ModuleBuilder[] builders = new ModuleBuilder[] { a, b, c };
        ModuleDependencySort.sort(builders);
    }

    @Test(expected = YangValidationException.class)
    public void testImportTwiceDifferentRevision() throws Exception {
        Date date1 = new Date(463846463486L);
        Date date2 = new Date(364896446683L);
        b = mockModuleBuilder("b", date1);
        ModuleBuilder b2 = mockModuleBuilder("b", date2);

        mockDependency(a, b);
        mockDependency(c, b2);

        ModuleBuilder[] builders = new ModuleBuilder[] { a, c, b, b2 };
        try {
            ModuleDependencySort.sort(builders);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(), containsString("Module:b imported twice with different revisions:"
                    + YangParserListenerImpl.simpleDateFormat.format(date1) + ", "
                    + YangParserListenerImpl.simpleDateFormat.format(date2)));
            throw e;
        }
    }

    @Test
    public void testModuleTwiceWithDifferentRevs() throws Exception {
        ModuleBuilder a2 = mockModuleBuilder("a", new Date());

        ModuleBuilder[] builders = new ModuleBuilder[] { a, a2 };
        ModuleDependencySort.sort(builders);
    }

    @Test(expected = YangValidationException.class)
    public void testModuleTwice2() throws Exception {
        Date rev = new Date();
        ModuleBuilder a2 = mockModuleBuilder("a", rev);
        ModuleBuilder a3 = mockModuleBuilder("a", rev);

        ModuleBuilder[] builders = new ModuleBuilder[] { a, a2, a3 };
        try {
            ModuleDependencySort.sort(builders);
        } catch (YangValidationException e) {
            assertThat(e.getMessage(), containsString("Module:a with revision:"
                    + YangParserListenerImpl.simpleDateFormat.format(rev) + " declared twice"));
            throw e;
        }
    }

    private void assertDependencyGraph(Map<String, Map<Date, ModuleNodeImpl>> moduleGraph) {
        for (Entry<String, Map<Date, ModuleNodeImpl>> node : moduleGraph.entrySet()) {
            String name = node.getKey();

            // Expects only one module revision

            Set<Edge> inEdges = node.getValue().values().iterator().next().getInEdges();
            Set<Edge> outEdges = node.getValue().values().iterator().next().getOutEdges();

            if (name.equals("a")) {
                assertEdgeCount(inEdges, 0, outEdges, 1);
            } else if (name.equals("b")) {
                assertEdgeCount(inEdges, 1, outEdges, 2);
            } else {
                assertEdgeCount(inEdges, 1, outEdges, 0);
            }
        }
    }

    private void assertEdgeCount(Set<Edge> inEdges, int i, Set<Edge> outEdges, int j) {
        assertThat(inEdges.size(), is(i));
        assertThat(outEdges.size(), is(j));
    }

    private void mockDependency(ModuleBuilder a, ModuleBuilder b) {
        ModuleImport imprt = mock(ModuleImport.class);
        doReturn(b.getName()).when(imprt).getModuleName();
        doReturn(b.getRevision()).when(imprt).getRevision();
        a.getModuleImports().add(imprt);
    }

    private void mockDependency(Module a, Module b) {
        ModuleImport imprt = mock(ModuleImport.class);
        doReturn(b.getName()).when(imprt).getModuleName();
        doReturn(b.getRevision()).when(imprt).getRevision();
        a.getImports().add(imprt);
    }

    private ModuleBuilder mockModuleBuilder(String name, Date rev) {
        ModuleBuilder a = mock(ModuleBuilder.class);
        doReturn(name).when(a).getName();
        Set<ModuleImport> set = Sets.newHashSet();
        doReturn(set).when(a).getModuleImports();
        if (rev != null) {
            doReturn(rev).when(a).getRevision();
        }
        return a;
    }

    private Module mockModule(String name, Date rev) {
        Module a = mock(Module.class);
        doReturn(name).when(a).getName();
        Set<ModuleImport> set = Sets.newHashSet();
        doReturn(set).when(a).getImports();
        if (rev != null) {
            doReturn(rev).when(a).getRevision();
        }
        return a;
    }
}
