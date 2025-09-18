package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.LocalDate

class ReleaseDateLabelFactoryTest {

  private val workingDaysService = mock<WorkingDaysService>()
  private val factory = ReleaseDateLabelFactory(workingDaysService)

  @Test
  fun `should get correct release date label`() {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    assertThat(factory.getLabel(null, null, null, null)).isEqualTo(LABEL_FOR_CRD_RELEASE_DATE)
    assertThat(factory.getLabel(today, today, null, null)).isEqualTo(LABEL_FOR_CONFIRMED_RELEASE_DATE)
    assertThat(factory.getLabel(tomorrow, tomorrow, null, null)).isEqualTo(
      LABEL_FOR_CONFIRMED_RELEASE_DATE,
    )
    assertThat(factory.getLabel(tomorrow, null, null, tomorrow)).isEqualTo(LABEL_FOR_HDC_RELEASE_DATE)
    assertThat(factory.getLabel(tomorrow, tomorrow, null, tomorrow)).isEqualTo(LABEL_FOR_CONFIRMED_RELEASE_DATE)
    // for PRRDs, LSD is the last working day before PRRD
    whenever(workingDaysService.getLastWorkingDay(tomorrow)).thenReturn(today)
    assertThat(factory.getLabel(today, null, tomorrow, null)).isEqualTo(LABEL_FOR_PRRD_RELEASE_DATE)
    assertThat(factory.getLabel(today, today, tomorrow, null)).isEqualTo(LABEL_FOR_CONFIRMED_RELEASE_DATE)
  }
}
