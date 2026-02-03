import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  id("org.owasp.dependencycheck") version "12.2.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

repositories {
  mavenCentral()
}

ext["logback.version"] = "1.5.19"

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // hmpps-kotlin-lib
  constraints {
    implementation("org.apache.commons:commons-compress:1.26.0") {
      because("1.24.0 has CVE-2024-25710 and CVE-2024-26308 vulnerabilities")
    }
  }
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.8.2")

  // CVE-2025-67735 - it does not fix all occurrences
  implementation(enforcedPlatform("io.netty:netty-bom:4.2.8.Final"))
  implementation("io.netty:netty-buffer")
  implementation("io.netty:netty-codec-http")
  implementation("io.netty:netty-handler")
  implementation("io.netty:netty-transport")
  // END of CVE-2025-67735 - Remove when fixed

  // Fix for CVE-2025-48924
  implementation("org.apache.commons:commons-lang3:3.18.0")

  // Fix for CVE-2025-68161 -  () - maven/org.apache.logging.log4j/log4j-api@2.25.0
  implementation(enforcedPlatform("org.apache.logging.log4j:log4j-bom:2.25.1"))
  implementation("org.apache.logging.log4j:log4j-api")
  // End of CVE-2025-68161 remove when not needed.

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  // GOVUK Notify:
  implementation("uk.gov.service.notify:notifications-java-client:6.0.0-RELEASE")

  // PDF Box - for processing MapMaker file upload to get image / text for exclusion zone
  implementation("org.apache.pdfbox:pdfbox:3.0.6")
  implementation("org.apache.pdfbox:jbig2-imageio:3.0.4")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  implementation("com.google.code.gson:gson:2.13.2")
  implementation("io.arrow-kt:arrow-core:2.2.1.1")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.14.1")

  // SQS/SNS dependencies
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.3")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

  // Digital prison reporting
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:9.11.20")

  // Test dependencies
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-orgjson:0.13.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.37")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.3")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
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
  testImplementation {
    exclude(group = "org.junit.vintage")
    exclude(group = "org.mozilla:rhino")
  }

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
      includeTestsMatching("*IntegrationTest*")
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
