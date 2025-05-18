pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
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
include(":ambilWarna")
include(":util")
include(":superToasts")
include(":androidFileChooser")
include(":dragSortListview")
include(":fbreader")
