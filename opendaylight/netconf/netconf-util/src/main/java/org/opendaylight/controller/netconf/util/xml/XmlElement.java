/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedElementException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class XmlElement {

    private final Element element;
    private static final Logger logger = LoggerFactory.getLogger(XmlElement.class);

    private XmlElement(Element element) {
        this.element = element;
    }

    public static XmlElement fromDomElement(Element e) {
        return new XmlElement(e);
    }

    public static XmlElement fromDomDocument(Document xml) {
        return new XmlElement(xml.getDocumentElement());
    }

    public static XmlElement fromString(String s) throws NetconfDocumentedException {
        try {
            return new XmlElement(XmlUtil.readXmlToElement(s));
        } catch (IOException | SAXException e) {
            throw NetconfDocumentedException.wrap(e);
        }
    }

    public static XmlElement fromDomElementWithExpected(Element element, String expectedName) throws NetconfDocumentedException {
        XmlElement xmlElement = XmlElement.fromDomElement(element);
        xmlElement.checkName(expectedName);
        return xmlElement;
    }

    public static XmlElement fromDomElementWithExpected(Element element, String expectedName, String expectedNamespace) throws NetconfDocumentedException {
        XmlElement xmlElement = XmlElement.fromDomElementWithExpected(element, expectedName);
        xmlElement.checkNamespace(expectedNamespace);
        return xmlElement;
    }

    private static Map<String, String> extractNamespaces(Element typeElement) throws NetconfDocumentedException {
        Map<String, String> namespaces = new HashMap<>();
        NamedNodeMap attributes = typeElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attribKey = attribute.getNodeName();
            if (attribKey.startsWith(XmlUtil.XMLNS_ATTRIBUTE_KEY)) {
                String prefix;
                if (attribKey.equals(XmlUtil.XMLNS_ATTRIBUTE_KEY)) {
                    prefix = "";
                } else {
                    if (!attribKey.startsWith(XmlUtil.XMLNS_ATTRIBUTE_KEY + ":")){
                        throw new NetconfDocumentedException("Attribute doesn't start with :",
                                NetconfDocumentedException.ErrorType.application,
                                NetconfDocumentedException.ErrorTag.invalid_value,
                                NetconfDocumentedException.ErrorSeverity.error);
                    }
                    prefix = attribKey.substring(XmlUtil.XMLNS_ATTRIBUTE_KEY.length() + 1);
                }
                namespaces.put(prefix, attribute.getNodeValue());
            }
        }
        return namespaces;
    }

    public void checkName(String expectedName) throws UnexpectedElementException {
        if (!getName().equals(expectedName)){
            throw new UnexpectedElementException(String.format("Expected %s xml element but was %s", expectedName,
                    getName()),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
    }

    public void checkNamespaceAttribute(String expectedNamespace) throws UnexpectedNamespaceException, MissingNameSpaceException {
        if (!getNamespaceAttribute().equals(expectedNamespace))
        {
            throw new UnexpectedNamespaceException(String.format("Unexpected namespace %s for element %s, should be %s",
                    getNamespaceAttribute(),
                    expectedNamespace),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
    }

    public void checkNamespace(String expectedNamespace) throws UnexpectedNamespaceException, MissingNameSpaceException {
        if (!getNamespace().equals(expectedNamespace))
       {
            throw new UnexpectedNamespaceException(String.format("Unexpected namespace %s for element %s, should be %s",
                    getNamespace(),
                    expectedNamespace),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
    }

    public String getName() {
        if (element.getLocalName()!=null && !element.getLocalName().equals("")){
            return element.getLocalName();
        }
        return element.getTagName();
    }

    public String getAttribute(String attributeName) {
        return element.getAttribute(attributeName);
    }

    public String getAttribute(String attributeName, String namespace) {
        return element.getAttributeNS(namespace, attributeName);
    }

    public NodeList getElementsByTagName(String name) {
        return element.getElementsByTagName(name);
    }

    public void appendChild(Element element) {
        this.element.appendChild(element);
    }

    public Element getDomElement() {
        return element;
    }

    public Map<String, Attr> getAttributes() {

        Map<String, Attr> mappedAttributes = Maps.newHashMap();

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            mappedAttributes.put(attr.getNodeName(), attr);
        }

        return mappedAttributes;
    }

    /**
     * Non recursive
     */
    private List<XmlElement> getChildElementsInternal(ElementFilteringStrategy strat) {
        NodeList childNodes = element.getChildNodes();
        final List<XmlElement> result = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (!(item instanceof Element)) {
                continue;
            }
            if (strat.accept((Element) item)) {
                result.add(new XmlElement((Element) item));
            }
        }

        return result;
    }

    public List<XmlElement> getChildElements() {
        return getChildElementsInternal(new ElementFilteringStrategy() {
            @Override
            public boolean accept(Element e) {
                return true;
            }
        });
    }

    public List<XmlElement> getChildElementsWithinNamespace(final String childName, String namespace) {
        return Lists.newArrayList(Collections2.filter(getChildElementsWithinNamespace(namespace),
                new Predicate<XmlElement>() {
                    @Override
                    public boolean apply(@Nullable XmlElement xmlElement) {
                        return xmlElement.getName().equals(childName);
                    }
                }));
    }

    public List<XmlElement> getChildElementsWithinNamespace(final String namespace) {
        return getChildElementsInternal(new ElementFilteringStrategy() {
            @Override
            public boolean accept(Element e) {
                try {
                    return XmlElement.fromDomElement(e).getNamespace().equals(namespace);
                } catch (MissingNameSpaceException e1) {
                    return false;
                }
            }

        });
    }

    public List<XmlElement> getChildElements(final String tagName) {
        return getChildElementsInternal(new ElementFilteringStrategy() {
            @Override
            public boolean accept(Element e) {
                return e.getTagName().equals(tagName);
            }
        });
    }

    public XmlElement getOnlyChildElement(String childName) throws NetconfDocumentedException {
        List<XmlElement> nameElements = getChildElements(childName);
        if (nameElements.size() != 1){
            throw new NetconfDocumentedException("One element " + childName + " expected in " + toString(),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        return nameElements.get(0);
    }

    public Optional<XmlElement> getOnlyChildElementOptionally(String childName) {
        try {
            return Optional.of(getOnlyChildElement(childName));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public Optional<XmlElement> getOnlyChildElementOptionally(String childName, String namespace) {
        try {
            return Optional.of(getOnlyChildElement(childName, namespace));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public XmlElement getOnlyChildElementWithSameNamespace(String childName) throws  NetconfDocumentedException {
        return getOnlyChildElement(childName, getNamespace());
    }

    public Optional<XmlElement> getOnlyChildElementWithSameNamespaceOptionally(String childName) {
        try {
            return Optional.of(getOnlyChildElement(childName, getNamespace()));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public XmlElement getOnlyChildElementWithSameNamespace() throws NetconfDocumentedException {
        XmlElement childElement = getOnlyChildElement();
        childElement.checkNamespace(getNamespace());
        return childElement;
    }

    public Optional<XmlElement> getOnlyChildElementWithSameNamespaceOptionally() {
        try {
            XmlElement childElement = getOnlyChildElement();
            childElement.checkNamespace(getNamespace());
            return Optional.of(childElement);
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public XmlElement getOnlyChildElement(final String childName, String namespace) throws NetconfDocumentedException {
        List<XmlElement> children = getChildElementsWithinNamespace(namespace);
        children = Lists.newArrayList(Collections2.filter(children, new Predicate<XmlElement>() {
            @Override
            public boolean apply(@Nullable XmlElement xmlElement) {
                return xmlElement.getName().equals(childName);
            }
        }));
        if (children.size() != 1){
            throw new NetconfDocumentedException(String.format("One element %s:%s expected in %s but was %s", namespace,
                    childName, toString(), children.size()),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }

        return children.get(0);
    }

    public XmlElement getOnlyChildElement() throws NetconfDocumentedException {
        List<XmlElement> children = getChildElements();
        if (children.size() != 1){
            throw new NetconfDocumentedException(String.format( "One element expected in %s but was %s", toString(),
                    children.size()),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        return children.get(0);
    }

    public String getTextContent() throws NetconfDocumentedException {
        Node textChild = element.getFirstChild();
        if (null == textChild){
            throw new NetconfDocumentedException(String.format( "Child node expected, got null for " + getName() + " : " + element),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        if (!(textChild instanceof Text)){
            throw new NetconfDocumentedException(String.format(getName() + " should contain text." +
                    Text.class.getName() + " expected, got " + textChild),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        String content = textChild.getTextContent();
        // Trim needed
        return content.trim();
    }

    public String getNamespaceAttribute() throws MissingNameSpaceException {
        String attribute = element.getAttribute(XmlUtil.XMLNS_ATTRIBUTE_KEY);
        if (attribute == null || attribute.equals("")){
            throw new MissingNameSpaceException(String.format("Element %s must specify namespace",
                    toString()),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        return attribute;
    }

    public String getNamespace() throws MissingNameSpaceException {
        String namespaceURI = element.getNamespaceURI();
        if (namespaceURI  == null || namespaceURI.equals("")){
            throw new MissingNameSpaceException(String.format("No namespace defined for %s", this),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        return namespaceURI;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmlElement{");
        sb.append("name='").append(getName()).append('\'');
        if (element.getNamespaceURI() != null) {
            try {
                sb.append(", namespace='").append(getNamespace()).append('\'');
            } catch (MissingNameSpaceException e) {
                logger.trace("Missing namespace for element.");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Search for element's attributes defining namespaces. Look for the one
     * namespace that matches prefix of element's text content. E.g.
     *
     * <pre>
     * &lt;type
     * xmlns:th-java="urn:opendaylight:params:xml:ns:yang:controller:threadpool:impl"&gt;th-java:threadfactory-naming&lt;/type&gt;
     * </pre>
     *
     * returns {"th-java","urn:.."}. If no prefix is matched, then default
     * namespace is returned with empty string as key. If no default namespace
     * is found value will be null.
     */
    public Map.Entry<String/* prefix */, String/* namespace */> findNamespaceOfTextContent() throws NetconfDocumentedException {
        Map<String, String> namespaces = extractNamespaces(element);
        String textContent = getTextContent();
        int indexOfColon = textContent.indexOf(':');
        String prefix;
        if (indexOfColon > -1) {
            prefix = textContent.substring(0, indexOfColon);
        } else {
            prefix = "";
        }
        if (!namespaces.containsKey(prefix)) {
            throw new IllegalArgumentException("Cannot find namespace for " + XmlUtil.toString(element) + ". Prefix from content is "
                    + prefix + ". Found namespaces " + namespaces);
        }
        return Maps.immutableEntry(prefix, namespaces.get(prefix));
    }

    public List<XmlElement> getChildElementsWithSameNamespace(final String childName) throws MissingNameSpaceException {
        List<XmlElement> children = getChildElementsWithinNamespace(getNamespace());
        return Lists.newArrayList(Collections2.filter(children, new Predicate<XmlElement>() {
            @Override
            public boolean apply(@Nullable XmlElement xmlElement) {
                return xmlElement.getName().equals(childName);
            }
        }));
    }

    public void checkUnrecognisedElements(List<XmlElement> recognisedElements,
            XmlElement... additionalRecognisedElements) throws NetconfDocumentedException {
        List<XmlElement> childElements = getChildElements();
        childElements.removeAll(recognisedElements);
        for (XmlElement additionalRecognisedElement : additionalRecognisedElements) {
            childElements.remove(additionalRecognisedElement);
        }
        if (!childElements.isEmpty()){
            throw new NetconfDocumentedException(String.format("Unrecognised elements %s in %s", childElements, this),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
    }

    public void checkUnrecognisedElements(XmlElement... additionalRecognisedElements) throws NetconfDocumentedException {
        checkUnrecognisedElements(Collections.<XmlElement>emptyList(), additionalRecognisedElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        XmlElement that = (XmlElement) o;

        if (!element.isEqualNode(that.element)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    public boolean hasNamespace() {
        try {
            getNamespaceAttribute();
        } catch (MissingNameSpaceException e) {
            try {
                getNamespace();
            } catch (MissingNameSpaceException e1) {
                return false;
            }
            return true;
        }
        return true;
    }

    private interface ElementFilteringStrategy {
        boolean accept(Element e);
    }
}
