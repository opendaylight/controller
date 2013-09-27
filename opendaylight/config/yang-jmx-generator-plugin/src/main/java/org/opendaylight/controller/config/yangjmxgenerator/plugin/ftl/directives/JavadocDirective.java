/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add javadoc to freemarker template as String.
 */
public class JavadocDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        String javadoc = "";

        if (object != null) {
            if (object instanceof SimpleScalar)
                javadoc = ((SimpleScalar) object).getAsString();
            else if (object instanceof FtlTemplate) {
                javadoc = ((FtlTemplate) object).getJavadoc();
            } else
                throw new IllegalArgumentException(
                        "Object must be a String or instance of "
                                + FtlTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        writeJavadoc(build, javadoc, "");
        out.write(build.toString().toCharArray());
    }

    static void writeJavadoc(StringBuilder build, String javadoc,
            String linePrefix) {
        build.append(linePrefix + "/**");
        build.append(System.lineSeparator());
        build.append(linePrefix + "* ");
        build.append(javadoc == null ? "" : javadoc);
        build.append(System.lineSeparator());
        build.append(linePrefix + "*/");
        build.append(System.lineSeparator());
    }

}
