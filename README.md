# SRClient 1.6fix2 — recovered Fabric project

This IntelliJ/Gradle project was reconstructed from `srclient-1.6fix2.jar`.
The original archive contains compiled classes but no Java source, so the files
under `src/main/java` were recovered with Vineflower 1.12.0 and then repaired
where bytecode type erasure prevented direct recompilation.

## Target

- Minecraft: **26.1.2**
- Java: **25**
- Fabric Loader: **0.19.2**
- Fabric Loom: **1.16.1**
- Fabric API: **0.155.2+26.1.2**
- Official, non-obfuscated Minecraft names (no mappings artifact is needed)

These values match the original archive metadata, except that Fabric API did
not record its exact version in the JAR; the latest published 26.1.2-compatible
release is used. Minecraft 26.1+ is unobfuscated, so this build intentionally
uses Loom's `net.fabricmc.fabric-loom` no-remap plugin and has no `mappings`
dependency.

## Open in IntelliJ IDEA

1. Open this directory (the one containing `settings.gradle`).
2. Choose **Import as Gradle project** if IntelliJ asks.
3. Set the Gradle JVM/project SDK to Java 25. A Java 25 installation is required.
4. Wait for the Gradle sync to finish.

Run `gradlew.bat runClient` to launch a development client, or
`gradlew.bat build` to build the mod. Built JARs are written to `build/libs`.

## Recovery notes

- `libs/bundled-dependencies.jar` contains third-party bytecode separated from
  the recovered project source. The build shades it back into the output JAR,
  matching the supplied fat JAR's layout.
- Decompilation cannot recover comments, original local-variable names, or exact
  formatting. The recovered project has been verified with `gradlew.bat build`
  on Java 25.
- Keep the supplied original JAR as a reference while validating behavior.
