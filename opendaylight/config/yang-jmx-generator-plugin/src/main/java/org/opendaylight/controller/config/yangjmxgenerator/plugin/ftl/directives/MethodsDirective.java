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
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDeclaration;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;

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
public class MethodsDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        List<? extends Method> methods = Lists.newArrayList();

        if (object != null) {
            if (object instanceof SimpleSequence)
                methods = ((SimpleSequence) object).toList();
            else if (object instanceof FtlTemplate) {
                methods = ((FtlTemplate) object).getMethods();
            } else
                throw new IllegalArgumentException(
                        "Object must be a SimpleSequence or instance of "
                                + FtlTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        for (Method method : methods) {
            if (method.getJavadoc() != null)
                JavadocDirective.writeJavadoc(build, method.getJavadoc(), "    ");

            if (!method.getAnnotations().isEmpty()) {
                AnnotationsDirective.writeAnnotations(method.getAnnotations(),
                        build, "    ");
            }

            build.append("    " + "public ");
            for (String mod : method.getModifiers()) {
                build.append(mod + " ");
            }
            build.append(method.getReturnType() + " ");

            build.append(method.getName() + "(");
            for (Field param : method.getParameters()) {
                for (String mod : param.getModifiers()) {
                    build.append(mod + " ");
                }
                build.append(param.getType() + " ");
                build.append(param.getName() + ", ");
            }
            if (method.getParameters().isEmpty())
                build.append(")");
            else {
                build.deleteCharAt(build.length() - 1);
                build.deleteCharAt(build.length() - 1);
                build.append(')');
            }

            if (method instanceof MethodDeclaration) {
                build.append(";");
                build.append(System.lineSeparator());
            } else if (method instanceof MethodDefinition) {
                if (!((MethodDefinition) method).getThrowsExceptions()
                        .isEmpty())
                    build.append(" throws ");
                for (String ex : ((MethodDefinition) method)
                        .getThrowsExceptions()) {
                    build.append(ex + " ");
                }
                build.append(" {");
                build.append(System.lineSeparator());
                build.append("        ");
                build.append(((MethodDefinition) method).getBody());
                build.append(System.lineSeparator());
                build.append("    ");
                build.append("}");
                build.append(System.lineSeparator());
            }
            build.append(System.lineSeparator());

        }

        if (!methods.isEmpty())
            out.write(build.toString().toCharArray());
    }
}
