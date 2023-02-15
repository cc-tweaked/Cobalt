plugins {
	java
	`maven-publish`
	id("com.github.hierynomus.license") version "0.16.1"
	id("org.checkerframework") version "0.6.17"
}

group = "org.squiddev"
version = "0.6.0"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(8))
	}

	withSourcesJar()
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
	testImplementation("org.hamcrest:hamcrest-library:2.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")

	testImplementation("org.openjdk.jmh:jmh-core:1.23")
	testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.23")
}

license {
	include("*.java")
}

checkerFramework {
	// add '-PskipCheckerFramework' to the gradle command line to speed up the build
	checkers = listOf(
		"org.checkerframework.checker.signedness.SignednessChecker"
	)
	extraJavacArgs = listOf(
		"-AonlyDefs=^org\\.squiddev\\.cobalt\\.lib\\.doubles\\."
	)
}

publishing {
	publications {
		register<MavenPublication>("maven") {
			from(components["java"])

			pom {
				name.set("Cobalt")
				description.set("A reentrant fork of LuaJ for Lua 5.1")
				url.set("https://github.com/SquidDev/Cobalt")

				scm {
					url.set("https://github.com/SquidDev/Cobalt.git")
				}

				issueManagement {
					system.set("github")
					url.set("https://github.com/SquidDev/Cobalt/issues")
				}

				licenses {
					license {
						name.set("MIT")
						url.set("https://github.com/SquidDev/Cobalt/blob/master/LICENSE")
					}
				}
			}
		}
	}

	repositories {
		maven("https://squiddev.cc/maven") {
			name = "SquidDev"
			credentials(PasswordCredentials::class)
		}
	}
}

sourceSets.configureEach {
	val sourceSet = this
	tasks.named(compileJavaTaskName, JavaCompile::class) {
		options.compilerArgs.add("-Xlint")

		// Only enable skipCheckerFramework on main.
		extensions.configure(org.checkerframework.gradle.plugin.CheckerFrameworkTaskExtension::class) {
			skipCheckerFramework = sourceSet.name != "main"
		}
	}
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("skipped", "failed")
	}
}
