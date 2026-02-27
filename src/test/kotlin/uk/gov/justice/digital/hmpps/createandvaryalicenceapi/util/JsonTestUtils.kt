package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.junit.jupiter.api.Assertions.fail as jupiterFail

@Component
class JsonTestUtils(
  private val objectMapper: ObjectMapper,
) {

  private val templateEngine: TemplateEngine by lazy {
    val resolver = ClassLoaderTemplateResolver().apply {
      prefix = "test_data/tl/responses/"
      suffix = ".json"
      characterEncoding = "UTF-8"
      templateMode = org.thymeleaf.templatemode.TemplateMode.JAVASCRIPT
    }
    TemplateEngine().apply { setTemplateResolver(resolver) }
  }

  fun processTemplate(templateName: String, variables: Map<String, *>): String {
    val context = Context().apply {
      setVariables(variables)
    }
    return templateEngine.process(templateName, context)
  }

  fun assertJsonEquals(templateName: String, variables: Map<String, *>, actualJson: String) {
    val expectedJson = processTemplate(templateName, variables)
    val expectedNode = objectMapper.readTree(expectedJson)
    val actualNode = objectMapper.readTree(actualJson)

    log.info(
      "Expected JSON (from template $templateName):\n{}",
      objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedNode),
    )
    log.info(
      "Actual JSON (from response):\n{}",
      objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualNode),
    )

    assertJsonEqualsDetailed(expectedNode, actualNode)
  }

  fun assertJsonEquals(templateName: String, variables: Map<String, *>, responseSpec: WebTestClient.ResponseSpec) {
    val actualJson = extractJson(responseSpec)
    assertJsonEquals(templateName, variables, actualJson)
  }

  fun extractJson(responseSpec: WebTestClient.ResponseSpec): String = responseSpec
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody()
    .returnResult()
    .responseBody!!
    .toString(Charsets.UTF_8)

  private fun assertJsonEqualsDetailed(expected: JsonNode, actual: JsonNode, path: String = "$", ignoreUnexpectedFields: Boolean = true) {
    when {
      expected.isObject && actual.isObject -> {
        val expectedFields = expected.fieldNames().asSequence().toSet()
        val actualFields = actual.fieldNames().asSequence().toSet()

        for (field in expectedFields - actualFields) {
          failTest("Missing field at $path.$field")
        }

        if (!ignoreUnexpectedFields) {
          for (field in actualFields - expectedFields) {
            failTest("Unexpected field at $path.$field")
          }
        }

        for (field in expectedFields.intersect(actualFields)) {
          assertJsonEqualsDetailed(expected.get(field), actual.get(field), "$path.$field")
        }
      }

      expected.isArray && actual.isArray -> {
        if (expected.size() != actual.size()) {
          failTest("Array size mismatch at $path: expected ${expected.size()} but got ${actual.size()}")
        }
        for (i in 0 until expected.size()) {
          assertJsonEqualsDetailed(expected.get(i), actual.get(i), "$path[$i]")
        }
      }

      else -> {
        if (expected != actual) {
          failTest("Value mismatch at $path: expected '$expected' but got '$actual'")
        }
      }
    }
  }

  private fun failTest(message: String): Nothing = jupiterFail(message)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
