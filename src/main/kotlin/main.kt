package tax
import java.io.File
import java.util.*
import tax.germany.GermanTax

val CURRENT_YEAR: Int = Calendar.getInstance().get(Calendar.YEAR)

class Subject(var budget:MutableList<Budget>) {
    val sum = budget.fold(0.0) {ac, v -> ac + v.sum}
}

open class Budget(var sum:Double, var name:String = "") {
    constructor(_sum: Int, name:String = ""):this(_sum.toDouble(), name)
}

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

interface Factor {//TODO2: for each factor description and possible switches (caution: not "become a single parent", rather "if you are a single parent and for some reason forgot to state it, please do")
}

class BirthYear(val birthYear: Int): Factor {
}

class TaxYear(val taxYear: Int = CURRENT_YEAR) : Factor {
}

interface LegalReduction {
    val desc: String
        get() = ""
}

interface TaxChain {
    fun <T:Reduction, R: Factor> apply(subj: Subject, changes: List<T>, factors: List<R>): Pair<Budget, Budget>
}

fun <T:Tax> Budget.apply(tax: T): Pair<Budget, Budget> {
    return tax.apply(this)
}

fun main(args: Array<String>) {
    val a1 = GermanTax.EmploymentIncome(10000.0)
    val a2 = GermanTax.AgricultureForestryIncome(20000.0)
    val a3 = GermanTax.SelfEmploymentIncome(30000.0)
    val mariia = Subject(mutableListOf(a1, a2, a3))
    val changes = listOf(
        tax.germany.access.FoodInBusinessTrip(1000.0)
        , tax.germany.access.FoodInBusinessTrip(2000.0)
        , tax.germany.access.StudyExpenses(5000.0)
        , tax.germany.access.PostMaritalMaintenance(15000.0)
    )
    val factors:MutableList<Factor> = mutableListOf(
        tax.germany.access.EasternGermany()
        , tax.germany.access.TaxClass1()
        //, tax.germany.access.TaxClass2()
        , BirthYear(1992)
        , TaxYear(2022)
        , tax.germany.access.SingleParentWith(2)
    )
    val res = GermanTax.apply(mariia, changes, factors)
    println(res.first.sum)
}
