package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReviewablePostRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDateTime

class ReviewablePostReleaseTest {

  @Test
  fun `isReviewNeeded returns true when kind is created by prison, status is ACTIVE, and reviewDate is null`() {
    val reviewable = object : ReviewablePostRelease {
      override val kind = LicenceKind.HARD_STOP
      override val reviewDate: LocalDateTime? = null
      override val statusCode = ACTIVE
    }

    assertThat(reviewable.isReviewNeeded()).isTrue
  }

  @Test
  fun `isReviewNeeded returns false when kind is not created by prison`() {
    val reviewable = object : ReviewablePostRelease {
      override val kind = LicenceKind.CRD
      override val reviewDate: LocalDateTime? = null
      override val statusCode = ACTIVE
    }

    assertThat(reviewable.isReviewNeeded()).isFalse
  }

  @Test
  fun `isReviewNeeded returns false when status is not ACTIVE`() {
    val reviewable = object : ReviewablePostRelease {
      override val kind = LicenceKind.HARD_STOP
      override val reviewDate: LocalDateTime? = null
      override val statusCode = LicenceStatus.INACTIVE
    }

    assertThat(reviewable.isReviewNeeded()).isFalse
  }

  @Test
  fun `isReviewNeeded returns false when reviewDate is not null`() {
    val reviewable = object : ReviewablePostRelease {
      override val kind = LicenceKind.HARD_STOP
      override val reviewDate: LocalDateTime? = LocalDateTime.now()
      override val statusCode = ACTIVE
    }

    assertThat(reviewable.isReviewNeeded()).isFalse
  }
}
