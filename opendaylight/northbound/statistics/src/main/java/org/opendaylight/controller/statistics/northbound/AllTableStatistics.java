/**
 * 
 */
package org.opendaylight.controller.statistics.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author adityavaja
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AllTableStatistics {
    @XmlElement
    List<TableStatistics> tableStatistics;
    //To satisfy JAXB
    private AllTableStatistics() {
    }
    
    public AllTableStatistics(List<TableStatistics> tableStatistics) {
            this.tableStatistics = tableStatistics;
    }

    public List<TableStatistics> getTableStatistics() {
            return tableStatistics;
    }

    public void setTableStatistics(List<TableStatistics> tableStatistics) {
            this.tableStatistics = tableStatistics;
    }

}
