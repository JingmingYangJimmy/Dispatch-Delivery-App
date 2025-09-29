plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.delivery"
version = "0.0.1-SNAPSHOT"
description = "routeplanning_complete"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("javax.persistence:javax.persistence-api:2.2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
