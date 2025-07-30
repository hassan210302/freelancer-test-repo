plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.javamoney:moneta:1.4.5")
    implementation("com.itextpdf:itext7-core:9.2.0")
    runtimeOnly("org.postgresql:postgresql")
}