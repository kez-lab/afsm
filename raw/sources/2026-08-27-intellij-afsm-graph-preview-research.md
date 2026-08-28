# IntelliJ / Android Studio Afsm Graph Preview Research

Date: 2026-08-27

## Project trigger

- The user approved implementing a separate Android Studio / IntelliJ
  `Afsm Graph Preview` plugin.
- Local RFC: `.github/issues/03-ide-live-preview-and-time-travel.md`
- Public RFC: <https://github.com/kez-lab/afsm/issues/58>
- This implementation covers only the WebView-based Mermaid preview prototype.
  Time-travel tracing, replay, and bidirectional graph/source navigation remain
  outside this slice.

## Official IntelliJ Platform constraints

- IntelliJ Platform Gradle Plugin 2.x setup and requirements:
  <https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html>
  - Checked version: `2.18.1`
  - Current requirement: Gradle `9.0+`, build runtime Java `17+`
- Platform build ranges:
  <https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html>
  - IntelliJ Platform `2026.1` is branch `261` and uses Java `21` bytecode.
- Android Studio release mapping:
  <https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html>
  - The local installation reports
    `AI-261.26222.65.2613.16025427`, based on platform `261.26222.65`.
  - The local bundled runtime is JBR `25.0.2`.
- Gutter markers:
  <https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html>
- Tool windows:
  <https://plugins.jetbrains.com/docs/intellij/tool-windows.html>
- Embedded browser / diagram preview:
  <https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html>
- Plugin compatibility verification:
  <https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html>

## Mermaid distribution and security

- Package: `mermaid@11.16.1`
- Registry metadata: <https://www.npmjs.com/package/mermaid/v/11.16.1>
- License: MIT
- Registry integrity:
  `sha512-TQsq6u22fAn3rek5VOubrhKPo1g5hwC3FXUN9hiyupTckcYiGuuKGkNQrKYwGJkXUxZdojwRG46gsSCFZMDp4g==`
- Security configuration:
  <https://github.com/mermaid-js/mermaid/blob/develop/docs/config/usage.md#securitylevel>
  - The embedded renderer uses `securityLevel: "strict"`.
  - The JavaScript bundle is packaged locally; the preview does not use a CDN.

## Repository compatibility finding

The Afsm root build stays on Gradle `8.11.1`, AGP `8.10.1`, Kotlin `2.0.21`,
and the JDK 17 library verification gate. The IDE plugin therefore uses an
independent Gradle build and wrapper instead of joining the root multi-project
build or Maven Central publication bundle.
