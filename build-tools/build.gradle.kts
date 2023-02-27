plugins {
	application
	alias(libs.plugins.kotlin)
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(8))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.slf4j.api)
	implementation(libs.bundles.asm)
	implementation(libs.bundles.kotlin)

	runtimeOnly(libs.slf4j.simple)
}

tasks.jar {
	manifest.attributes("Main-Class" to "cc.squiddev.cobalt.build.Main")
}
