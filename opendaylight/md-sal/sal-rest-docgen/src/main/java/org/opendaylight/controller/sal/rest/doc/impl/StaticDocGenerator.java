/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang2sources.spi.CodeGenerator;

import javax.ws.rs.core.UriInfo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class gathers all yang defined {@link Module}s and generates Swagger compliant documentation.
 */
public class StaticDocGenerator extends ApiDocGenerator implements CodeGenerator {

    private static final String DEFAULT_OUTPUT_BASE_DIR_PATH = "target" + File.separator + "generated-resources"
        + File.separator + "swagger-api-documentation";

    private static Logger _logger = LoggerFactory.getLogger(ApiDocGenerator.class);

    private MavenProject mavenProject;
    private File projectBaseDir;
    private Map<String, String> additionalConfig;
    private File resourceBaseDir;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     *
     * @param context
     * @param outputDir
     * @param yangModules
     * @return
     * @throws IOException
     */
    @Override
    public Collection<File> generateSources(SchemaContext context, File outputDir, Set<Module> yangModules) throws IOException {
        List<File> result = new ArrayList<>();

        // Create Base Directory
        final File outputBaseDir;
        if (outputDir == null) {
            outputBaseDir = new File(DEFAULT_OUTPUT_BASE_DIR_PATH);
        }
        else outputBaseDir = outputDir;
        outputBaseDir.mkdirs();

        // Create Resources directory
        File resourcesDir = new File(outputBaseDir, "resources");
        resourcesDir.mkdirs();

        // Create JS file
        File resourcesJsFile = new File(outputBaseDir, "resources.js");
        resourcesJsFile.createNewFile();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(resourcesJsFile));
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Write resource listing to JS file
        ResourceList resourceList = super.getResourceListing(null, context, "");
        String resourceListJson = mapper.writeValueAsString(resourceList);
        resourceListJson = resourceListJson.replace("\'", "\\\'").replace("\\n", "\\\\n");
        bufferedWriter.write("function getSpec() {\n\treturn \'" + resourceListJson + "\';\n}\n\n");

        // Write resources/APIs to JS file and to disk
        bufferedWriter.write("function jsonFor(resource) {\n\tswitch(resource) {\n");
        for (Resource resource : resourceList.getApis()) {
            int revisionIndex = resource.getPath().indexOf('(');
            String name = resource.getPath().substring(0, revisionIndex);
            String revision = resource.getPath().substring(revisionIndex + 1, resource.getPath().length() - 1);
            ApiDeclaration apiDeclaration = super.getApiDeclaration(name, revision, null, context, "");
            String json = mapper.writeValueAsString(apiDeclaration);
            // Manually insert models because org.json.JSONObject cannot be serialized by ObjectMapper
            json = json.replace("\"models\":{}", "\"models\":" + apiDeclaration.getModels().toString().replace("\\\"", "\""));
            // Escape single quotes and new lines
            json = json.replace("\'", "\\\'").replace("\\n", "\\\\n");
            bufferedWriter.write("\t\tcase \"" + name + "(" + revision + ")\": return \'" + json + "\';\n");

            File resourceFile = new File(resourcesDir, name + "(" + revision + ").json");
            BufferedWriter resourceFileWriter = new BufferedWriter(new FileWriter(resourceFile));
            resourceFileWriter.write(json);
            resourceFileWriter.close();
            result.add(resourceFile);
        }
        bufferedWriter.write("\t}\n\treturn \"\";\n}");
        bufferedWriter.close();

        result.add(resourcesJsFile);
        return result;
    }

    @Override
    protected String generatePath(UriInfo uriInfo, String name, String revision) {
        if (uriInfo == null) {
            return name + "(" + revision + ")";
        }
        return super.generatePath(uriInfo, name, revision);
    }

    @Override
    protected String createBasePathFromUriInfo(UriInfo uriInfo) {
        if (uriInfo == null) {
            return RESTCONF_CONTEXT_ROOT;
        }
        return super.createBasePathFromUriInfo(uriInfo);
    }

    @Override
    public void setLog(Log log) {
    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalConfig) {
        this.additionalConfig = additionalConfig;
    }

    @Override
    public void setResourceBaseDir(File resourceBaseDir) {
        this.resourceBaseDir = resourceBaseDir;
    }

    @Override
    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.projectBaseDir = mavenProject.getBasedir();
    }
}
