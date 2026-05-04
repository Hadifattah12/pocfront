# Repository Guidelines

## Project Structure & Module Organization

This repository contains two ContactSync components:

- `app/`: Android Kotlin application using Jetpack Compose, Room, WorkManager, Retrofit, and KSP.
- `app/src/main/java/com/example/contactsyncpoc/`: app code grouped by `data`, `network`, `repository`, `ui`, and `worker`.
- `app/src/main/res/`: Android resources, themes, launcher assets, and XML configuration.
- `app/src/test/` and `app/src/androidTest/`: JVM unit tests and instrumented Android tests.
- `test1/`: Spring Boot Java backend service with Maven wrapper.
- `test1/src/main/java/com/test1/test1/`: backend controllers, DTOs, models, repositories, services, mappers, exceptions, enums, and utilities.
- `test1/src/test/`: backend tests.

Keep production code inside the relevant module. Treat root-level scratch files such as `test.kt` and `TestList.kt` as temporary unless formalized.

## Build, Test, and Development Commands

- `./gradlew :app:assembleDebug`: build the Android debug APK.
- `./gradlew :app:testDebugUnitTest`: run Android JVM unit tests.
- `./gradlew :app:connectedDebugAndroidTest`: run instrumented tests on a connected emulator or device.
- `cd test1 && ./mvnw spring-boot:run`: start the backend locally.
- `cd test1 && ./mvnw test`: run backend tests.
- `cd test1 && ./mvnw package`: compile, test, and package the backend.

Use Java 17 for both Android and backend builds.

## Coding Style & Naming Conventions

Use Kotlin conventions in `app`: 4-space indentation, `PascalCase` for classes and composables, `camelCase` for functions/properties, and packages by feature or layer. Keep Compose UI in `ui/`, persistence in `data/`, API code in `network/`, and background work in `worker/`.

Use Java conventions in `test1`: 4-space indentation, `PascalCase` classes, `camelCase` methods/fields, and role suffixes such as `Controller`, `Service`, `Repository`, `Request`, `Response`, and `Exception`.

## Testing Guidelines

Add Android unit tests under `app/src/test/java` and instrumented tests under `app/src/androidTest/java`. Name test classes after the subject, for example `ContactSyncRepositoryTest`.

Add backend tests under `test1/src/test/java`. Prefer focused service and controller tests for sync sessions, validation failures, hashing, and chunk upload edge cases. Run module-specific tests before opening a PR.

## Commit & Pull Request Guidelines

Current history is minimal and uses terse messages. Prefer clear imperative commit subjects such as `Add contact sync retry handling` or `Validate chunk indexes`.

Pull requests should include a short description, affected module (`app`, `test1`, or both), test commands run, related issues, and screenshots or recordings for visible Android UI changes.

## Security & Configuration Tips

Do not commit real database credentials, API keys, hash secrets, or machine-specific paths. Move local values from `test1/src/main/resources/application.yml` and Android endpoint configuration into ignored or environment-specific configuration before sharing or deploying.
