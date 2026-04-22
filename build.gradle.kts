plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
    jacoco
}

group = "ovo.sypw"
version = "0.0.1-SNAPSHOT"
description = "OnlineExamSystemBack"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}
dependencies{
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // OpenAPI documentation - use springdoc directly for Spring Boot 4.0
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // File Upload
    implementation("commons-io:commons-io:2.15.1")
    
    // Baidu Cloud BOS SDK - exclude old commons-io to avoid conflicts with POI
    implementation("com.baidubce:bce-java-sdk:0.10.165") {
        exclude(group = "commons-io", module = "commons-io")
    }
    
    // HTTP Client for OpenAI API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Apache POI for Excel import/export
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("org.apache.poi:poi:5.4.0")
    
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

configurations.all {
    resolutionStrategy {
        force("commons-io:commons-io:2.15.1")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
