package com.autonomousapps.graph

/**
 * TODO.
 *
 * With inspiration from [Algorithms](https://algs4.cs.princeton.edu/42digraph/Digraph.java.html).
 */
class DependencyGraph {

  private var edgeCount: Int = 0
  private val adj = LinkedHashMap<Node, MutableSet<Node>>()
  private val inDegree = LinkedHashMap<Node, Int>()

  companion object {
    fun newGraph(map: Map<Node, Set<Node>>): DependencyGraph {
      val graph = DependencyGraph()
      map.forEach { (from, tos) ->
        tos.forEach { to ->
          graph.addEdge(from, to)
        }
      }
      return graph
    }
  }

  fun nodeCount(): Int = adj.size
  fun edgeCount(): Int = edgeCount

  /**
   * Adds the directed edge v→w to this digraph.
   *
   * @param from the from vertex
   * @param to the to vertex
   */
  fun addEdge(from: Node, to: Node) {
    var added = true
    adj.merge(from, mutableSetOf(to)) { set, increment ->
      added = set.addAll(increment)
      set
    }
    // Only increment these things if we actually added a new edge. This graph does not support
    // parallel edges
    if (added) {
      inDegree.merge(to, 1) { old, new -> old + new }
      edgeCount++
    }
  }

  /**
   * Returns the vertices adjacent from vertex `from` in this digraph.
   *
   * @param  from the vertex
   * @return the vertices adjacent from vertex `from` in this digraph, as an iterable
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun adj(from: Node): Iterable<Node> = adj[from] ?: missingNode(from)

  /**
   * Returns a map representation of the graph.
   */
  fun map(): Map<Node, Set<Node>> = LinkedHashMap<Node, Set<Node>>().apply {
    putAll(adj) // TODO deep-copy each node? If they're immutable it wouldn't matter.
  }

  /**
   * Returns the number of directed edges incident from vertex `from`.
   *
   * @param  from the vertex
   * @return the outdegree of vertex `from`
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun outdegree(from: Node): Int {
    return adj[from]?.size ?: missingNode(from)
  }

  /**
   * Returns the number of directed edges incident to vertex `from`.
   *
   * @param  from the vertex
   * @return the indegree of vertex `from`
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun indegree(from: Node): Int {
    return inDegree[from] ?: missingNode(from)
  }

  /**
   * Returns the reverse of the digraph.
   *
   * @return the reverse of the digraph
   */
  fun reverse(): DependencyGraph {
    val reverse = DependencyGraph()
    for (v in adj.keys) {
      for (w in adj(v)) {
        reverse.addEdge(w, v)
      }
    }
    return reverse
  }

  /**
   * Returns a string representation of the graph.
   *
   * @return the number of vertices *nodeCount*, followed by the number of edges *edgeCount*,
   * followed by the *node* adjacency lists
   */
  override fun toString(): String = buildString {
    append("${nodeCount()} vertices, $edgeCount edges\n")
    adj.forEach { (node, edges) ->
      append("$node >> ")
      append(edges.joinToString(separator = ", "))
      append("\n")
    }
  }

  // TODO this probably shouldn't be an instance method on this class (meaning it should not be this class's responsibility to render itself in the DOT format)
  fun toDot(): String = buildString {
    append("digraph G {\n")
    //  ranksep=4; nodesep=0.1
    append("\n")
    adj.forEach { (from, tos) ->
      val fromLabel = from.identifier
      tos.forEach { to ->
        append("  \"$fromLabel\" -> \"${to.identifier}\";\n")
      }
    }
    append("}")
  }

  private fun missingNode(v: Node): Nothing {
    throw IllegalArgumentException("Node $v is not in the graph")
  }
}
