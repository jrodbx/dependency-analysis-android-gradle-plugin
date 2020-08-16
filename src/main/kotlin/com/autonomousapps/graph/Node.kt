package com.autonomousapps.graph

import com.autonomousapps.internal.Component
import com.autonomousapps.internal.KtFile
import com.autonomousapps.internal.utils.flatMapToSet

/**
 * Represents a module in the dependency hierarchy rooted on the project-under-analysis (PUA). May
 * be a [ConsumerNode] (i.e., the PUA), or a [ProducerNode] (i.e. a dependency).
 */
sealed class Node(
  open val identifier: String
) {

  override fun toString(): String = identifier

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Node

    if (identifier != other.identifier) return false

    return true
  }

  override fun hashCode(): Int {
    return identifier.hashCode()
  }
}

/**
 * The project under analysis. It "consumes" its dependencies.
 */
data class ConsumerNode(
  override val identifier: String,
  val classes: Set<String>
) : Node(identifier) {

}

/**
 * A dependency. May be a project or an external binary. It "provides" capabilities for use by the
 * project-under-analysis, represented by [ConsumerNode].
 */
data class ProducerNode(
  override val identifier: String,
  val classes: Set<String> = emptySet(),
  val ktFiles: List<KtFile> = emptyList(),
  val constants: Set<String> = emptySet(),
  val isCompileOnly: Boolean = false,
  val isSecurityProvider: Boolean = false
) : Node(identifier) {

  constructor(identifier: String, component: Component?) : this(
    identifier = identifier,
    classes = component?.classes ?: emptySet(),
    ktFiles = component?.ktFiles ?: emptyList(),
    constants = component?.constantFields?.values?.flatMapToSet { it } ?: emptySet(),
    isCompileOnly = component?.isCompileOnlyAnnotations ?: false,
    isSecurityProvider = component?.isSecurityProvider ?: false
  )
}
