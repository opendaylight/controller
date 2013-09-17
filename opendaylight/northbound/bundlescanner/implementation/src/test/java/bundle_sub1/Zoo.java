package bundle_sub1;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import bundle_base.Animal;
import bundle_base.Mammal;


@XmlRootElement
public class Zoo {
    private Animal creature;

    @XmlElementRef
    public Animal getCreature() {
        return creature;
    }

    public void setCreature(Animal creature) {
        this.creature = creature;
    }

    public Zoo() {
        creature = new Mammal();
    }
}
