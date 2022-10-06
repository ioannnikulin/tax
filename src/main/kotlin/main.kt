import java.io.File
import kotlin.math.roundToInt

class Budget(var sum:Double) {
    constructor(_sum: Int):this(_sum.toDouble())
}


interface Tax {
    fun apply(taxpoint: Budget): Pair<Budget, Budget>
}

abstract class Reduction(var red_amount: Double? = null, var red_proof: File? = null): Tax {
    override fun apply(taxpoint: Budget): Pair<Budget, Budget> {
        return Pair(
            Budget(taxpoint.sum - (let { red_amount } ?: 0.0)),
            Budget(let { red_amount } ?: 0.0)
        )
    }
}

interface Factor {
}

class Age(val age: Int = 0): Factor {
}

interface LegalReduction {
    val desc: String
        get() = ""
}

interface TaxChain {
    fun <T:Reduction> apply(taxpoint: Budget, changes: List<T>): Pair<Budget, Budget>
}

object GermanTax: TaxChain {

    override fun <T:Reduction> apply(taxpoint: Budget, changes: List<T>): Pair<Budget, Budget> {
        val _lvl1 = changes.filter {it is Werbungskosten && it.red_amount != null}
            .fold(taxpoint) { accum, change ->
                Budget(accum.sum - (let { (change as Reduction).red_amount } ?: 0.0))
            }
        val lvl1 = Pair<Budget, Budget> (_lvl1, Budget(taxpoint.sum - _lvl1.sum))
        return lvl1
//        val _lvl2 = changes.filter {it is Werbungskosten && it.red_amount != null}
//            .fold(taxpoint) { accum, change ->
//                Budget(accum.sum - (let { (change as Reduction)!!.red_amount } ?: 0.0))
//            }

    }

    interface Werbungskosten: LegalReduction {
    }

    interface Sonderausgaben: LegalReduction {
    }

    class StudyExpenses(var amount:Double, var proof:File? = null): Reduction(amount, proof), Sonderausgaben {
    }

    interface BusinessTrip: Werbungskosten {
        override val desc: String
            get() = "Suppose you are on a not regular long-term trip connected to your work. That includes relocation. You can reduce your taxable income by the amount of money you spend during this trip."
    }

    class FoodInBusinessTrip(var amount:Double, var proof:File? = null): Reduction(amount, proof), BusinessTrip {
    }

    class EasternGermany: Factor {
    }

    interface TaxClass: Factor {
    }

    class TaxClass1: TaxClass {
    }

    class TaxClass2: TaxClass {
    }

    class TaxClass3: TaxClass {
    }

    class TaxClass4: TaxClass {
    }

    class TaxClass5: TaxClass {
    }

    object IncomeTax: Tax {
        override fun apply(taxpoint: Budget): Pair<Budget, Budget> {
            val year = 2023//TODO:dummy here
            val tax = when (year) {
                2023 -> {//actually current 2022
                    val taxfree = 10347.0
                    val y = ((taxpoint.sum.roundToInt() - taxfree) * 0.0001)
                    val z = ((taxpoint.sum.roundToInt() - 14926) * 0.0001)
                    val x = taxpoint.sum.roundToInt().toDouble()
                    when (taxpoint.sum)
                    {

                        in 0.0..taxfree -> 0.0
                        in taxfree..14926.0 -> (1088.67*y + 1400)*y
                        in 14927.0..58596.0 -> (206.43*z+2397)*z + 869.32
                        in 58597.0..277825.0 -> 0.42*x - 9336.45
                        in 277825.0..Double.MAX_VALUE -> 0.45*x - 17671.20
                        else -> taxpoint.sum
                    }
                }

                2022 -> {//actually an older version of 2022
                    val taxfree = 9984.0
                    val y = ((taxpoint.sum.roundToInt() - taxfree) * 0.0001)
                    val z = ((taxpoint.sum.roundToInt() - 14926) * 0.0001)
                    val x = taxpoint.sum.roundToInt().toDouble()
                    when (taxpoint.sum)
                    {
                        in 0.0..taxfree -> 0.0
                        in taxfree..14926.0 -> (1008.70*y + 1400)*y
                        in 14927.0..58596.0 -> (206.43*z+2397)*z + 938.24
                        in 58597.0..277825.0 -> 0.42*x - 9267.53
                        in 277825.0..Double.MAX_VALUE -> 0.45*x - 17602.28
                        else -> taxpoint.sum
                    }
                }

                2021 -> {
                    val taxfree = 9744.0
                    val y = ((taxpoint.sum.roundToInt() - taxfree) * 0.0001)
                    val z = ((taxpoint.sum.roundToInt() - 14753.0) * 0.0001)
                    val x = taxpoint.sum.roundToInt().toDouble()
                    when (taxpoint.sum)
                    {
                        in 0.0..taxfree -> 0.0
                        in taxfree..14753.0 -> (995.21*y + 1400)*y
                        in 14754.0..57918.0 -> (208.85*z+2397)*z + 950.96
                        in 57919.0..274612.0 -> 0.42*x - 9136.63
                        in 274613.0..Double.MAX_VALUE -> 0.45*x - 17374.99
                        else -> taxpoint.sum
                    }
                }

                else -> {
                    taxpoint.sum
                }
            }
            return Pair (Budget(taxpoint.sum - tax), Budget(tax))
        }
    }
}

fun <T:Tax> Budget.apply(tax: T): Pair<Budget, Budget> {
    return tax.apply(this)
}

fun main(args: Array<String>) {
    val mariia = Budget(70900.0)
    val changes = listOf(
        GermanTax.FoodInBusinessTrip(1000.0)
        , GermanTax.FoodInBusinessTrip(2000.0)
        , GermanTax.StudyExpenses(5000.0)
    )
    val res = GermanTax.apply(mariia, changes)
    println(res.first.sum)
}
