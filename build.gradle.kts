plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.2.2"
  kotlin("plugin.spring") version "1.8.22"
  kotlin("plugin.jpa") version "1.8.22"
  id("io.gitlab.arturbosch.detekt") version "1.23.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}
val integrationTest = task<Test>("integrationTest") {
  description = "Integration tests"
  group = "verification"
  shouldRunAfter("test")
}

tasks.named<Test>("integrationTest") {
  useJUnitPlatform()
  filter {
    includeTestsMatching("*.integration.*")
  }
}

tasks.named<Test>("test") {
  filter {
    excludeTestsMatching("*.integration.*")
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // GOVUK Notify:
  implementation("uk.gov.service.notify:notifications-java-client:4.1.0-RELEASE")

  // PDF Box - for processing MapMaker file upload to get image / text for exclusion zone
  implementation("org.apache.pdfbox:pdfbox:2.0.28")
  implementation("org.apache.pdfbox:jbig2-imageio:3.0.4")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  implementation("com.google.code.gson:gson:2.10.1")
  implementation("io.arrow-kt:arrow-core:1.1.5")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

  // Test dependencies
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-orgjson:0.11.5")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.3")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}
repositories {
  mavenCentral()
}

detekt {
  source.setFrom("$projectDir/src/main")
  buildUponDefaultConfig = true // preconfigure defaults
  allRules = false // activate all available (even unstable) rules.
  config.setFrom("$projectDir/detekt.yml") // point to your custom config defining rules to run, overwriting default behavior
  baseline = file("$projectDir/detekt-baseline.xml") // a way of suppressing issues before introducing detekt
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.8.21")
    }
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
  withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
      html.required.set(true) // observe findings in your browser with structure and code snippets
    }
  }
  getByPath("detekt").onlyIf { project.hasProperty("runDetekt") }
}
