import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

apply(plugin = "signing")

val projectUrl = "https://github.com/kez-lab/afsm"
val stagingRepository = rootProject.layout.buildDirectory.dir("central-staging-repository")

extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.convention("Afsm")
            description.convention("Afsm Android/Kotlin finite state machine tooling.")
            url.set(projectUrl)

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("kez-lab")
                    name.set("Kez")
                    url.set("https://github.com/kez-lab")
                }
            }

            scm {
                connection.set("scm:git:https://github.com/kez-lab/afsm.git")
                developerConnection.set("scm:git:ssh://git@github.com/kez-lab/afsm.git")
                url.set(projectUrl)
                tag.set("v${project.version}")
            }
        }
    }

    repositories {
        maven {
            name = "centralStaging"
            url = stagingRepository.get().asFile.toURI()
        }
    }
}

val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull

if (!signingKey.isNullOrBlank()) {
    extensions.configure<SigningExtension> {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
