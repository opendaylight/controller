/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class DocGenTestHelper {

    private Map<File, Module> modules;
    private ObjectMapper mapper;

    public Map<File, Module> loadModules(String resourceDirectory) throws FileNotFoundException,
            URISyntaxException {

        URI resourceDirUri = getClass().getResource(resourceDirectory).toURI();
        final YangContextParser parser = new YangParserImpl();
        final File testDir = new File(resourceDirUri);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory.toString());
        }
        for (String fileName : fileList) {

            testFiles.add(new File(testDir, fileName));
        }
        return parser.parseYangModelsMapped(testFiles);
    }

    public Map<File, Module> getModules() {
        return modules;
    }

    public void setUp() throws Exception {
        modules = loadModules("/yang");
        mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public SchemaService createMockSchemaService() {
        return createMockSchemaService(null);
    }

    public SchemaService createMockSchemaService(SchemaContext mockContext) {
        if (mockContext == null) {
            mockContext = createMockSchemaContext();
        }

        SchemaService mockSchemaService = mock(SchemaService.class);
        when(mockSchemaService.getGlobalContext()).thenReturn(mockContext);
        return mockSchemaService;
    }

    public SchemaContext createMockSchemaContext() {
        SchemaContext mockContext = mock(SchemaContext.class);
        when(mockContext.getModules()).thenReturn(new HashSet<Module>(modules.values()));

        final ArgumentCaptor<String> moduleCapture = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Date> dateCapture = ArgumentCaptor.forClass(Date.class);
        final ArgumentCaptor<URI> namespaceCapture = ArgumentCaptor.forClass(URI.class);
        when(mockContext.findModuleByName(moduleCapture.capture(), dateCapture.capture())).then(
                new Answer<Module>() {
                    @Override
                    public Module answer(InvocationOnMock invocation) throws Throwable {
                        String module = moduleCapture.getValue();
                        Date date = dateCapture.getValue();
                        for (Module m : modules.values()) {
                            if (m.getName().equals(module) && m.getRevision().equals(date)) {
                                return m;
                            }
                        }
                        return null;
                    }
                });
        when(mockContext.findModuleByNamespaceAndRevision(namespaceCapture.capture(), dateCapture.capture())).then(
                new Answer<Module>() {
                    @Override
                    public Module answer(InvocationOnMock invocation) throws Throwable {
                        URI namespace = namespaceCapture.getValue();
                        Date date = dateCapture.getValue();
                        for (Module m : modules.values()) {
                            if (m.getNamespace().equals(namespace) && m.getRevision().equals(date)) {
                                return m;
                            }
                        }
                        return null;
                    }
                });
        return mockContext;
    }

    public UriInfo createMockUriInfo(String urlPrefix) throws URISyntaxException {
        final URI uri = new URI(urlPrefix);

        UriBuilder mockBuilder = mock(UriBuilder.class);

        final ArgumentCaptor<String> subStringCapture = ArgumentCaptor.forClass(String.class);
        when(mockBuilder.path(subStringCapture.capture())).thenReturn(mockBuilder);
        when(mockBuilder.build()).then(new Answer<URI>() {
            @Override
            public URI answer(InvocationOnMock invocation) throws Throwable {
                return URI.create(uri + "/" + subStringCapture.getValue());
            }
        });

        UriInfo info = mock(UriInfo.class);

        when(info.getRequestUriBuilder()).thenReturn(mockBuilder);
        when(info.getBaseUri()).thenReturn(uri);
        return info;
    }

}
