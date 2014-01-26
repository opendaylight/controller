/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.events.StartElement;

import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public final class RestUtil {
    
    public static final String SQUOTE = "'";
    public static final String DQUOTE = "\"";
    private static final Pattern PREDICATE_PATTERN = Pattern.compile("\\[(.*?)\\]");

    public final static TypeDefinition<?> resolveBaseTypeFrom(TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

    public static IdentityValuesDTO asInstanceIdentifier(String value, PrefixesMaping prefixMap) {
        String valueTrimmed = value.trim();
        if (!valueTrimmed.startsWith("/")) {
            return null;
        }
        String[] xPathParts = valueTrimmed.split("/");
        if (xPathParts.length < 2) { // must be at least "/pr:node"
            return null;
        }
        IdentityValuesDTO identityValuesDTO = new IdentityValuesDTO();
        for (int i = 1; i < xPathParts.length; i++) {
            String xPathPartTrimmed = xPathParts[i].trim();
            
            String xPathPartStr = getIdAndPrefixAsStr(xPathPartTrimmed);
            IdentityValue identityValue = toIdentity(xPathPartStr, prefixMap);
            if (identityValue == null) {
                return null;
            }
            
            List<Predicate> predicates = toPredicates(xPathPartTrimmed, prefixMap);
            if (predicates == null) {
                return null;
            }
            identityValue.setPredicates(predicates);
            
            identityValuesDTO.add(identityValue);
        }
        return identityValuesDTO.getValuesWithNamespaces().isEmpty() ? null : identityValuesDTO;
    }
    
    private static String getIdAndPrefixAsStr(String pathPart) {
        int predicateStartIndex = pathPart.indexOf("[");
        return predicateStartIndex == -1 ? pathPart : pathPart.substring(0, predicateStartIndex);
    }
    
    private static IdentityValue toIdentity(String xPathPart, PrefixesMaping prefixMap) {
        String xPathPartTrimmed = xPathPart.trim();
        if (xPathPartTrimmed.isEmpty()) {
            return null;
        }
        String[] prefixAndIdentifier = xPathPartTrimmed.split(":");
        // it is not "prefix:value"
        if (prefixAndIdentifier.length != 2) {
            return null;
        }
        String prefix = prefixAndIdentifier[0].trim();
        String identifier = prefixAndIdentifier[1].trim();
        if (prefix.isEmpty() || identifier.isEmpty()) {
            return null;
        }
        String namespace = prefixMap.getNamespace(prefix);
        return new IdentityValue(namespace, identifier, namespace.equals(prefix) ? null : prefix);
    }

    private static List<Predicate> toPredicates(String predicatesStr, PrefixesMaping prefixMap) {
        List<Predicate> result = new ArrayList<>();
        List<String> predicates = new ArrayList<>();
        Matcher matcher = PREDICATE_PATTERN.matcher(predicatesStr);
        while (matcher.find()) {
            predicates.add(matcher.group(1).trim());
        }
        for (String predicate : predicates) {
            int indexOfEqualityMark = predicate.indexOf("=");
            if (indexOfEqualityMark != -1) {
                String predicateValue = toPredicateValue(predicate.substring(indexOfEqualityMark + 1));
                if (predicate.startsWith(".")) { // it is leaf-list
                    if (predicateValue == null) {
                        return null;
                    }
                    result.add(new Predicate(null, predicateValue));
                } else {
                    IdentityValue identityValue = toIdentity(predicate.substring(0, indexOfEqualityMark),
                            prefixMap);
                    if (identityValue == null || predicateValue == null) {
                        return null;
                    }
                    result.add(new Predicate(identityValue, predicateValue));
                }
            }
        }
        return result;
    }

    private static String toPredicateValue(String predicatedValue) {
        String predicatedValueTrimmed = predicatedValue.trim();
        if ((predicatedValueTrimmed.startsWith(DQUOTE) || predicatedValueTrimmed.startsWith(SQUOTE))
                && (predicatedValueTrimmed.endsWith(DQUOTE) || predicatedValueTrimmed.endsWith(SQUOTE))) {
            return predicatedValueTrimmed.substring(1, predicatedValueTrimmed.length() - 1);
        }
        return null;
    }

    public interface PrefixesMaping {
        public String getNamespace(String prefix);
    }

    public static class PrefixMapingFromXml implements PrefixesMaping {
        StartElement startElement = null;

        public PrefixMapingFromXml(StartElement startElement) {
            this.startElement = startElement;
        }

        @Override
        public String getNamespace(String prefix) {
            return startElement.getNamespaceContext().getNamespaceURI(prefix);
        }
    }

    public static class PrefixMapingFromJson implements PrefixesMaping {

        @Override
        public String getNamespace(String prefix) {
            return prefix;
        }
    }

}
