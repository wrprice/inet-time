plugins {
    `java-library`
    `jacoco`
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.vanniktech.maven.publish)
}

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort

repositories {
    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit.tests)
    testRuntimeOnly(libs.bundles.junit.runtime)

    mockitoAgent(libs.mockito.core) { isTransitive = false }

    spotbugs(libs.spotbugs)
    spotbugsPlugins(libs.findsecbugs)

    jacocoAgent(libs.jacoco)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25) // 23+ for markdown Javadoc; see compiler args
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.1")
        }
    }
}

val group: String by project
val artifactId: String by project
val version: String by project
val isRelease: String by project
val publishVersion: String by extra { version + (if (isRelease.toBoolean()) "" else "-SNAPSHOT") }
mavenPublishing {
    signAllPublications()
    publishToMavenCentral(automaticRelease=false)
    coordinates(group, artifactId, publishVersion)
    configureBasedOnAppliedPlugins()

    pom {
        name.set(project.description)
        description.set("Value, unit, and field types supporting .Beats with the Java Time API")
        inceptionYear.set("2025")
        url.set("https://github.com/wrprice/inet-time")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
          developer {
            id.set("wrprice")
            name.set("William R. Price")
            url.set("https://github.com/wrprice")
          }
        }
        scm {
          url.set("https://github.com/wrprice/inet-time")
          connection.set("scm:git:git://github.com/wrprice/inet-time.git")
          developerConnection.set("scm:git:ssh://git@github.com/wrprice/inet-time.git")
        }
    }
}

tasks {
    jar {
        manifest {
            attributes["Implementation-Version"] = project.version
            attributes["Implementation-Vendor"] = "William R. Price"
        }
    }

    test {
        jvmArgs("-javaagent:${mockitoAgent.asPath}")
        finalizedBy("jacocoTestReport")

        testLogging {
            events("failed", "skipped" /*, "passed"*/)
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    jacocoTestReport {
        reports {
            csv.required = true
        }
        dependsOn("test")
    }

    spotbugs {
        effort = Effort.MAX
        reportLevel = Confidence.DEFAULT // limiting to HIGH confidence would report fewer issues
        showProgress = true
    }

    spotbugsMain {
        reports.create("html") {
            required = true
            outputLocation = file("build/reports/spotbugs-main.html")
            setStylesheet("fancy-hist.xsl")
        }
    }

    spotbugsTest {
        reports.create("html") {
            required = true
            outputLocation = file("build/reports/spotbugs-test.html")
            setStylesheet("fancy-hist.xsl")
        }
    }

    check {
        dependsOn("javadoc")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Compile library for compatibility with Java 21 -- the earliest LTS w/ switch pattern support
    options.compilerArgs.add("--release")
    options.compilerArgs.add("21")

    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("--module-version")
    options.compilerArgs.add("${project.version}")
}

tasks.withType<Javadoc>().configureEach {
    options {
        // https://github.com/gradle/gradle/issues/7038
        this as StandardJavadocDocletOptions // unsafe cast
        // links("https://docs.gradle.org/4.9/javadoc/") // now we can use all props of StandardJavadocDocletOptions
        // noQualifiers = listOf("all")

        memberLevel = JavadocMemberLevel.PROTECTED
        addBooleanOption("html5", true)
        addBooleanOption("Xdoclint:all,-missing", true)
    }
}
