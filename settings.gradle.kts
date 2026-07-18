pluginManagement {
    fun useNexus(): Boolean {
        return File("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { java.util.Properties().apply { load(it) } }
            ?.getProperty("useNexus", "true")
            ?.toBoolean() ?: true
    }

    repositories {
        if (useNexus()) {
            maven("https://nexus.vkteam.ru/repository/maven/")
        } else {
            google {
                content {
                    includeGroupByRegex("""com\.android.*""")
                    includeGroupByRegex("""com\.google.*""")
                    includeGroupByRegex("""androidx.*""")
                    excludeGroup("ru.ok.tracer")
                }
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    fun useNexus(): Boolean {
        return File("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { java.util.Properties().apply { load(it) } }
            ?.getProperty("useNexus", "true")
            ?.toBoolean() ?: true
    }

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (useNexus()) {
            maven("https://nexus.vkteam.ru/repository/maven/")
        } else {
            google {
                content {
                    excludeGroup("ru.ok.tracer")
                }
            }
        }
        mavenCentral()
    }
}

rootProject.name = "SV APP"

include(":app")
include(":commonarchitecture")
include(":commonui")
include(":main")
include(":books")
include(":wiki")
include(":news")
include(":info")
include(":models")
include(":bookreader")
// ambilWarna, dragSortListview, superToasts и androidFileChooser перенесены в fbreader
include(":util")
include(":fbreader")
include(":managers")
include(":api")