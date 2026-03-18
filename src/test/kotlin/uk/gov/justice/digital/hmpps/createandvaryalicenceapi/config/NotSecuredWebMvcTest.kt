package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ActiveProfiles("test")
@WebMvcTest(
  excludeAutoConfiguration = [
    OAuth2ResourceServerAutoConfiguration::class,
    OAuth2ClientAutoConfiguration::class,
  ],
)
@AutoConfigureMockMvc
annotation class NotSecuredWebMvcTest(
  @get:AliasFor(annotation = WebMvcTest::class, attribute = "controllers")
  val controllers: Array<KClass<*>> = [],
)
