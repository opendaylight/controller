/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package bundle_sub2;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import bundle_base.BasePerson;
import bundle_base.Person;


@XmlRootElement
public class Customer extends Person {

    private String password;
    private List<String> phoneNumbers;
    @XmlElementRef
    @XmlElementWrapper
    private final List<BasePerson> agents = new ArrayList<BasePerson>();

    @XmlTransient
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @XmlElement(name = "phone-number")
    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public void addAgent(Person mgr) {
        this.agents.add(mgr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" password:").append(password);
        sb.append(" phoneNumbers:").append(phoneNumbers);
        sb.append(" agents:").append(agents);
        return sb.toString();
    }
}

