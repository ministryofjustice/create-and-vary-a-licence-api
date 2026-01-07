package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ManagedOffenderCrnTransformer.toProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail

class ManagedOffenderCrnTransformerTest {
  @Test
  fun `should return unallocated ProbationPractitioner when staff is null`() {
    val offender = ManagedOffenderCrn(staff = null)
    val result = offender.toProbationPractitioner()
    assertThat(result).isNotNull.extracting("allocated").isEqualTo(false)
  }

  @Test
  fun `should map staff to unallocated ProbationPractitioner when staff is unallocated`() {
    val staff = StaffDetail(code = "X123", name = Name("John", null, "Doe"), unallocated = true)
    val offender = ManagedOffenderCrn(staff = staff)

    val result = offender.toProbationPractitioner()

    assertThat(result).isNotNull.extracting("allocated").isEqualTo(false)
  }

  @Test
  fun `should map staff to allocated ProbationPractitioner when allocated`() {
    val staff = StaffDetail(code = "X123", name = Name("John", null, "Doe"), unallocated = false)
    val offender = ManagedOffenderCrn(staff = staff)

    val result = offender.toProbationPractitioner()

    assertThat(result)
      .isNotNull
      .extracting("staffCode", "name", "allocated")
      .containsExactly("X123", "John Doe", true)
  }

  @Test
  fun `should handle null name gracefully`() {
    val staff = StaffDetail(code = "X123", name = null, unallocated = false)
    val offender = ManagedOffenderCrn(staff = staff)

    val result = offender.toProbationPractitioner()

    assertThat(result)
      .isNotNull
      .extracting("staffCode", "name")
      .containsExactly("X123", null)
  }
}
