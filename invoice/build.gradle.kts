plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":util"))
    implementation(project(":tenant"))
    implementation(project(":contact"))
    implementation(project(":ledger"))
    implementation(project(":company"))
    implementation(project(":ledger"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("com.itextpdf:itext7-core:9.2.0")
    runtimeOnly("org.postgresql:postgresql")
}
