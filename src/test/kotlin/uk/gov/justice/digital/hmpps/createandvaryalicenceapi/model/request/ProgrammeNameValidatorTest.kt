package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ProgrammeNameValidatorTest {
  private lateinit var validator: ProgrammeNameValidator
  private lateinit var context: ConstraintValidatorContext

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    validator = ProgrammeNameValidator()
    context = Mockito.mock(ConstraintValidatorContext::class.java)
  }

  @Test
  fun `should return true when isToBeTaggedForProgramme is false`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = false,
      programmeName = null,
    )
    assertTrue(validator.isValid(request, context))
  }

  @Test
  fun `should return true when isToBeTaggedForProgramme is true and programmeName is provided`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Valid Programme",
    )
    assertTrue(validator.isValid(request, context))
  }

  @Test
  fun `should return false when isToBeTaggedForProgramme is true and programmeName is null`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = null,
    )
    assertFalse(validator.isValid(request, context))
  }

  @Test
  fun `should return false when isToBeTaggedForProgramme is true and programmeName is blank`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "   ",
    )
    assertFalse(validator.isValid(request, context))
  }

  @Test
  fun `should return true when request is null`() {
    assertTrue(validator.isValid(null, context))
  }
}
