package tax.germany.access

import tax.Factor

class EasternGermany: Factor {
}

class SingleParentWith (val childrenQtty:Int): Factor {//TODO1: conflicts with tax class 2, difficulties with joint assessment
}

interface DontSendTax: Factor {
    companion object {}
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