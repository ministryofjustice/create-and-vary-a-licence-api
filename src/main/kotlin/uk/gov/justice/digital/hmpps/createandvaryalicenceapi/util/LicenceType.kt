package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceType {
  AP {
    override fun conditionTypes() = setOf(AP.name)
  },

  // PSS has been repealed - retained only to correctly render existing historical AP_PSS/PSS licences
  AP_PSS {
    override fun conditionTypes() = setOf(AP.name, PSS.name)
  },

  // PSS has been repealed - retained only to correctly render existing historical AP_PSS/PSS licences
  PSS {
    override fun conditionTypes() = setOf(PSS.name)
  },
  ;

  abstract fun conditionTypes(): Set<String>
}
