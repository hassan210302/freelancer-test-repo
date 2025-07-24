plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":util"))
    implementation(project(":tenant"))
    implementation(project(":contact"))
    implementation(project(":ledger"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    runtimeOnly("org.postgresql:postgresql")
}
