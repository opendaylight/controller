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

import com.google.common.collect.Lists;

import freemarker.core.Environment;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add fields to freemarker template.
 */
public class FieldsDirectiveProg implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        List<Field> fields = Lists.newArrayList();

        if (object != null) {
            if (object instanceof SimpleSequence)
                fields = ((SimpleSequence) object).toList();
            else if (object instanceof FtlTemplate) {
                fields = ((FtlTemplate) object).getFields();
            } else
                throw new IllegalArgumentException(
                        "Object must be a SimpleSequence or instance of "
                                + FtlTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        for (Field field : fields) {
            build.append("     private ");
            for (String mod : field.getModifiers()) {
                build.append(mod + " ");
            }
            build.append(field.getType() + " ");
            build.append(field.getName());
            if (field.getDefinition() != null)
                build.append(" = " + field.getDefinition());
            build.append(";");
            build.append(System.lineSeparator());
        }

        if (!fields.isEmpty())
            out.write(build.toString().toCharArray());
    }

    // String templateStr = "Hello ${user}";
    // Template t = new Template("name", new StringReader(templateStr), new
    // Configuration());
}
