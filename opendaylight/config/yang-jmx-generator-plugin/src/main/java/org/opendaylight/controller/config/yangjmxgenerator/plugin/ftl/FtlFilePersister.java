/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.AnnotationsDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.ConstructorsDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.FieldsDirectiveProg;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.HeaderDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.JavadocDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.MethodsDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.ModuleFieldsDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.TypeDeclarationDirective;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives.UnimplementedExceptionDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FtlFilePersister {
    private static final Logger logger = LoggerFactory
            .getLogger(FtlFilePersister.class);

    private static final Pattern TRAILING_WHITESPACES = Pattern.compile(" +$", Pattern.MULTILINE);

    @VisibleForTesting
    public Map<FtlTemplate, String> serializeFtls(
            Collection<? extends FtlTemplate> ftlFiles) {
        Map<FtlTemplate, String> result = new HashMap<>();
        for (FtlTemplate ftlFile : ftlFiles) {

            try (Writer writer = new StringWriter()) {
                Template template = getCfg().getTemplate(
                        ftlFile.getFtlTempleteLocation());
                try {
                    template.process(ftlFile, writer);
                } catch (TemplateException e) {
                    throw new IllegalStateException(
                            "Template error while generating " + ftlFile, e);
                }
                String fileContent = writer.toString();
                // remove trailing spaces
                fileContent = TRAILING_WHITESPACES.matcher(fileContent).replaceAll("");
                result.put(ftlFile, fileContent);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Exception while processing template", e);
            }
        }

        return result;
    }

    public List<File> persist(Collection<? extends FtlTemplate> ftlFiles,
            File dstFolder, boolean overwrite) throws IOException {
        Map<FtlTemplate, String> ftlFileStringMap = serializeFtls(ftlFiles);
        List<File> result = new ArrayList<>();
        for (Entry<FtlTemplate, String> entry : ftlFileStringMap.entrySet()) {
            FtlTemplate ftlFile = entry.getKey();
            File targetFile = new File(dstFolder, ftlFile.getRelativeFile()
                    .getPath());
            File pathToFile = targetFile.getParentFile();
            if (pathToFile.exists() == false) {
                pathToFile.mkdirs();
            }
            if (targetFile.exists() && overwrite == false) {
                logger.info("Skipping {} since it already exists", targetFile);
            } else {
                try (Writer fileWriter = new FileWriter(targetFile)) {
                    fileWriter.write(entry.getValue());
                }
                logger.info("{}: File {} generated successfully",
                        JMXGenerator.class.getCanonicalName(), targetFile);
                result.add(targetFile);
            }
        }
        return result;
    }

    private Configuration getCfg() {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(getClass(), "/freeMarker/");
        cfg.setSharedVariable("javadocD", new JavadocDirective());
        cfg.setSharedVariable("annotationsD", new AnnotationsDirective());
        cfg.setSharedVariable("typeDeclarationD",
                new TypeDeclarationDirective());
        cfg.setSharedVariable("constructorsD", new ConstructorsDirective());
        cfg.setSharedVariable("fieldsD", new FieldsDirectiveProg());
        cfg.setSharedVariable("moduleFieldsD", new ModuleFieldsDirective());
        cfg.setSharedVariable("methodsD", new MethodsDirective());
        cfg.setSharedVariable("headerD", new HeaderDirective());
        cfg.setSharedVariable("unimplementedExceptionD",
                new UnimplementedExceptionDirective());
        return cfg;
    }

}
