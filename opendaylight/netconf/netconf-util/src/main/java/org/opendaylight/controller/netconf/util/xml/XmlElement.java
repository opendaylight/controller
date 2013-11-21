/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

public class XmlElement {

    public final Element element;

    private XmlElement(Element element) {
        this.element = element;
    }

    public static XmlElement fromDomElement(Element e) {
        return new XmlElement(e);
    }

    public static XmlElement fromDomDocument(Document xml) {
        return new XmlElement(xml.getDocumentElement());
    }

    public static XmlElement fromString(String s) {
        try {
            return new XmlElement(XmlUtil.readXmlToElement(s));
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException("Unable to create from " + s, e);
        }
    }

    public static XmlElement fromDomElementWithExpected(Element element, String expectedName) {
        XmlElement xmlElement = XmlElement.fromDomElement(element);
        xmlElement.checkName(expectedName);
        return xmlElement;
    }

    public static XmlElement fromDomElementWithExpected(Element element, String expectedName, String expectedNamespace) {
        XmlElement xmlElement = XmlElement.fromDomElementWithExpected(element, expectedName);
        xmlElement.checkNamespace(expectedNamespace);
        return xmlElement;
    }

    private static Map<String, String> extractNamespaces(Element typeElement) {
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
                    Preconditions.checkState(attribKey.startsWith(XmlUtil.XMLNS_ATTRIBUTE_KEY + ":"));
                    prefix = attribKey.substring(XmlUtil.XMLNS_ATTRIBUTE_KEY.length() + 1);
                }
                namespaces.put(prefix, attribute.getNodeValue());
            }
        }
        return namespaces;
    }

    public void checkName(String expectedName) {
        Preconditions.checkArgument(getName().equals(expectedName), "Expected %s xml element but was %s", expectedName,
                getName());
    }

    public void checkNamespaceAttribute(String expectedNamespace) {
        Preconditions.checkArgument(getNamespaceAttribute().equals(expectedNamespace),
                "Unexpected namespace %s for element %s, should be %s", getNamespaceAttribute(), expectedNamespace);
    }

    public void checkNamespace(String expectedNamespace) {
        Preconditions.checkArgument(getNamespace().equals(expectedNamespace),
                "Unexpected namespace %s for element %s, should be %s", getNamespace(), expectedNamespace);
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
        // Element newElement = (Element) element.cloneNode(true);
        // newElement.appendChild(configElement);
        // return XmlElement.fromDomElement(newElement);
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
            if (item instanceof Element == false)
                continue;
            if (strat.accept((Element) item))
                result.add(new XmlElement((Element) item));
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
                return XmlElement.fromDomElement(e).getNamespace().equals(namespace);
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

    public XmlElement getOnlyChildElement(String childName) {
        List<XmlElement> nameElements = getChildElements(childName);
        Preconditions.checkState(nameElements.size() == 1, "One element " + childName + " expected in " + toString());
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

    public XmlElement getOnlyChildElementWithSameNamespace(String childName) {
        return getOnlyChildElement(childName, getNamespace());
    }

    public Optional<XmlElement> getOnlyChildElementWithSameNamespaceOptionally(String childName) {
        try {
            return Optional.of(getOnlyChildElement(childName, getNamespace()));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public XmlElement getOnlyChildElementWithSameNamespace() {
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

    public XmlElement getOnlyChildElement(final String childName, String namespace) {
        List<XmlElement> children = getChildElementsWithinNamespace(namespace);
        children = Lists.newArrayList(Collections2.filter(children, new Predicate<XmlElement>() {
            @Override
            public boolean apply(@Nullable XmlElement xmlElement) {
                return xmlElement.getName().equals(childName);
            }
        }));
        Preconditions.checkState(children.size() == 1, "One element %s:%s expected in %s but was %s", namespace,
                childName, toString(), children.size());
        return children.get(0);
    }

    public XmlElement getOnlyChildElement() {
        List<XmlElement> children = getChildElements();
        Preconditions.checkState(children.size() == 1, "One element expected in %s but was %s", toString(),
                children.size());
        return children.get(0);
    }

    public String getTextContent() {
        Node textChild = element.getFirstChild();
        Preconditions.checkState(textChild instanceof Text, getName() + " should contain text");
        String content = textChild.getTextContent();
        // Trim needed
        return content.trim();
    }

    public String getNamespaceAttribute() {
        String attribute = element.getAttribute(XmlUtil.XMLNS_ATTRIBUTE_KEY);
        Preconditions.checkState(attribute != null && !attribute.equals(""), "Element %s must specify namespace",
                toString());
        return attribute;
    }

    public String getNamespace() {
        String namespaceURI = element.getNamespaceURI();
        Preconditions.checkState(namespaceURI != null, "No namespace defined for %s", this);
        return namespaceURI.toString();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("XmlElement{");
        sb.append("name='").append(getName()).append('\'');
        if (element.getNamespaceURI() != null) {
            sb.append(", namespace='").append(getNamespace()).append('\'');
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
    public Map.Entry<String/* prefix */, String/* namespace */> findNamespaceOfTextContent() {
        Map<String, String> namespaces = extractNamespaces(element);
        String textContent = getTextContent();
        int indexOfColon = textContent.indexOf(":");
        String prefix;
        if (indexOfColon > -1) {
            prefix = textContent.substring(0, indexOfColon);
        } else {
            prefix = "";
        }
        if (namespaces.containsKey(prefix) == false) {
            throw new IllegalArgumentException("Cannot find namespace for " + XmlUtil.toString(element) + ". Prefix from content is "
                    + prefix + ". Found namespaces " + namespaces);
        }
        return Maps.immutableEntry(prefix, namespaces.get(prefix));
    }

    public List<XmlElement> getChildElementsWithSameNamespace(final String childName) {
        List<XmlElement> children = getChildElementsWithinNamespace(getNamespace());
        return Lists.newArrayList(Collections2.filter(children, new Predicate<XmlElement>() {
            @Override
            public boolean apply(@Nullable XmlElement xmlElement) {
                return xmlElement.getName().equals(childName);
            }
        }));
    }

    public void checkUnrecognisedElements(List<XmlElement> recognisedElements,
            XmlElement... additionalRecognisedElements) {
        List<XmlElement> childElements = getChildElements();
        childElements.removeAll(recognisedElements);
        for (XmlElement additionalRecognisedElement : additionalRecognisedElements) {
            childElements.remove(additionalRecognisedElement);
        }
        Preconditions.checkState(childElements.isEmpty(), "Unrecognised elements %s in %s", childElements, this);
    }

    public void checkUnrecognisedElements(XmlElement... additionalRecognisedElements) {
        checkUnrecognisedElements(Collections.<XmlElement> emptyList(), additionalRecognisedElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        XmlElement that = (XmlElement) o;

        if (!element.isEqualNode(that.element))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    public boolean hasNamespace() {
        try {
            getNamespaceAttribute();
        } catch (IllegalStateException e) {
            try {
                getNamespace();
            } catch (IllegalStateException e1) {
                return false;
            }
            return true;
        }
        return true;
    }

    private static interface ElementFilteringStrategy {
        boolean accept(Element e);
    }
}
