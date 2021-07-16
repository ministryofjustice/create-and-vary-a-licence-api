package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TestRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TestData

@Service
class TestService(private val testRepository: TestRepository) {

  fun getTestData(): List<TestData> {
    return testRepository.findAll().map { transform(it) }
  }
}