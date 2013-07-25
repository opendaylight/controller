package org.opendaylight.controller.sal.java.api.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

public final class BuilderGenerator implements CodeGenerator {

    public static final String FILE_NAME_SUFFIX = "Builder";

    @Override
    public Writer generate(Type type) throws IOException {
        Writer writer = new StringWriter();
        if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
            BuilderTemplate builerGeneratorXtend = new BuilderTemplate();
            writer.write(builerGeneratorXtend.generate(new BuilderClassDescriptor((GeneratedType) type)).toString());
        }
        return writer;
    }

}
