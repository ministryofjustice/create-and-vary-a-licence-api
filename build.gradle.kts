import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.6"
  id("org.owasp.dependencycheck") version "12.1.3"
  kotlin("plugin.spring") version "2.2.10"
  kotlin("plugin.jpa") version "2.2.10"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

repositories {
  mavenCentral()
}

ext["logback.version"] = "1.5.14"

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // hmpps-kotlin-lib
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.5.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  // GOVUK Notify:
  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")

  // PDF Box - for processing MapMaker file upload to get image / text for exclusion zone
  implementation("org.apache.pdfbox:pdfbox:3.0.5")
  implementation("org.apache.pdfbox:jbig2-imageio:3.0.4")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.7")

  implementation("com.google.code.gson:gson:2.13.1")
  implementation("io.arrow-kt:arrow-core:2.1.2")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

  // SQS/SNS dependencies
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

  // Digital prison reporting
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:7.10.5")

  // Test dependencies
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-orgjson:0.13.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.31")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.testcontainers:postgresql:1.21.3")
}

detekt {
  source.setFrom("$projectDir/src/main")
  buildUponDefaultConfig = true // preconfigure defaults
  allRules = false // activate all available (even unstable) rules.
  config.setFrom("$projectDir/detekt.yml") // point to your custom config defining rules to run, overwriting default behavior
  baseline = file("$projectDir/detekt-baseline.xml") // a way of suppressing issues before introducing detekt
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }

  matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
      }
    }
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JVM_21
    compilerOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
  }
  withType<Detekt> {
    reports {
      html.required.set(true) // observe findings in your browser with structure and code snippets
    }
  }
  named<Test>("test") {
    filter {
      excludeTestsMatching("*.integration.*")
    }
  }

  register<Test>("initialiseDatabase", fun Test.() {
    include("**/InitialiseDatabaseTest.class")
  })

  register<Test>("integrationTest") {
    description = "Integration tests"
    group = "verification"
    shouldRunAfter("test")
    useJUnitPlatform()
    filter {
      includeTestsMatching("*IntegrationTest")
    }
  }
  register<Copy>("installLocalGitHook") {
    from(File(rootProject.rootDir, ".scripts/pre-commit"))
    into(File(rootProject.rootDir, ".git/hooks"))
    filePermissions { unix("755") }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

allOpen {
  annotation("jakarta.persistence.Entity")
}

dependencyCheck {
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}
