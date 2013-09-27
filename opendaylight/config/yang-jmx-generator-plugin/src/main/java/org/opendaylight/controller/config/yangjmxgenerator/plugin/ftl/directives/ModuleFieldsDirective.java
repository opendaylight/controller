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

import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractModuleTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField;

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
public class ModuleFieldsDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "moduleFields";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        List<ModuleField> fields = Lists.newArrayList();

        if (object != null) {
            if (object instanceof SimpleSequence)
                fields = ((SimpleSequence) object).toList();
            else if (object instanceof AbstractModuleTemplate) {
                fields = ((AbstractModuleTemplate) object).getModuleFields();
            } else
                throw new IllegalArgumentException(
                        "Object must be a SimpleSequence or instance of "
                                + AbstractModuleTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        for (ModuleField field : fields) {
            build.append("    ");
            build.append("protected final "
                    + JmxAttribute.class.getCanonicalName() + " "
                    + field.getName() + "JmxAttribute = new "
                    + JmxAttribute.class.getCanonicalName() + "(\""
                    + field.getAttributeName() + "\");");
            build.append(System.lineSeparator());

            build.append("     private ");
            for (String mod : field.getModifiers()) {
                build.append(mod + " ");
            }
            build.append(field.getType() + " ");
            build.append(field.getName());
            if (field.getNullableDefault() != null)
                build.append(" = " + field.getNullableDefault());
            build.append(";");

            if (field.isDependent()) {
                String comment = field.getDependency().isMandatory() ? "mandatory"
                        : "optional";
                build.append(" // " + comment);
            }
            build.append(System.lineSeparator());

            build.append(System.lineSeparator());

        }

        if (!fields.isEmpty())
            out.write(build.toString().toCharArray());
    }
}
