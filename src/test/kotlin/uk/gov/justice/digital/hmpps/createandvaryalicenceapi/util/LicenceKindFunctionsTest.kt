package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LicenceKindFunctionsTest {
  @Nested
  inner class DetermineReleaseDateKind {
    @Test
    fun `if PRRD is today and CRD is null, returns PRRD`() {
      val prrd = LocalDate.now()
      val crd = null

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.PRRD)
    }

    @Test
    fun `if PRRD is in the future and CRD is null, returns PRRD`() {
      val prrd = LocalDate.now().plusDays(10)
      val crd = null

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.PRRD)
    }

    @Test
    fun `if PRRD is in the future and after CRD, returns PRRD`() {
      val prrd = LocalDate.now().plusDays(10)
      val crd = LocalDate.now().plusDays(9)

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.PRRD)
    }

    @Test
    fun `if PRRD is null, returns CRD`() {
      val prrd = null
      val crd = null

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.CRD)
    }

    @Test
    fun `if PRRD is in the past, returns CRD`() {
      val prrd = LocalDate.now().minusDays(1)
      val crd = null

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.CRD)
    }

    @Test
    fun `if PRRD is in the future, but before CRD, returns CRD`() {
      val prrd = LocalDate.now().plusDays(9)
      val crd = LocalDate.now().plusDays(10)

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.CRD)
    }

    @Test
    fun `if PRRD is is the future and equal to CRD, returns CRD`() {
      val prrd = LocalDate.now().plusDays(10)
      val crd = LocalDate.now().plusDays(10)

      val result = determineReleaseDateKind(prrd, crd)
      assertThat(result).isEqualTo(LicenceKind.CRD)
    }
  }
}
