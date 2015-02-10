/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.file.xml.model;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.config.persist.storage.file.xml.DataEncrypter;

@XmlRootElement(name = "persisted-snapshots")
public final class Config {

    private List<ConfigSnapshot> snapshots;

    Config(List<ConfigSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public Config() {
        this.snapshots = Lists.newArrayList();
    }

    @XmlElement(name = "snapshot")
    @XmlElementWrapper(name = "snapshots")
    public List<ConfigSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<ConfigSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public void toXml(File to) {
        try {

            // TODO Moxy has to be used instead of default jaxb impl due to a bug
            // default implementation has a bug that prevents from serializing xml in a string
            JAXBContext jaxbContext = org.eclipse.persistence.jaxb.JAXBContextFactory.createContext(new Class[]{Config.class}, null);

            Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshaller.marshal(this, to);
        } catch (JAXBException e) {
            throw new PersistException("Unable to persist configuration", e);
        }
    }

    public static Config fromXml(File from) {
        if(isEmpty(from)) {
            return new Config();
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
            Unmarshaller um = jaxbContext.createUnmarshaller();
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            //XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(from));
            XMLStreamReader xsr = xif.createXMLStreamReader(DataEncrypter.decryptCredentialAttributes(from.getPath()));
            Config config = ((Config) um.unmarshal(xsr));
            DataEncrypter.encryptCredentialAttributes(from.getPath());
            return config;
        } catch (JAXBException | XMLStreamException e) {
            throw new PersistException("Unable to restore configuration", e);
        }
    }

    private static boolean isEmpty(File from) {
        return from.length() == 0 || isBlank(from);
    }

    private static boolean isBlank(File from) {
        try {
            return StringUtils.isBlank(Files.toString(from, Charsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error reading file" + from, e);
        }
    }

    public Optional<ConfigSnapshot> getLastSnapshot() {
        ConfigSnapshot last = Iterables.getLast(snapshots, null);
        return last == null ? Optional.<ConfigSnapshot>absent() : Optional.of(last);
    }

    public void addConfigSnapshot(ConfigSnapshot snap, int numberOfStoredBackups) {
        if(shouldReplaceLast(numberOfStoredBackups) && snapshots.isEmpty() == false) {
            snapshots.remove(0);
        }
        snapshots.add(snap);
    }

    private boolean shouldReplaceLast(int numberOfStoredBackups) {
        return numberOfStoredBackups == snapshots.size();
    }
}
