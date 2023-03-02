plugins {
	java
	`maven-publish`
	id("com.github.hierynomus.license") version "0.16.1"
	alias(libs.plugins.checkerFramework)
}

group = "org.squiddev"
version = "0.6.0"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}

	withSourcesJar()
}

repositories {
	mavenCentral()
}

val buildTools by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	testImplementation(libs.bundles.test)
	testRuntimeOnly(libs.bundles.testRuntime)

	testAnnotationProcessor(libs.bundles.testAnnotationProcessor)

	"buildTools"(project(":build-tools"))
}

// Point compileJava to emit to classes/uninstrumentedJava/main, and then add a task to instrument these classes,
// saving them back to the the original class directory. This is held together with so much string :(.
val mainSource = sourceSets.main.get()
val javaClassesDir = mainSource.java.classesDirectory.get()
val untransformedClasses = project.layout.buildDirectory.dir("classes/uninstrumentedJava/main")

val instrumentJava = tasks.register(mainSource.getTaskName("Instrument", "Java"), JavaExec::class) {
	dependsOn(tasks.compileJava)
	inputs.dir(untransformedClasses).withPropertyName("inputDir")
	outputs.dir(javaClassesDir).withPropertyName("outputDir")

	javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
	mainClass.set("cc.squiddev.cobalt.build.MainKt")
	classpath = buildTools

	args = listOf(
		untransformedClasses.get().asFile.absolutePath,
		javaClassesDir.asFile.absolutePath,
	)

	doFirst { project.delete(javaClassesDir) }
}

mainSource.compiledBy(instrumentJava)
tasks.compileJava {
	destinationDirectory.set(untransformedClasses)
	finalizedBy(instrumentJava)
}

license {
	include("*.java")
}

checkerFramework {
	// add '-PskipCheckerFramework' to the gradle command line to speed up the build
	checkers = listOf("org.checkerframework.checker.signedness.SignednessChecker")
	extraJavacArgs = listOf("-AonlyDefs=^org\\.squiddev\\.cobalt\\.lib\\.doubles\\.")
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
