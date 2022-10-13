package tax.germany.access

import tax.LegalReduction
import tax.Reduction
import java.io.File

interface Sonderausgaben: LegalReduction {
}

class StudyExpenses(var amount:Double, var proof: File? = null): Reduction(amount, proof), Sonderausgaben {
}

class PostMaritalMaintenance(var amount:Double, var proof: File? = null): Reduction(amount, proof), Sonderausgaben {
     //TODO:
}