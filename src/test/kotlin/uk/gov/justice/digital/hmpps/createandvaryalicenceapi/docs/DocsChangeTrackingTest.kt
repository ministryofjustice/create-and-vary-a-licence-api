package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.docs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.security.MessageDigest

/**
 * Certain classes (or packages) implement significant business/policy rules in code,
 * with human-readable documentation kept separately under `docs/`. This test ensures
 * that documentation doesn't silently go stale: each entry in `docs-change-tracking.yaml`
 * records the MD5 hash of the source file(s) it documents, as of the last time the
 * documentation was reviewed.
 *
 * If the source changes, the computed hash will no longer match the recorded hash, and
 * the corresponding dynamic test below will fail - prompting the developer to:
 *   1. review the linked markdown doc and update it if the business rules changed, then
 *   2. update the `hash` value in `docs-change-tracking.yaml` to the new hash reported in
 *      the failure message.
 *
 * See `docs-change-tracking.yaml` for the registry format.
 */
class DocsChangeTrackingTest {

  private val repoRoot: File = findRepoRoot()

  @TestFactory
  fun `documented business logic is up to date`(): List<DynamicTest> {
    val registry = loadRegistry()

    return registry.entries.map { entry ->
      dynamicTest(entry.id) { assertEntryIsUpToDate(entry) }
    }
  }

  private fun assertEntryIsUpToDate(entry: RegistryEntry) {
    val docFile = repoRoot.resolve(entry.doc)
    assertThat(docFile)
      .withFailMessage(
        "Documentation file '${entry.doc}' referenced by entry '${entry.id}' in " +
          "$REGISTRY_FILE does not exist. Has it been moved or renamed?",
      )
      .exists()

    val sourceFiles = resolveSourceFiles(entry)
    assertThat(sourceFiles)
      .withFailMessage(
        "Entry '${entry.id}' in $REGISTRY_FILE has no resolvable source files (checked: " +
          "${entry.sources}). Update the entry's 'sources' list.",
      )
      .isNotEmpty

    val actualHash = hashOf(sourceFiles)

    assertThat(actualHash)
      .withFailMessage(
        "Business logic covered by entry '${entry.id}' in $REGISTRY_FILE has changed " +
          "(source files: ${sourceFiles.map { it.relativeTo(repoRoot).path }}).\n" +
          "Please review the documentation at '${entry.doc}', update it if the business " +
          "rules changed, then update the entry in $REGISTRY_FILE to:\n" +
          "    hash: $actualHash.\n" +
          "Please also ensure any changes are reflected in implementation notes in confluence.",
      )
      .isEqualTo(entry.hash)
  }

  private fun resolveSourceFiles(entry: RegistryEntry): List<File> = entry.sources
    .flatMap { path ->
      val resolved = repoRoot.resolve(path)
      when {
        resolved.isDirectory -> resolved.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        resolved.isFile -> listOf(resolved)
        else -> emptyList()
      }
    }
    .sortedBy { it.relativeTo(repoRoot).path }

  private fun hashOf(files: List<File>): String {
    val digest = MessageDigest.getInstance("MD5")
    files.forEach { file ->
      digest.update(file.relativeTo(repoRoot).path.toByteArray(Charsets.UTF_8))
      digest.update(0)
      digest.update(file.readBytes())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun loadRegistry(): Registry {
    val registryFile = repoRoot.resolve(REGISTRY_FILE)
    assertThat(registryFile)
      .withFailMessage("Could not find $REGISTRY_FILE at repo root: ${registryFile.path}")
      .exists()

    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    return mapper.readValue(registryFile)
  }

  private fun findRepoRoot(): File {
    var dir = File(System.getProperty("user.dir")).absoluteFile
    while (!File(dir, REGISTRY_FILE).exists()) {
      dir =
        dir.parentFile ?: error("Could not locate $REGISTRY_FILE by walking up from ${System.getProperty("user.dir")}")
    }
    return dir
  }

  companion object {
    private const val REGISTRY_FILE = "docs-change-tracking.yaml"
  }
}

private data class Registry(val entries: List<RegistryEntry> = emptyList())

private data class RegistryEntry(
  val id: String,
  val description: String = "",
  val doc: String,
  val sources: List<String>,
  val hash: String,
)
