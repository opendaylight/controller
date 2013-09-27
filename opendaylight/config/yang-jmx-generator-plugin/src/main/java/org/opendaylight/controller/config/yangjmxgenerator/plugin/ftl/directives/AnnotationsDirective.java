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
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation.Parameter;

import com.google.common.collect.Lists;

import freemarker.core.Environment;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add annotations to freemarker template.
 */
public class AnnotationsDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        List<Annotation> annotations = Lists.newArrayList();

        if (object != null) {
            if (object instanceof SimpleSequence)
                annotations = ((SimpleSequence) object).toList();
            else if (object instanceof FtlTemplate) {
                annotations = ((FtlTemplate) object).getAnnotations();
            } else
                throw new IllegalArgumentException(
                        "Object must be a SimpleSequence or instance of "
                                + FtlTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        writeAnnotations(annotations, build, "");

        if (!annotations.isEmpty())
            out.write(build.toString().toCharArray());
    }

    static void writeAnnotations(List<Annotation> annotations,
            StringBuilder build, String linePrefix) {
        for (Annotation annotation : annotations) {
            build.append(linePrefix + "@");
            build.append(annotation.getName());
            if (!annotation.getParams().isEmpty()) {
                build.append("(");
                for (Parameter param : annotation.getParams()) {
                    build.append(param.getKey());
                    build.append(" = ");
                    build.append(fixString(param.getValue()));
                    build.append(", ");
                }
                build.setCharAt(build.length() - 2, ')');
            }
            build.append(System.lineSeparator());
        }
    }

    private static String fixString(String value) {
        // TODO replace with compress single line if possible
        return value.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" +", " ");
    }

}
