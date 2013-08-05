package org.opendaylight.controller.sal.binding.generator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

public class SupportTestUtil {

    public static void containsSignatures(final GeneratedType genType,
            final MethodSignaturePattern... searchedSignsWhat) {
        final List<MethodSignature> searchedSignsIn = genType.getMethodDefinitions();
        containsSignatures(searchedSignsIn, searchedSignsWhat);
    }

    public static void containsSignatures(final List<MethodSignature> searchedSignsIn,
            final MethodSignaturePattern... searchedSignsWhat) {
        if (searchedSignsIn == null) {
            throw new IllegalArgumentException("List of method signatures in which should be searched can't be null");
        }
        if (searchedSignsWhat == null) {
            throw new IllegalArgumentException("Array of method signatures which should be searched can't be null");
        }

        for (MethodSignaturePattern searchedSignWhat : searchedSignsWhat) {
            boolean nameMatchFound = false;
            String typeNameFound = "";
            for (MethodSignature searchedSignIn : searchedSignsIn) {
                if (searchedSignWhat.getName().equals(searchedSignIn.getName())) {
                    nameMatchFound = true;
                    typeNameFound = resolveFullNameOfReturnType(searchedSignIn.getReturnType());
                    if (searchedSignWhat.getType().equals(typeNameFound)) {
                        break;
                    }
                }
            }
            assertTrue("Method " + searchedSignWhat.getName() + " wasn't found.", nameMatchFound);
            assertEquals("Return type in method " + searchedSignWhat.getName() + " doesn't match expected type ",
                    searchedSignWhat.getType(), typeNameFound);

        }
    }

    public static String resolveFullNameOfReturnType(final Type type) {
        final StringBuilder nameBuilder = new StringBuilder();
        if (type instanceof ParameterizedType) {
            nameBuilder.append(type.getName() + "<");
            ParameterizedType parametrizedTypes = (ParameterizedType) type;
            for (Type parametrizedType : parametrizedTypes.getActualTypeArguments()) {
                nameBuilder.append(parametrizedType.getName() + ",");
            }
            if (nameBuilder.charAt(nameBuilder.length() - 1) == ',') {
                nameBuilder.deleteCharAt(nameBuilder.length() - 1);
            }
            nameBuilder.append(">");
        } else {
            nameBuilder.append(type.getName());
        }
        return nameBuilder.toString();
    }

    public static void containsInterface(String interfaceNameSearched, GeneratedType genType) {
        List<Type> caseCImplements = genType.getImplements();
        boolean interfaceFound = false;
        for (Type caseCImplement : caseCImplements) {
            String interfaceName = resolveFullNameOfReturnType(caseCImplement);
            if (interfaceName.equals(interfaceNameSearched)) {
                interfaceFound = true;
                break;
            }
        }
        assertTrue("Generated type " + genType.getName() + " doesn't implement inrerface " + interfaceNameSearched,
                interfaceFound);
    }

}
