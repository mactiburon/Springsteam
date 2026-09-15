plugins {
    `java-library`
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

dependencies {
    api("org.springframework:spring-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}