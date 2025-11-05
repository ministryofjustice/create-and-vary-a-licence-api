package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceType {
  AP {
    override fun conditionTypes() = setOf(AP.name)
  },
  AP_PSS {
    override fun conditionTypes() = setOf(AP.name, PSS.name)
  },
  PSS {
    override fun conditionTypes() = setOf(PSS.name)
  },
  ;

  abstract fun conditionTypes(): Set<String>
}
