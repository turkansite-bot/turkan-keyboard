# Gradle bootstrap

This project ships `gradlew` / `gradlew.bat` launchers and `gradle-wrapper.properties`
pinned to **Gradle 8.9** (required by Android Gradle Plugin 8.7.x).

* If an installed `gradle` is exactly 8.9 it is used; any other version is ignored.
* Otherwise Gradle 8.9 is downloaded and verified against the `.sha256` published
  next to the distribution.

These are launchers, not the official Gradle Wrapper JAR. For the canonical wrapper run
`gradle wrapper --gradle-version 8.9` on a machine with Gradle and commit the generated
`gradle-wrapper.jar` and scripts (this replaces the launchers).

GitHub Actions does not use these launchers: the workflow installs Gradle 8.9 explicitly.
