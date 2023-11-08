plugins {
	application
	alias(libs.plugins.kotlin)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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
	manifest.attributes("Main-Class" to "cc.tweaked.cobalt.build.Main")
}
