package com.autonomousapps.graph

import org.junit.Test

class DependencyGraphTest {

  @Test fun test() {
    val node1 = ConsumerNode(":proj")
    val node2 = ProducerNode("junit")
    val node3 = ProducerNode("truth")

    val graph = DependencyGraph(3)
    graph.addEdge(node1, node2)
    graph.addEdge(node1, node3)
    graph.addEdge(node2, node3)

    println("GRAPH\n$graph")
  }
}