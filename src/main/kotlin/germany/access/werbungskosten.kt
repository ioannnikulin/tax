package tax.germany.access

import tax.LegalReduction
import tax.Reduction
import java.io.File

interface Werbungskosten: LegalReduction {
}

interface BusinessTrip: Werbungskosten {
    override val desc: String
        get() = "Suppose you are on a not regular long-term trip connected to your work. Your vital interest center is left behind, and you are forced to have a double household (so this case doesn't apply to relocation). You can reduce your taxable income by the amount of money you spend during this trip."
}

class FoodInBusinessTrip(var amount:Double, var proof: File? = null): Reduction(amount, proof), BusinessTrip {
}