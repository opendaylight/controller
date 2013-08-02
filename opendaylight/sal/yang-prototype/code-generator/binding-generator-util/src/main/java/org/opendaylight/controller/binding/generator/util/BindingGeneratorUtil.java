package org.opendaylight.controller.binding.generator.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public final class BindingGeneratorUtil {

    private static final String[] SET_VALUES = new String[] { "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default", "double", "do", "else", "enum", "extends",
            "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while" };

    private BindingGeneratorUtil() {
    }

    public static final Set<String> JAVA_RESERVED_WORDS = new HashSet<String>(Arrays.asList(SET_VALUES));

    public static String validateJavaPackage(final String packageName) {
        if (packageName != null) {
            final String[] packNameParts = packageName.split("\\.");
            if (packNameParts != null) {
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < packNameParts.length; ++i) {
                    final String packNamePart = packNameParts[i];
                    if (Character.isDigit(packNamePart.charAt(0))) {
                        packNameParts[i] = "_" + packNamePart;
                    } else if (JAVA_RESERVED_WORDS.contains(packNamePart)) {
                        packNameParts[i] = "_" + packNamePart;
                    }
                    if (i > 0) {
                        builder.append(".");
                    }
                    builder.append(packNameParts[i]);
                }
                return builder.toString();
            }
        }
        return packageName;
    }

    public static String validateParameterName(final String parameterName) {
        if (parameterName != null) {
            if (JAVA_RESERVED_WORDS.contains(parameterName)) {
                return "_" + parameterName;
            }
        }
        return parameterName;
    }

    public static GeneratedTOBuilder schemaNodeToTransferObjectBuilder(final String packageName,
            final SchemaNode schemaNode, final String transObjectName) {
        if (packageName != null && schemaNode != null && transObjectName != null) {

            final String genTOName = BindingGeneratorUtil.parseToClassName(transObjectName);
            final GeneratedTOBuilder newType = new GeneratedTOBuilderImpl(packageName, genTOName);

            return newType;

        }
        return null;
    }

    public static String moduleNamespaceToPackageName(final Module module) {
        final StringBuilder packageNameBuilder = new StringBuilder();

        final Calendar calendar = Calendar.getInstance();
        if (module.getRevision() == null) {
            throw new IllegalArgumentException("Module " + module.getName() + " does not specify revision date!");
        }
        packageNameBuilder.append("org.opendaylight.yang.gen.v");
        packageNameBuilder.append(module.getYangVersion());
        packageNameBuilder.append(".");

        String namespace = module.getNamespace().toString();
        namespace = namespace.replace("://", ".");
        namespace = namespace.replace("/", ".");
        namespace = namespace.replace(":", ".");
        namespace = namespace.replace("-", ".");
        namespace = namespace.replace("@", ".");
        namespace = namespace.replace("$", ".");
        namespace = namespace.replace("#", ".");
        namespace = namespace.replace("'", ".");
        namespace = namespace.replace("*", ".");
        namespace = namespace.replace("+", ".");
        namespace = namespace.replace(",", ".");
        namespace = namespace.replace(";", ".");
        namespace = namespace.replace("=", ".");

        packageNameBuilder.append(namespace);
        calendar.setTime(module.getRevision());
        packageNameBuilder.append(".rev");
        packageNameBuilder.append(calendar.get(Calendar.YEAR));
        packageNameBuilder.append((calendar.get(Calendar.MONTH) + 1));
        packageNameBuilder.append(calendar.get(Calendar.DAY_OF_MONTH));

        return validateJavaPackage(packageNameBuilder.toString());
    }

    public static String packageNameForGeneratedType(final String basePackageName, final SchemaPath schemaPath) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (schemaPath == null) {
            throw new IllegalArgumentException("Schema Path cannot be NULL!");
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(basePackageName);
        final List<QName> pathToNode = schemaPath.getPath();
        final int traversalSteps = (pathToNode.size() - 1);
        for (int i = 0; i < traversalSteps; ++i) {
            builder.append(".");
            String nodeLocalName = pathToNode.get(i).getLocalName();

            nodeLocalName = nodeLocalName.replace(":", ".");
            nodeLocalName = nodeLocalName.replace("-", ".");
            builder.append(nodeLocalName);
        }
        return validateJavaPackage(builder.toString());
    }

    public static String packageNameForTypeDefinition(final String basePackageName,
            final TypeDefinition<?> typeDefinition) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (typeDefinition == null) {
            throw new IllegalArgumentException("Type Definition reference cannot be NULL!");
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(basePackageName);
        return validateJavaPackage(builder.toString());
    }

    public static String parseToClassName(String token) {
        token = token.replace(".", "");
        String correctStr = parseToCamelCase(token);

        // make first char upper-case
        char first = Character.toUpperCase(correctStr.charAt(0));
        correctStr = first + correctStr.substring(1);
        return correctStr;
    }

    public static String parseToValidParamName(final String token) {
        final String validToken = token.replace(".", "");
        String correctStr = parseToCamelCase(validToken);

        // make first char lower-case
        char first = Character.toLowerCase(correctStr.charAt(0));
        correctStr = first + correctStr.substring(1);
        return validateParameterName(correctStr);
    }

    public static String convertToCapitalLetters(final String token) {
        String convertedStr = token.replace(" ", "_");
        convertedStr = convertedStr.replace(".", "_");
        convertedStr = convertedStr.toUpperCase();
        return convertedStr;
    }

    private static String parseToCamelCase(String token) {
        if (token == null) {
            throw new NullPointerException("Name can not be null");
        }

        String correctStr = token.trim();
        if (correctStr.length() == 0) {
            throw new IllegalArgumentException("Name can not be emty");
        }

        correctStr = replaceWithCamelCase(correctStr, ' ');
        correctStr = replaceWithCamelCase(correctStr, '-');
        correctStr = replaceWithCamelCase(correctStr, '_');
        return correctStr;
    }

    private static String replaceWithCamelCase(String text, char removalChar) {
        StringBuilder sb = new StringBuilder(text);
        String toBeRemoved = String.valueOf(removalChar);

        int toBeRemovedPos = sb.indexOf(toBeRemoved);
        while (toBeRemovedPos != -1) {
            sb.replace(toBeRemovedPos, toBeRemovedPos + 1, "");
            // check if 'toBeRemoved' character is not the only character in
            // 'text'
            if (sb.length() == 0) {
                throw new IllegalArgumentException("Name can not be '" + toBeRemoved + "'");
            }
            String replacement = String.valueOf(sb.charAt(toBeRemovedPos)).toUpperCase();
            sb.setCharAt(toBeRemovedPos, replacement.charAt(0));
            toBeRemovedPos = sb.indexOf(toBeRemoved);
        }
        return sb.toString();
    }
}
