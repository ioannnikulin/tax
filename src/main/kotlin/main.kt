import java.io.File
import kotlin.math.roundToInt

class Subject(var budget:MutableList<Budget>) {
    val sum = budget.fold(0.0) {ac, v -> ac + v.sum}
}

open class Budget(var sum:Double) {
    constructor(_sum: Int):this(_sum.toDouble())
}

//class VirtualBudget {
//    var takesFrom: MutableList<Budget> = mutableListOf()
//    var sum = takesFrom.fold(0.0) { accum, v -> accum + v.sum}
//}

interface IncomeType {
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
    fun <T:Reduction, R: Factor> apply(subj: Subject, changes: List<T>, factors: List<R>): Pair<Budget, Budget>
}

object GermanTax: TaxChain {
    private var taxClass: Int? = null
    private var age: Int? = null
    private var east: Boolean? = null

    private fun <T:Factor> checkFactors(changes: List<T>): Boolean {
        changes.map {
            when (it) {
                is Age -> age = (it as Age).age
                is TaxClass -> {
                    taxClass = if (taxClass != null) {
                        null // contradiction
                    } else {
                        when (it) {
                            is TaxClass1 -> 1
                            is TaxClass2 -> 2
                            is TaxClass3 -> 3
                            is TaxClass4 -> 4
                            is TaxClass5 -> 5
                            else -> null
                        }
                    }
                }
                is EasternGermany -> east = true
            }
        }
        if (east == null) east = false
        return (taxClass != null && age != null)
    }

    /*private*/ fun untaxableDueToOldAge(birthYear:Int, curYear: Int, income: Double): Double {
        val _age = curYear - birthYear - 1 // -1 because it starts the year AFTER person hits 64
        println(_age)
        if (_age < 64) return 0.0
        val table:Map<Int, Pair<Double, Double>> = mapOf(
            2005 to Pair(40.0, 1900.0), 2006 to Pair(38.4, 1824.0), 2007 to Pair(36.8, 1748.0), 2008 to Pair(35.2, 1672.0), 2009 to Pair(33.6, 1596.0), 2010 to Pair(32.0, 1520.0)
            , 2011 to Pair(30.4, 1444.0), 2012 to Pair(28.8, 1368.0), 2013 to Pair(27.2, 1292.0), 2014 to Pair(25.6, 1216.0), 2015 to Pair(24.0, 1140.0)
            , 2016 to Pair(22.4, 1064.0), 2017 to Pair(20.8, 988.0), 2018 to Pair(19.2, 912.0), 2019 to Pair(17.6, 836.0), 2020 to Pair(16.0, 760.0)
            , 2021 to Pair(15.2, 722.0), 2022 to Pair(14.4, 684.0), 2023 to Pair(13.6, 646.0), 2024 to Pair(12.8, 608.0), 2025 to Pair(12.0, 570.0)
            , 2026 to Pair(11.2, 532.0), 2027 to Pair(10.4, 494.0), 2028 to Pair(9.6, 456.0), 2029 to Pair(8.8, 418.0), 2030 to Pair(8.0, 380.0)
            , 2031 to Pair(7.2, 342.0), 2032 to Pair(6.4, 304.0), 2033 to Pair(5.6, 266.0), 2034 to Pair(4.8, 228.0), 2035 to Pair(4.0, 190.0)
            , 2036 to Pair(3.2, 152.0), 2037 to Pair(2.4, 114.0), 2038 to Pair(1.6, 76.0), 2039 to Pair(0.8, 38.0))
        return table.let {
            val untax = it[birthYear + 65]
            minOf(untax?.second ?: 0.0, income * (untax?.first ?: 0.0))
        } ?: 0.0

    }

    override fun <T:Reduction, R: Factor> apply(subj: Subject, changes: List<T>, factors: List<R>): Pair<Budget, Budget> {
        if (!checkFactors(factors)) {
            println ("ERROR: some vital tax factors were not provided (age, tax class)")
            return Pair(Budget(subj.sum), Budget(0.0))
        }

        val lvl0 = subj.budget.filter {it is EmploymentIncome || it is SelfEmploymentIncome/*TODO1: more here*/}.fold(Budget(0.0)) { acc, v ->
                Budget(acc.sum + v.sum)
            }
        // first brutto income
        //TODO1: actually self-employment is a different budget, and it seems betriebsausgaben are deducted only from it, not from other incomes

        val lvl1 = changes.filter {it is Werbungskosten && it.red_amount != null}
            .fold(lvl0) { acc, change ->
                Budget(acc.sum - (let { (change as Reduction).red_amount } ?: 0.0))
            }
        //minus werbungskosten

        var totalTaxPaid = subj.sum - lvl1.sum

        //TODO: old age

        return Pair(lvl1, Budget(totalTaxPaid))
//        val _lvl2 = changes.filter {it is Werbungskosten && it.red_amount != null}
//            .fold(taxpoint) { accum, change ->
//                Budget(accum.sum - (let { (change as Reduction)!!.red_amount } ?: 0.0))
//            }

    }

    class AgricultureForestryIncome(sum: Double): Budget(sum), IncomeType {
    }

    class OwnBusinessIncome(sum: Double): Budget(sum), IncomeType {
    }

    class SelfEmploymentIncome(sum: Double): Budget(sum), IncomeType {
    }

    class EmploymentIncome(sum: Double): Budget(sum), IncomeType {
    }

    class CapitalIncome(sum: Double): Budget(sum), IncomeType {
    }

    class RentLeaseIncome(sum: Double): Budget(sum), IncomeType {
    }

    class Other(sum: Double): Budget(sum), IncomeType {
        //TODO1: add others from https://www.gesetze-im-internet.de/estg/__22.html
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
            val year = 2023//TODO2:dummy here
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
    /*val a1 = GermanTax.EmploymentIncome(10000.0)
    val a2 = GermanTax.AgricultureForestryIncome(20000.0)
    val a3 = GermanTax.SelfEmploymentIncome(30000.0)
    val mariia = Subject(mutableListOf(a1, a2, a3))
    val changes = listOf(
        GermanTax.FoodInBusinessTrip(1000.0)
        , GermanTax.FoodInBusinessTrip(2000.0)
        , GermanTax.StudyExpenses(5000.0)
    )
    val res = GermanTax.apply(mariia, changes)
    println(res.first.sum)*/
    println(GermanTax.untaxableDueToOldAge(1992, 2100, 8000.0))
}
