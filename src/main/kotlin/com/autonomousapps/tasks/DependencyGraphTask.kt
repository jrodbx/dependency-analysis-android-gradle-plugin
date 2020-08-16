package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Dependency
import com.autonomousapps.graph.ConsumerNode
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.graph.Node
import com.autonomousapps.graph.ProducerNode
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.VariantClass
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class DependencyGraphTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces the dependency graph for the current project"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   * It is the result of resolving `runtimeClasspath`. cf. [configuration]
   */
  @get:Classpath
  abstract val artifactFiles: ConfigurableFileCollection

  /**
   * This is what the task actually uses as its input. We really only care about the
   * [ResolutionResult]. cf. [artifactFiles].
   */
  @get:Internal
  lateinit var configuration: Configuration

  @get:InputFile
  abstract val components: RegularFileProperty

  @get:InputFile
  abstract val usedClasses: RegularFileProperty

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    val outputJsonFile = outputJson.getAndDelete()
    val outputDotFile = outputDot.getAndDelete()

    val components = components.fromJsonList<Component>()
    val usedClasses = usedClasses.fromJsonList<VariantClass>().mapToSet { it.theClass }

    val graph = GraphBuilder(
      root = configuration.incoming.resolutionResult.root,
      components = components,
      usedClasses = usedClasses
    ).buildGraph()

    logger.quiet("Graph JSON at ${outputJsonFile.path}")
    outputJsonFile.writeText(graph.toJson())

    logger.quiet("Graph DOT at ${outputDotFile.path}")
    outputDotFile.writeText(graph.toDot())
  }
}

private class GraphBuilder(
  private val root: ResolvedComponentResult,
  private val components: List<Component>,
  private val usedClasses: Set<String>
) {

  private val graph = DependencyGraph()
  private val nodes = mutableListOf<Node>()

  /**
   * Returns a [DependencyGraph]. Not a copy, can be mutated.
   */
  fun buildGraph(): DependencyGraph {
    traverse(root, true)
    return graph
  }

  fun printGraph() {
    println("GRAPH START >>>")
    println(graph.toString())
    println("<<< GRAPH END")
  }

  private fun traverse(root: ResolvedComponentResult, isConsumer: Boolean = false) {
    val rootDep = root.toDependency()
    val rootComponent = components.find { it.dependency == rootDep }
    // While most nodes are the roots of subgraphs, only one is the absolute root (with in-degree=0)
    val rootNode = if (isConsumer) {
      ConsumerNode(identifier = rootDep.identifier, classes = usedClasses)
    } else {
      ProducerNode(identifier = rootDep.identifier, component = rootComponent)
    }

    // Don't visit the same node more than once
    if (nodes.contains(rootNode)) {
      return
    }
    nodes.add(rootNode)

    root.dependencies.filterIsInstance<ResolvedDependencyResult>()
      .map { dependencyResult ->
        val componentResult = dependencyResult.selected
        val dependency = componentResult.toDependency()

        val component = components.find { it.dependency == dependency }
        val depNode = ProducerNode(
          identifier = dependency.identifier,
          component = component
        )

        graph.addEdge(rootNode, depNode)
        traverse(componentResult)
      }
  }

  private fun ResolvedComponentResult.toDependency(): Dependency =
    when (val componentIdentifier = id) {
      is ProjectComponentIdentifier -> Dependency(componentIdentifier)
      is ModuleComponentIdentifier -> Dependency(componentIdentifier)
      else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
    }
}
