pluginManagement {
    repositories {
//        google {
//            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")
//                excludeGroup("ru.ok.tracer")
//            }
//        }
        repositories.maven("https://nexus.vkteam.ru/repository/maven/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        google {
//            content {
//                excludeGroup("ru.ok.tracer")
//            }
//        }
        repositories.maven("https://nexus.vkteam.ru/repository/maven/")

//        mavenCentral()
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
// ambilWarna и dragSortListview перенесены в fbreader
include(":util")
include(":superToasts")
include(":androidFileChooser")
include(":fbreader")
include(":managers")
include(":api")
