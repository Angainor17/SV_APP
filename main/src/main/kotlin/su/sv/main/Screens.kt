package su.sv.main

sealed class Screens(val route: String) {
    object News : Screens("news_route")
    object Books : Screens("books_route")
    object Wiki : Screens("wiki_route")
    object Info : Screens("info_route")
}
