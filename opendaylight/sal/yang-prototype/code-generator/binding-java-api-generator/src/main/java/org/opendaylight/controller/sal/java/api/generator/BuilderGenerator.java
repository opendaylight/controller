package org.opendaylight.controller.sal.java.api.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public final class BuilderGenerator implements CodeGenerator {
    
    private static final String MAIN_TEMPLATE = "main";
    private static final String CLASS_DESCRIPTOR_IN_TEMPLATE = "class";
    public static final String FILE_NAME_SUFFIX = "Builder";

    private ST contentTemplate;

    @Override
    public Writer generate(Type type) throws IOException {
        Writer writer = new StringWriter();
        if (type instanceof GeneratedType && !(type instanceof GeneratedTransferObject)) {
            STGroupFile group = new STGroupFile("BuilderGenerator.stg", '$', '$');
            group.registerRenderer(String.class, new BasicFormatRenderer());
            contentTemplate = group.getInstanceOf(MAIN_TEMPLATE);
            contentTemplate.add(CLASS_DESCRIPTOR_IN_TEMPLATE, new BuilderClassDescriptor((GeneratedType) type));
            writer.write(contentTemplate.render());
        }
        return writer;
    }
    
    public class BasicFormatRenderer implements AttributeRenderer {
        
        private String firstInUpperCase(String string) {
            return  Character.toUpperCase(string.charAt(0)) + string.substring(1);
        }
        
        private String firstInLowerCase(String string) {
            return Character.toLowerCase(string.charAt(0)) + string.substring(1);
        }

        @Override
        public String toString(Object o, String formatString, Locale locale) {
            if (formatString != null) {
                if (formatString.equals("firstToUpper")) {
                    return firstInUpperCase(o.toString());
                } else if (formatString.equals("firstToLower")) {
                    return firstInLowerCase(o.toString());
                } else {
                    throw new IllegalArgumentException("Unsupported format name " + formatString);
                }
            }
            return o.toString();
        }
        
    }
    
}
