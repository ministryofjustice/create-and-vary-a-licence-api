package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remand

enum class RemandCourtEvents(val code: String, val description: String) {
  BAIL_APPEAL_WITHDRAWN("2507", "Bail Appeal Withdrawn"),
  COMMITTED_TO_CROWN_COURT_FOR_SENTENCE("4001", "Committed to Crown Court for Sentence"),
  SENTENCE_POSTPONED("4004", "Sentence Postponed"),
  REMITTAL_FOR_SENTENCE_IN_CUSTODY("4012", "Remittal for Sentence in Custody"),
  COMMIT_TO_CROWN_COURT_FOR_SENTENCE_IN_CUSTODY("4016", "Commit to Crown Court for Sentence in Custody"),
  WARRANT_FOR_FURTHER_DETENTION("4505", "Warrant for Further Detention"),
  ADJOURNMENT("4506", "Adjournment"),
  REMAND_IN_CUSTODY_BAIL_REFUSED("4531", "Remand in Custody (Bail Refused)"),
  REMAND_IN_CUSTODY_ANCILLARY_RESULTS("4532", "Remand in Custody Ancillary Results"),
  REMAND_IN_CUSTODY_AFTER_BAIL_APPEAL_BY_PROSECUTOR("4534", "Remand in Custody after Bail Appeal by Prosecutor"),
  REMAND_IN_CUSTODY_TO_CUSTOMS_DETENTION("4535", "Remand in Custody to Customs Detention"),
  REMAND_IN_CUSTODY_TO_HOSPITAL_MENTAL_HEALTH_ACT("4536", "Remand in Custody to Hospital (Mental Health Act)"),
  REMAND_IN_LOCAL_AUTHORITY_ACCOMMODATION_BAIL_REFUSED("4537", "Remand in Local Authority Accommodation (Bail Refused)"),
  REMAND_IN_SECURE_LOCAL_AUTHORITY_ACCOMMODATION("4539", "Remand in Secure Local Authority Accommodation"),
  REMITTAL_TO_YOUTH_COURT_IN_CUSTODY("4549", "Remittal to Youth Court in Custody"),
  CASE_RESCHEDULED_NOT_BAILED("4553", "Case Rescheduled - Not bailed"),
  CASE_RESCHEDULED_NOT_BAILED_PERSONAL_SERVICE("4554", "Case Rescheduled - Not bailed (Personal Service)"),
  COMMIT_TRANSFER_SEND_TO_CROWN_COURT_FOR_TRIAL_IN_CUSTODY(
    "4560",
    "Commit/Transfer/Send to Crown Court for Trial in Custody",
  ),
  COMMIT_TRANSFER_SEND_TO_CROWN_COURT_FOR_TRIAL_IN_CUSTODY_WITH_DIRECTION_TO_BAIL(
    "4561",
    "Commit/Transfer/Send to Crown Court for Trial in Custody With Direction to Bail",
  ),
  COMMIT_TRANSFER_SEND_TO_CROWN_COURT_FOR_TRIAL_IN_SECURE_LOCAL_AUTHORITY_ACCOMMODATION(
    "4563",
    "Commit/Transfer/Send to Crown Court for Trial in Secure Local Authority Accommodation",
  ),
  COMMIT_TRANSFER_SEND_TO_CROWN_COURT_FOR_TRIAL_IN_CUSTODY_TRANSFER_DIRECTION(
    "4564",
    "Commit/Transfer/Send to Crown Court for Trial in Custody (Transfer Direction)",
  ),
  COMMIT_TO_CROWN_COURT_FOR_TRIAL_SUMMARY_EITHER_WAY_OFFENCES(
    "4565",
    "Commit to Crown Court for Trial (Summary / Either Way Offences)",
  ),
  MENTAL_HEALTH_COMMIT_TO_CROWN_COURT_FOR_RESTRICTION_REMAND_TO_HOSPITAL(
    "4570",
    "Mental Health Commit to Crown Court for Restriction (Remand to Hospital)",
  ),
  MENTAL_HEALTH_COMMIT_TO_CROWN_COURT_FOR_RESTRICTION_REMAND_TO_PRISON(
    "4571",
    "Mental Health Commit to Crown Court for Restriction (Remand to Prison)",
  ),
  REMITTAL_FOR_TRIAL_IN_CUSTODY("4588", "Remittal for Trial - In Custody"),
  CIVIL_REMAND("5601", "civil Remand"),
  ;

  companion object {
    val REMAND_CODES: List<String> = entries.map { it.code }
  }
}
