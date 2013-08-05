/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.Type;

public final class ClassGenerator implements CodeGenerator {

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();
        if (type instanceof GeneratedTransferObject) {
            final GeneratedTransferObject genTO = (GeneratedTransferObject) type;
            final ClassTemplate template = new ClassTemplate(genTO);
            writer.write(template.generate().toString());
        }
        return writer;
    }

}
