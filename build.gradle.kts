import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
	java
	`maven-publish`
}

group = "cc.tweaked"
version = "0.9.3"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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

val checkerFramework by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	compileOnly(libs.checkerFramework.qual)

	"checkerFramework"(libs.checkerFramework)

	testCompileOnly(libs.checkerFramework.qual)
	testImplementation(libs.bundles.test)
	testRuntimeOnly(libs.bundles.testRuntime)

	testAnnotationProcessor(libs.bundles.testAnnotationProcessor)

	"buildTools"(project(":build-tools"))
}

/**
 * Create a task which runs checker framework against our source code.
 *
 * This runs separately from the main compile task, allowing us to run checker framework multiple times with different
 * options.
 */
fun runCheckerFramework(name: String, args: List<String>) {
	val sourceSet = sourceSets.main.get()

	val runCheckerFramework = tasks.register("checker${name.uppercaseFirstChar()}", JavaCompile::class) {
		description = "Runs CheckerFramework against the $name sources."
		group = LifecycleBasePlugin.VERIFICATION_GROUP

		javaCompiler = javaToolchains.compilerFor(java.toolchain)
		source = sourceSet.java
		classpath = sourceSets.main.get().compileClasspath

		destinationDirectory = layout.buildDirectory.dir("classes/java/${sourceSet.name}Doubles")

		options.annotationProcessorPath = checkerFramework
		options.compilerArgs.add("-proc:only")
		options.compilerArgs.addAll(args)
		options.isFork = true
		options.forkOptions.jvmArgs!!.addAll(
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
			),
		)
	}

	tasks.check { dependsOn(runCheckerFramework) }
}

runCheckerFramework(
	"doubles",
	listOf(
		"-processor", "org.checkerframework.checker.signedness.SignednessChecker",
		"""-AonlyDefs=^cc\.tweaked\.cobalt\.internal\.doubles\.""",
	),
)

runCheckerFramework(
	"nonNull",
	listOf(
		"-processor", "org.checkerframework.checker.nullness.NullnessChecker",
		"""-AonlyDefs=^cc\.tweaked\.cobalt\.""",
	),
)

// Point compileJava to emit to classes/uninstrumentedJava/main, and then add a task to instrument these classes,
// saving them back to the the original class directory. This is held together with so much string :(.
val mainSource = sourceSets.main.get()
val javaClassesDir = mainSource.java.classesDirectory.get()
val untransformedClasses = project.layout.buildDirectory.dir("classes/uninstrumentedJava/main")

val instrumentJava = tasks.register(mainSource.getTaskName("Instrument", "Java"), JavaExec::class) {
	dependsOn(tasks.compileJava, "cleanInstrumentJava")
	inputs.dir(untransformedClasses).withPropertyName("inputDir")
	outputs.dir(javaClassesDir).withPropertyName("outputDir")

	javaLauncher = javaToolchains.launcherFor(java.toolchain)
	mainClass = "cc.tweaked.cobalt.build.MainKt"
	classpath = buildTools

	args = listOf(
		untransformedClasses.get().asFile.absolutePath,
		javaClassesDir.asFile.absolutePath,
	)
}

mainSource.compiledBy(instrumentJava)
tasks.compileJava {
	destinationDirectory = untransformedClasses
	finalizedBy(instrumentJava)
}

publishing {
	publications {
		register<MavenPublication>("maven") {
			from(components["java"])

			pom {
				name = "Cobalt"
				description = "A reentrant fork of LuaJ for Lua 5.2"
				url = "https://github.com/SquidDev/Cobalt"

				scm {
					url = "https://github.com/SquidDev/Cobalt.git"
				}

				issueManagement {
					system = "github"
					url = "https://github.com/SquidDev/Cobalt/issues"
				}

				licenses {
					license {
						name = "MIT"
						url = "https://github.com/SquidDev/Cobalt/blob/master/LICENSE"
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

val benchmark by tasks.registering(JavaExec::class) {
	description = "Run our benchmarking suite"

	classpath(sourceSets.test.map { it.runtimeClasspath })
	mainClass = "cc.tweaked.cobalt.benchmark.BenchmarkFull"
}
