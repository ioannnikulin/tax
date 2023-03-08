package tax.germany

import tax.*
import tax.germany.access.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

//TODO1: IMPORTANT: joint assessment is not supported yet (classes 3/5, landforstwirtschaft etc.)
//probably will have to send two Subjects into a TaxChain

object GermanTax: TaxChain {
    private var taxClass: Int? = null
    private var birthYear: Int? = null
    private var taxYear: Int? = null
    private var east: Boolean = false
    private var singleParentWith: Int = 0
    private var dontSendTax: Boolean = false

    private fun <T:Factor> checkFactors(factors: List<T>): Boolean {
        factors.map {
            when (it) {
                is BirthYear -> birthYear = if (birthYear == null) (it as BirthYear).birthYear else {
                    println("ERROR: multiple birth year assignments for one person")
                    null
                }
                is TaxYear -> taxYear = if (taxYear == null) (it as TaxYear).taxYear else {
                    println("ERROR: multiple tax year assignments for one calculation")
                    null
                }
                is SingleParentWith -> singleParentWith = if (singleParentWith == 0) ((it as SingleParentWith).childrenQtty) else {
                    println("ERROR: multiple single parent assignments for one person, assuming no children")
                    0
                }
                is DontSendTax -> dontSendTax = true
                is TaxClass -> {
                    taxClass = if (taxClass != null) {
                        println("ERROR: multiple tax class assignments for one person")
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
        return (taxClass != null && birthYear != null && taxYear != null)
    }

    private fun nontaxableDueToOldAge(income: Double): Double { // Altersentlastungsbetrag, automatically
        val age = let { taxYear!! - birthYear!! - 1 } ?: 0 // -1 because it starts the year AFTER person hits 64
        if (age < 64) return 0.0
        val table:Map<Int, Pair<Double, Double>> = mapOf(
            2005 to Pair(40.0, 1900.0), 2006 to Pair(38.4, 1824.0), 2007 to Pair(36.8, 1748.0), 2008 to Pair(35.2, 1672.0), 2009 to Pair(33.6, 1596.0)
            , 2010 to Pair(32.0, 1520.0), 2011 to Pair(30.4, 1444.0), 2012 to Pair(28.8, 1368.0), 2013 to Pair(27.2, 1292.0), 2014 to Pair(25.6, 1216.0)
            , 2015 to Pair(24.0, 1140.0), 2016 to Pair(22.4, 1064.0), 2017 to Pair(20.8, 988.0), 2018 to Pair(19.2, 912.0), 2019 to Pair(17.6, 836.0)
            , 2020 to Pair(16.0, 760.0), 2021 to Pair(15.2, 722.0), 2022 to Pair(14.4, 684.0), 2023 to Pair(13.6, 646.0), 2024 to Pair(12.8, 608.0)
            , 2025 to Pair(12.0, 570.0), 2026 to Pair(11.2, 532.0), 2027 to Pair(10.4, 494.0), 2028 to Pair(9.6, 456.0), 2029 to Pair(8.8, 418.0)
            , 2030 to Pair(8.0, 380.0), 2031 to Pair(7.2, 342.0), 2032 to Pair(6.4, 304.0), 2033 to Pair(5.6, 266.0), 2034 to Pair(4.8, 228.0)
            , 2035 to Pair(4.0, 190.0), 2036 to Pair(3.2, 152.0), 2037 to Pair(2.4, 114.0), 2038 to Pair(1.6, 76.0), 2039 to Pair(0.8, 38.0))
        return table.let {
            val untax = it[birthYear!! + 65]//checked above
            minOf(untax?.second ?: 0.0, income * (untax?.first ?: 0.0))
        } ?: 0.0

    }

    override fun <T:Reduction, R:Factor> apply(subj: Subject, changes: List<T>, factors: List<R>): Pair<Budget, Budget> {
        if (!checkFactors(factors)) {
            println ("ERROR: some vital tax factors were not provided (age, tax class, taxation year)")
            return Pair(Budget(subj.sum), Budget(0.0))
        }

        val lvl0 = subj.budget.filter {it is EmploymentIncome || it is SelfEmploymentIncome || it is AgricultureForestryIncome || it is OwnBusinessIncome || it is CapitalIncome || it is RentLeaseIncome || it is OtherIncome}
            .fold(Budget(0.0)) { acc, v ->
                Budget(acc.sum + v.sum)
            }

        //TODO1: actually self-employment is a different budget, and it seems betriebsausgaben are deducted only from it, not from other incomes. maybe applies to all types of income.

        val lvl1 = if (dontSendTax) Budget(lvl0.sum - 1200.0) else {
            changes.filter {it is Werbungskosten && it.red_amount != null}
                .fold(lvl0) { acc, change ->
                    Budget(acc.sum - (let { (change as Reduction).red_amount } ?: 0.0))
                }
        }

        lvl1.name = "Summe der Einkünfte"

        val lvl2 = Budget(lvl1.sum)
        lvl2.sum -= nontaxableDueToOldAge(lvl1.sum)
        lvl2.sum -= reliefForSingleParents()
        val agriForest = subj.budget.filter {it is AgricultureForestryIncome}
            .fold(Budget(0.0)) { acc, v ->
                Budget(acc.sum + v.sum)
            }
        lvl2.sum -= agriForestAllowance(lvl1.sum, agriForest.sum)
        lvl2.name = "Gesamtbetrag der Einkünfte"

        //TODO1: loss compensation (Verlustabzug) here. no idea how to implement yet. can move losses in one type of income to other types of income, or even to +- 1 year to soften taxation.

        //TODO2: looks like this applies only for first training (first job), so should check previous years for earlier trainings
        //TODO2: invent some clever way to distribute study expenses between werbungskosten and sonderausgaben
        val totalSelfStudyExpenses = changes.filter {it is SelfStudyExpenses && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        val totalChildSchoolPayment = changes.filter {it is ChildSchoolPayment && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        //TODO1: one child for now, but actually should count 4000 for each
        val totalChildcareExpense = changes.filter {it is ChildcareExpense && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        val totalBasicInsuranceExpenses = changes.filter {it is OldAgePensionContribution && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        val totalEmployerPensionContributions = changes.filter {it is OldAgePensionEmployerShare && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        val pensionDeductibleShareCoef = 0.94//TODO1: depends on year
        val pensionDeductibleBruttoMax = 25639.0//TODO1: depends on year
        val totalBasicInsuranceDeductible =
            max(totalBasicInsuranceExpenses.sum + totalEmployerPensionContributions.sum, pensionDeductibleBruttoMax) * pensionDeductibleShareCoef - totalEmployerPensionContributions.sum

        val totalOtherInsuranceExpenses = changes.filter {it is InsuranceContribution && it !is OldAgePensionContribution && it.red_amount != null}
            .fold(Budget(0.0)) { acc, change ->
                Budget(acc.sum + (let { (change as Reduction).red_amount } ?: 0.0))
            }

        var lvl3 = if (dontSendTax) Budget(lvl2.sum - 36.0) else {
            changes.filter {it is Sonderausgaben && it.red_amount != null
                    && it !is SelfStudyExpenses
                    && it !is ChildSchoolPayment
                    && it !is InsuranceContribution
                }.fold(lvl2) { acc, change ->
                    Budget(acc.sum - (let { (change as Reduction).red_amount } ?: 0.0))
                }
            }
        lvl3.sum = lvl3.sum - min(totalSelfStudyExpenses.sum, 6000.0)
                            - min(totalChildSchoolPayment.sum / 3.0, 5000.0)
                            - min(totalChildcareExpense.sum / 3.0 * 2.0, 4000.0)
                            - min(totalBasicInsuranceExpenses.sum * 2.0, 4000.0)
                            - totalBasicInsuranceDeductible
                            - min(totalOtherInsuranceExpenses.sum, 1900.0)/*TODO1: actually can be 2800
                            if the contributions to Sick and Long-term care insurance were borne entirely
                            without tax-free grants throughout the calendar year, also something very complex here*/
        //TODO: extraodinary burden, smth about home ownership
        // + foreign income
        // = einkommen
        // children reduction
        // hardness compensation
        // = taxable income: zu versteundes einkommen
        // ...
        //
        return Pair(lvl3, Budget(0.0))
    }

    private fun agriForestAllowance(base: Double, agri: Double): Double { // Einkünfte_aus_Land-_und_Forstwirtschaft_(Deutschland) freibeitrag
        return if (base <= 30700) min(agri, 900.0) else 0.0 // TODO1: actually for this income the tax year is different, not jan-dec, but jul-jun. could cause errors. gotta dig that.
    }

    private fun reliefForSingleParents(): Double { //Alleinerziehendenentlastungsbetrag. automatically is taken into account with tax class 2 (in tax office, not in this program yet), so TODO1
        if (singleParentWith <= 0) return 0.0//TODO1: maybe not, there was something about gradual decrease
        return when (taxYear) {
            in 1990..2001 -> 2916.0
            in 2002..2003 -> 2340.0
            in 2004..2014 -> 1308.0
            in 2015..2019 -> 1908.0 + 240 * (singleParentWith - 1)
            in 2020..Int.MAX_VALUE -> 4008.0 + 240.0 * (singleParentWith - 1)
            else -> 0.0
        }
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

    class OtherIncome(sum: Double): Budget(sum), IncomeType {
        //TODO1: add others from https://www.gesetze-im-internet.de/estg/__22.html
    }

    private object IncomeTax: Tax {
        override fun apply(taxpoint: Budget): Pair<Budget, Budget> {
            val tax = when (taxYear) {
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