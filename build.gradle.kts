plugins {
	java
	`maven-publish`
}

group = "org.squiddev"
version = "0.7.3"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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
		options.compilerArgs.addAll(arguments())
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

tasks.jar {
	from(sourceSets["doubles"].output)
}

publishing {
	publications {
		register<MavenPublication>("maven") {
			from(components["java"])

			pom {
				name = "Cobalt"
				description = "A reentrant fork of LuaJ for Lua 5.1"
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
