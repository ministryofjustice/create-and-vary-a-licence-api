package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.WorkLoadAllocationResponse
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderManagerMapperTest {

  private val mapper = OffenderManagerMapper()

  @Test
  fun `mapFrom staff details and staff codes returns correct OffenderManager`() {
    // Given
    val staffDetails = User(
      id = 123L,
      code = "S001",
      username = "user1 ",
      email = "user1@example.com",
      telephoneNumber = "012345",
      name = Name(
        forename = "John",
        middleName = "K",
        surname = "Smith",
      ),
      teams = listOf(
        TeamDetail(
          code = "T1",
          description = "Team One",
          district = Detail("D1", "District One"),
          borough = Detail("B1", "Borough One"),
          provider = Detail("P1", "Provider One"),
        ),
      ),
      provider = Detail(
        code = "P1",
        description = "Provider One",
      ),
      unallocated = false,
    )

    val staffCodes = WorkLoadAllocationResponse(
      id = "123",
      staffCode = "S001",
      teamCode = "T1",
      createdDate = LocalDateTime.now(),
      crn = "CRN123",
    )

    // When
    val result = mapper.mapFrom(staffDetails, staffCodes)

    // Then
    assertThat(result).isEqualTo(
      OffenderManager(
        staffIdentifier = 123L,
        code = "S001",
        username = "USER1",
        email = "user1@example.com",
        forename = "John",
        surname = "Smith",
        providerCode = "P1",
        providerDescription = "Provider One",
        teamCode = "T1",
        teamDescription = "Team One",
        boroughCode = "B1",
        boroughDescription = "Borough One",
        districtCode = "D1",
        districtDescription = "District One",
        crn = "CRN123",
      ),
    )
  }

  @Test
  fun `mapFrom staff details and staff codes returns correct team when multiple teams given`() {
    // Given
    val staffDetails = User(
      id = 123L,
      code = "S001",
      username = "user1",
      email = "user1@example.com",
      telephoneNumber = "012345",
      name = Name(
        forename = "John",
        middleName = "K",
        surname = "Smith",
      ),
      teams = listOf(
        TeamDetail(
          code = "T1",
          description = "Team One",
          district = Detail("D1", "District One"),
          borough = Detail("B1", "Borough One"),
          provider = Detail("P1", "Provider One"),
        ),
        TeamDetail(
          code = "T2",
          description = "Team Two",
          district = Detail("D2", "District Two"),
          borough = Detail("B2", "Borough Two"),
          provider = Detail("P1", "Provider One"),
        ),
        TeamDetail(
          code = "T3",
          description = "Team Three",
          district = Detail("D3", "District Three"),
          borough = Detail("B3", "Borough Three"),
          provider = Detail("P1", "Provider One"),
        ),
      ),
      provider = Detail(
        code = "P1",
        description = "Provider One",
      ),
      unallocated = false,
    )

    val staffCodes = WorkLoadAllocationResponse(
      id = "123",
      staffCode = "S001",
      teamCode = "T2",
      createdDate = LocalDateTime.now(),
      crn = "CRN123",
    )

    // When
    val result = mapper.mapFrom(staffDetails, staffCodes)

    // Then
    assertThat(result.teamCode).isEqualTo("T2")
  }

  @Test
  fun `mapFrom community manager returns correct OffenderManager`() {
    // Given
    val cm = CommunityManager(
      case = ProbationCase(crn = "CRN456"),
      id = 456L,
      code = "CM01",
      username = "commanager ",
      email = "commanager@example.com",
      telephoneNumber = "0123456789",
      name = Name(forename = "Alice", middleName = "B", surname = "Johnson"),
      provider = Detail(code = "PR1", description = "Provider One"),
      team = TeamDetail(
        code = "TM1",
        description = "Team One",
        district = Detail(code = "DT1", description = "District One"),
        borough = Detail(code = "BR1", description = "Borough One"),
        provider = Detail("PR2", "Provider Two"),
      ),
      allocationDate = LocalDate.now(),
      unallocated = false,
    )

    // When
    val result = mapper.mapFrom(cm)

    // Then
    assertThat(result).isEqualTo(
      OffenderManager(
        staffIdentifier = 456L,
        code = "CM01",
        username = "COMMANAGER",
        email = "commanager@example.com",
        forename = "Alice",
        surname = "Johnson",
        providerCode = "PR2",
        providerDescription = "Provider Two",
        teamCode = "TM1",
        teamDescription = "Team One",
        boroughCode = "BR1",
        boroughDescription = "Borough One",
        districtCode = "DT1",
        districtDescription = "District One",
        crn = "CRN456",
      ),
    )
  }
}
