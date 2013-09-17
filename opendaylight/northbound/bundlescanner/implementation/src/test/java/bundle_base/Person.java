package bundle_base;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
@Deprecated
public class Person extends BasePerson {

    @XmlElement
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlRootElement
    public static class Info { }

    @XmlRootElement
    private static class PrivateInfo { }
}
