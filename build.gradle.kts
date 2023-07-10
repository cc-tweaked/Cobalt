plugins {
	java
	`maven-publish`
	id("com.github.hierynomus.license") version "0.16.1"
}

group = "org.squiddev"
version = "0.7.1"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}

	withSourcesJar()
}

sourceSets {
	// Put double conversion in a separate library, so we can run the signedness checker on it.
	register("doubles")
}

repositories {
	mavenCentral()
}

val buildTools by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	compileOnly(libs.checkerFramework.qual)
	implementation(sourceSets["doubles"].output)

	testCompileOnly(libs.checkerFramework.qual)
	testImplementation(libs.bundles.test)
	testRuntimeOnly(libs.bundles.testRuntime)

	testAnnotationProcessor(libs.bundles.testAnnotationProcessor)

	"buildTools"(project(":build-tools"))
}

/**
 * Configure the checker framework for a given source set.
 */
fun configureChecker(sourceSet: SourceSet, arguments: () -> List<String>) {
	dependencies {
		add(sourceSet.compileOnlyConfigurationName, libs.checkerFramework.qual)
		add(sourceSet.annotationProcessorConfigurationName, libs.checkerFramework)
	}

	tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class) {
		options.isFork = true
		options.compilerArgumentProviders.add {
			arguments()
		}
		options.forkOptions.jvmArgumentProviders.add {
			listOf(
				"--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
				"--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
				"--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
			)
		}
	}
}

configureChecker(sourceSets["doubles"]) {
	listOf("-processor", "org.checkerframework.checker.signedness.SignednessChecker")
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

tasks.jar {
	from(sourceSets["doubles"].output)
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

tasks.withType(JavaCompile::class) {
	options.compilerArgs.add("-Xlint")
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("skipped", "failed")
	}
}
