package analizator

data class Orf(
    val frame: Int,
    val start: Int,
    val end: Int,
    val length: Int,
    val sequence: String
)
