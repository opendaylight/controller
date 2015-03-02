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

    // FIXME: BUG-1275: this is code duplicates data.impl.codec

    public static final String SQUOTE = "'";
    public static final String DQUOTE = "\"";
    private static final Pattern PREDICATE_PATTERN = Pattern.compile("\\[(.*?)\\]");

    public final static TypeDefinition<?> resolveBaseTypeFrom(final TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

    public static IdentityValuesDTO asInstanceIdentifier(final String value, final PrefixesMaping prefixMap) {
        final String valueTrimmed = value.trim();
        if (!valueTrimmed.startsWith("/")) {
            return null;
        }
        final String[] xPathParts = valueTrimmed.split("/");
        if (xPathParts.length < 2) { // must be at least "/pr:node"
            return null;
        }
        final IdentityValuesDTO identityValuesDTO = new IdentityValuesDTO(value);
        for (int i = 1; i < xPathParts.length; i++) {
            String xPathPartTrimmed = xPathParts[i].trim();
            /*
             * If we have an odd number of quotes then we have an open string literal
             * and we should ignore the "/" character inside the string literal by grafting
             * the next one on after it
             */
            while(xPathPartTrimmed.split("'").length%2 != 1 && i+1 < xPathParts.length) {
                xPathPartTrimmed = xPathPartTrimmed.concat("/" + xPathParts[++i].trim());
            }

            final String xPathPartStr = getIdAndPrefixAsStr(xPathPartTrimmed);
            final IdentityValue identityValue = toIdentity(xPathPartStr, prefixMap);
            if (identityValue == null) {
                return null;
            }

            final List<Predicate> predicates = toPredicates(xPathPartTrimmed, prefixMap);
            if (predicates == null) {
                return null;
            }
            identityValue.setPredicates(predicates);

            identityValuesDTO.add(identityValue);
        }
        return identityValuesDTO.getValuesWithNamespaces().isEmpty() ? null : identityValuesDTO;
    }

    private static String getIdAndPrefixAsStr(final String pathPart) {
        final int predicateStartIndex = pathPart.indexOf("[");
        return predicateStartIndex == -1 ? pathPart : pathPart.substring(0, predicateStartIndex);
    }

    private static IdentityValue toIdentity(final String xPathPart, final PrefixesMaping prefixMap) {
        final String xPathPartTrimmed = xPathPart.trim();
        if (xPathPartTrimmed.isEmpty()) {
            return null;
        }
        final String[] prefixAndIdentifier = xPathPartTrimmed.split(":");
        // it is not "prefix:value"
        if (prefixAndIdentifier.length != 2) {
            return null;
        }
        final String prefix = prefixAndIdentifier[0].trim();
        final String identifier = prefixAndIdentifier[1].trim();
        if (prefix.isEmpty() || identifier.isEmpty()) {
            return null;
        }
        final String namespace = prefixMap.getNamespace(prefix);
        return new IdentityValue(namespace, identifier);
    }

    private static List<Predicate> toPredicates(final String predicatesStr, final PrefixesMaping prefixMap) {
        final List<Predicate> result = new ArrayList<>();
        final List<String> predicates = new ArrayList<>();
        final Matcher matcher = PREDICATE_PATTERN.matcher(predicatesStr);
        while (matcher.find()) {
            predicates.add(matcher.group(1).trim());
        }
        for (final String predicate : predicates) {
            final int indexOfEqualityMark = predicate.indexOf("=");
            if (indexOfEqualityMark != -1) {
                final String predicateValue = toPredicateValue(predicate.substring(indexOfEqualityMark + 1));
                if (predicate.startsWith(".")) { // it is leaf-list
                    if (predicateValue == null) {
                        return null;
                    }
                    result.add(new Predicate(null, predicateValue));
                } else {
                    final IdentityValue identityValue = toIdentity(predicate.substring(0, indexOfEqualityMark), prefixMap);
                    if (identityValue == null || predicateValue == null) {
                        return null;
                    }
                    result.add(new Predicate(identityValue, predicateValue));
                }
            }
        }
        return result;
    }

    private static String toPredicateValue(final String predicatedValue) {
        final String predicatedValueTrimmed = predicatedValue.trim();
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

        public PrefixMapingFromXml(final StartElement startElement) {
            this.startElement = startElement;
        }

        @Override
        public String getNamespace(final String prefix) {
            return startElement.getNamespaceContext().getNamespaceURI(prefix);
        }
    }

    public static class PrefixMapingFromJson implements PrefixesMaping {

        @Override
        public String getNamespace(final String prefix) {
            return prefix;
        }
    }

}
