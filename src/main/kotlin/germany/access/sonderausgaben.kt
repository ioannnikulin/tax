package tax.germany.access

import tax.LegalReduction
import tax.Reduction
import java.io.File
import java.lang.Double.max

interface Sonderausgaben: LegalReduction {
}

class SelfStudyExpenses(amount:Double, proof: File? = null): Reduction(amount, proof), Sonderausgaben {
}

class PostMaritalMaintenance(amount:Double, proof: File? = null)://Unterhalt
    Reduction(amount, proof), Sonderausgaben {
    var amount = amount
        get() = max(field, 13805.0)//TODO1: for some reason only spouse stated, need to check more
    // https://de.wikipedia.org/wiki/Sonderausgabe_(Steuerrecht)
    override val desc: String
        get() = "If you have been paying maintenance to a divorced partner after submitting joint tax declaration for some time, " +
                "your taxable income can be reduced by that maintenance. " +
                "For further details see https://de.wikipedia.org/wiki/Unterhalt_(Deutschland) or go to court"
}

open class InsuranceContribution(amount: Double, proof: File? = null):
    Reduction(amount, proof), Sonderausgaben {
        //TODO1: more options for old age pensions actually
        //also some even more complex prerequisites under (2) in estg 10, jesus christ
        //also some fuckery in 10a
    }

class OldAgePensionContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class OldAgePensionEmployerShare(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class OccupationalDisabilityInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class UnemploymentInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class HealthInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class LongTermCareInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class AccidentInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class PrivateLiabilityInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class RiskInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {
}

class LifeInsuranceContribution(amount:Double, proof: File? = null):
    InsuranceContribution(amount, proof) {//TODO1: complex terms
}

class ChurchTaxContribution(amount:Double, proof: File? = null):
    Reduction(amount, proof), Sonderausgaben {//TODO1: some exception i didnt understand
}

class ChildcareExpense(amount:Double, proof: File? = null):
    Reduction(amount, proof), Sonderausgaben {
}

class ChildSchoolPayment(amount:Double, proof: File? = null):
    Reduction(amount, proof), Sonderausgaben {
    override val desc = "Payment for school for each of your children. " +
            "Only attending lessons - not accommodation, not food, not care, just lessons price. " +
            "The school mut be within EU or be legally German - anywhere."
}

class Donations(amount:Double, proof: File? = null):
    Reduction(amount, proof), Sonderausgaben {
    var amount = amount
        get() = field//TODO1: complex limits somehow tied to total brutto and other stuff, have to go up the stairs...
}
