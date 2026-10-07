package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceType {
  AP {
    override fun conditionTypes() = setOf(AP.name)
  },

  // PSS has been repealed - retained for backwards compatibility with existing licences, but should not be used for new licences
  AP_PSS {
    override fun conditionTypes() = setOf(AP.name, PSS.name)
  },

  // PSS has been repealed - retained for backwards compatibility with existing licences, but should not be used for new licences
  PSS {
    override fun conditionTypes() = setOf(PSS.name)
  },
  ;

  abstract fun conditionTypes(): Set<String>
}
