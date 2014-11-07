/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.jaxb;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import java.io.File;
import javax.servlet.FilterConfig;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ParserTest {

    @Test
    public void testParsing() throws Exception {
        File xmlFile = new File(getClass().getResource("/sample-cors-config.xml").getFile());
        assertThat(xmlFile.canRead(), is(true));
        String xmlFileContent = FileUtils.readFileToString(xmlFile);
        Host host = Parser.parse(xmlFileContent, "fileName");
        assertEquals(1, host.getContexts().size());
        // check that MockedFilter has init params merged/replaced
        Optional<Context> context = host.findContext("/restconf");
        assertTrue(context.isPresent());
        assertEquals(1, context.get().getFilters().size());
        MockedFilter filter = (MockedFilter) context.get().getFilters().get(0).getActualFilter();
        FilterConfig filterConfig = filter.getFilterConfig();
        assertEquals("*", filterConfig.getInitParameter("cors.allowed.origins"));
        assertEquals("11", filterConfig.getInitParameter("cors.preflight.maxage"));
    }


    @Test
    public void testParsing_NoFilterDefined() throws Exception {
        File xmlFile = new File(getClass().getResource("/no-filter-defined.xml").getFile());
        assertThat(xmlFile.canRead(), is(true));
        String xmlFileContent = FileUtils.readFileToString(xmlFile);
        try {
            Parser.parse(xmlFileContent, "fileName");
            fail();
        }catch(Exception e){
            assertThat(e.getMessage(), containsString("Cannot find filter for filter-mapping CorsFilter"));
        }
    }

    @Test
    public void testConflictingClass() throws Exception {
        File xmlFile = new File(getClass().getResource("/conflicting-class.xml").getFile());
        assertThat(xmlFile.canRead(), is(true));
        String xmlFileContent = FileUtils.readFileToString(xmlFile);
        try {
            Parser.parse(xmlFileContent, "fileName");
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Error while processing filter CorsFilter of context /restconf"));
            assertThat(e.getCause().getMessage(), containsString("Conflict detected in template/filter filter-class definitions, filter name: CorsFilter"));
        }
    }
}
