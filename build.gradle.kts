plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.10.0"
  kotlin("plugin.spring") version "1.9.21"
  kotlin("plugin.jpa") version "1.9.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}
dependencyCheck {
  suppressionFiles.add("detekt-suppressions.xml")
}
val integrationTest = task<Test>("integrationTest") {
  description = "Integration tests"
  group = "verification"
  shouldRunAfter("test")
}

tasks.register<Copy>("installLocalGitHook") {
  from(File(rootProject.rootDir, ".scripts/pre-commit"))
  into(File(rootProject.rootDir, ".git/hooks"))
  fileMode = "755".toInt(radix = 8)
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
  implementation("org.springframework.boot:spring-boot-starter-cache")

  // GOVUK Notify:
  implementation("uk.gov.service.notify:notifications-java-client:4.1.0-RELEASE")

  // PDF Box - for processing MapMaker file upload to get image / text for exclusion zone
  implementation("org.apache.pdfbox:pdfbox:2.0.29")
  implementation("org.apache.pdfbox:jbig2-imageio:3.0.4")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  implementation("com.google.code.gson:gson:2.10.1")
  implementation("io.arrow-kt:arrow-core:1.2.1")
  implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.5.1")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

  // Test dependencies
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-orgjson:0.12.3")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.0.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.16")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
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

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
  withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
      html.required.set(true) // observe findings in your browser with structure and code snippets
    }
  }
  named("check").configure {
    this.setDependsOn(
      this.dependsOn.filterNot {
        it is TaskProvider<*> && it.name == "detekt"
      }
    )
  }
}
