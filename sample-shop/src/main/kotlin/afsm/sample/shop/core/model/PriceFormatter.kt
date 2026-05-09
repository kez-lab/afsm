package afsm.sample.shop.core.model

fun Long.asPriceText(): String {
    val dollars = this / 100
    val cents = this % 100
    return "$$dollars.${cents.toString().padStart(2, '0')}"
}
