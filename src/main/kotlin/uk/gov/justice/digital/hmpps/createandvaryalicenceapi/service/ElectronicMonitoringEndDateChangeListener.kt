package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

private const val LED_CHANGED_MSG = "licence end date"
private const val RELEASE_DATE_CHANGED_MSG = "release date"
private const val RELEASE_DATE_AND_LED_CHANGED_MSG = "release date and licence end date"

@Service
class ElectronicMonitoringEndDateChangeListener(
  private val notifyService: NotifyService,
  private val omuService: OmuService,
) : DateChangeListener {

  override fun isDateChangeRelevant(licence: EntityLicence, sentenceChanges: SentenceChanges): Boolean =
    licence.additionalConditions.any { it.conditionCode == CONDITION_CODE_FOR_14B } &&
      licence.hasRelevantStatus() &&
      (sentenceChanges.ledChanged || sentenceChanges.crdChanged || sentenceChanges.ardChanged)

  override fun modify(licence: EntityLicence, sentenceChanges: SentenceChanges): EntityLicence {
    val condition14b = licence.additionalConditions.find { it.conditionCode == CONDITION_CODE_FOR_14B }!!
    val electronicMonitoringEndDate = licence.getElectronicMonitoringEndDate()

    val newCondition14b = condition14b.copy(
      additionalConditionData = condition14b.dataWithUpdatedMonitoringEndDate(electronicMonitoringEndDate),
      expandedConditionText = condition14b.expandedTextWithUpdatedMonitoringEndDate(electronicMonitoringEndDate)
    )

    return licence.copyAndUpdateCondition(newCondition14b)
  }

  override fun afterFlush(licence: EntityLicence, sentenceChanges: SentenceChanges) {
    val reasonForChange = sentenceChanges.reasonForDateChange()
    log.info("End date on additional condition has changed because of $reasonForChange changes for Licence id ${licence.id}, notifying OMU")

    val omuEmail = licence.prisonCode?.let { omuService.getOmuContactEmail(it)?.email }
    notifyService.sendElectronicMonitoringEndDatesChangedEmail(
      licence.id.toString(), omuEmail, licence.forename!!, licence.surname!!, licence.nomsId!!, reasonForChange
    )
  }

  private fun EntityLicence.hasRelevantStatus() = listOf(
    IN_PROGRESS, SUBMITTED, APPROVED, VARIATION_IN_PROGRESS, VARIATION_SUBMITTED, VARIATION_APPROVED
  ).contains(this.statusCode)

  private fun SentenceChanges.reasonForDateChange(): String = when {
    ledChanged && (ardChanged || crdChanged) -> RELEASE_DATE_AND_LED_CHANGED_MSG
    ledChanged -> LED_CHANGED_MSG
    ardChanged || crdChanged -> RELEASE_DATE_CHANGED_MSG
    else -> throw IllegalStateException("No change has happened that could effect monitoring end date.")
  }

  private fun AdditionalCondition.expandedTextWithUpdatedMonitoringEndDate(
    electronicMonitoringEndDate: String
  ) = this.conditionText!!.replace("[INSERT END DATE]", electronicMonitoringEndDate)

  private fun AdditionalCondition.dataWithUpdatedMonitoringEndDate(
    electronicMonitoringEndDate: String
  ) = this.additionalConditionData.map {
    if (it.dataField == CONDITION_14B_END_DATE) {
      it.copy(dataValue = electronicMonitoringEndDate)
    } else it
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
