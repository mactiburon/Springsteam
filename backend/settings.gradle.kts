rootProject.name = "springsteam-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "shared",
    "auth-service",
    "game-service",
    "library-service",
    "audit-service",
    "api-gateway",
)