import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("dev.detekt") version "2.0.0-alpha.2"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.2.1"
  id("org.owasp.dependencycheck") version "12.2.0"
  kotlin("plugin.spring") version "2.3.20"
  kotlin("plugin.jpa") version "2.3.20"
}

repositories {
  mavenCentral()
}

ext["logback.version"] = "1.5.25"

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // hmpps-kotlin-lib
  constraints {
    implementation("org.apache.commons:commons-compress:1.26.0") {
      because("1.24.0 has CVE-2024-25710 and CVE-2024-26308 vulnerabilities")
    }
    // FIX: CVE-2026-23907
    implementation("org.apache.pdfbox:pdfbox:3.0.7") {
      because("Fix CVE-2026-23907")
    }
  }

  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.1.0")

  // CVE-2026-33871 - it does not fix all occurrences
  implementation(enforcedPlatform("io.netty:netty-bom:4.2.12.Final"))
  implementation("io.netty:netty-buffer")
  implementation("io.netty:netty-codec-http")
  implementation("io.netty:netty-handler")
  implementation("io.netty:netty-transport")
  // END of CVE-2025-67735 - Remove when fixed

  // Fix for CVE-2025-48924
  implementation("org.apache.commons:commons-lang3:3.20.0")

  // Fix for CVE-2025-68161 -  () - maven/org.apache.logging.log4j/log4j-api@2.25.0
  implementation(enforcedPlatform("org.apache.logging.log4j:log4j-bom:2.25.4"))
  implementation("org.apache.logging.log4j:log4j-api")
  // End of CVE-2025-68161 remove when not needed.

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-flyway")

  // GOVUK Notify:
  implementation("uk.gov.service.notify:notifications-java-client:6.0.0-RELEASE")

  // PDF Box - for processing MapMaker file upload to get image / text for exclusion zone
  implementation("org.apache.pdfbox:pdfbox:3.0.7")
  implementation("org.apache.pdfbox:jbig2-imageio:3.0.4")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.10")
  implementation("com.google.code.gson:gson:2.13.2")
  implementation("io.arrow-kt:arrow-core:2.2.2.1")

  // SQS/SNS dependencies
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.3.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

  // Digital prison reporting
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:12.0.0")

  // To help override SAR
  implementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-lib:2.1.4")
  implementation("org.jsoup:jsoup:1.22.1")

  // New in Spring Boot 4: Dedicated starter for HTTP clients
  implementation("org.springframework.boot:spring-boot-starter-webclient")

  // Required for @AutoConfigureWebTestClient and testing WebClient
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")

  // Update to a version compatible with Spring Boot 4.0
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")

  // Test dependencies
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-orgjson:0.13.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.3")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-test-support:2.1.2")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.2")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.springframework:spring-test")

  // Specifically for Spring Boot 4 Web MVC testing
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

detekt {
  ignoreFailures.set(false)
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
        useVersion("2.3.0")
      }
    }
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JVM_21
      freeCompilerArgs.addAll(
        "-Xwhen-guards",
        "-Xjvm-default=all",
        "-Xjsr305=warn",
        "-Xtype-enhancement-improvements-strict-mode=false",
        "-Xjspecify-annotations=ignore",
      )
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
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
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
  suppressionFiles.add("cvl-api-suppressions.xml")
}
