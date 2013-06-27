//import groovy.xml.StreamingMarkupBuilder
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

class Counter {
    def counter = 0
    def get() {
        return get(true)
    }
    def get(isInc) {
        if (isInc) {
            counter++
        }
        return String.format('%02d', counter)
    }
}


cnt = new Counter()
def writer = new StringWriter()
xmlDoc = new MarkupBuilder(writer)
xmlDoc.setDoubleQuotes(true)
xmlDoc.getMkp().xmlDeclaration(version:'1.0', encoding: 'UTF-8')

//def data = {
//  mkp.xmlDeclaration()
//  network(xmlns: 'urn:opendaylight:controller:network') {
dataFile = new File(args[0])
evaluate(dataFile)
// xmlDoc.network(xmlns: 'urn:opendaylight:controller:network') {
    // topologies {
      // topology {
        // 'topology-id'('topId_'+cnt.get())
        // types()
        // nodes {
          // node {
            // 'node-id'('nodeId_'+cnt.get())
            // 'supporting-ne'('networkId_'+cnt.get())
            // 'termination-points' {
              // 'termination-point' {
                // 'tp-id'('tpId_'+cnt.get())
              // }
            // }
          // }
          // node {
            // 'node-id'('nodeId_'+cnt.get())
            // 'supporting-ne'('networkId_'+cnt.get())
            // 'termination-points' {
              // 'termination-point' {
                // 'tp-id'('tpId_'+cnt.get())
              // }
            // }
          // }
          // node {
            // 'node-id'('nodeId_'+cnt.get())
            // 'supporting-ne'('networkId_'+cnt.get())
            // 'termination-points' {
              // 'termination-point' {
                // 'tp-id'('tpId_'+cnt.get())
              // }
              // 'termination-point' {
                // 'tp-id'('tpId_'+cnt.get())
              // }
            // }
          // }
        // }
        // links {
          // link {
            // 'link-id'('linkId_'+cnt.get())
            // source {
              // 'source-node'('nodeId_'+cnt.get())
              // 'source-tp'('tpId_'+cnt.get(false))
            // }
            // destination {
              // 'dest-node'('nodeId_'+cnt.get())
              // 'dest-tp'('tpId_'+cnt.get(false))
            // }
          // }
          // link {
            // 'link-id'('linkId_'+cnt.get())
            // source {
              // 'source-node'('nodeId_'+cnt.get())
              // 'source-tp'('tpId_'+cnt.get(false))
            // }
            // destination {
              // 'dest-node'('nodeId_'+cnt.get())
              // 'dest-tp'('tpId_'+cnt.get(false))
            // }
          // }
        // }
      // }
    // }
    // 'network-elements' {
      // 'network-element' {
        // 'element-id'('ntElementId_'+cnt.get())
      // }
      // 'network-element' {
        // 'element-id'('ntElementId_'+cnt.get())
      // }
    // }
  // }

//}


// def xmlDoc = new StreamingMarkupBuilder()
// xmlDoc.encoding = 'UTF'
//println XmlUtil.serialize(xmlDoc.bind(data))

println writer
