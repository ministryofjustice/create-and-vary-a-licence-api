import { parseArgs } from './utils/args.mjs'
import { readLicences } from './utils/csv.mjs'
import { getToken, findExistingPrisonNumbers } from './utils/client.mjs'

const { input } = parseArgs(process.argv.filter((arg) => arg.startsWith('--')))
const envName = process.env.ENV_NAME ?? 'unknown'

const licences = readLicences(input)
console.error(`Checking ${licences.length} non-INACTIVE licences against prisoner-search (${envName})...`)

const distinctNomsIds = [...new Set(licences.map((licence) => licence.nomsId))]
const token = await getToken()
const found = await findExistingPrisonNumbers(token, distinctNomsIds)
const discrepancies = licences.filter((licence) => !found.has(licence.nomsId))

if (discrepancies.length === 0) {
  console.error('No discrepancies found: every non-INACTIVE licence has a matching prisoner record.')
} else {
  console.error(`Found ${discrepancies.length} licence(s) referencing a noms id that no longer exists.`)
}

console.log('CSV:licenceId,nomsId,crn,statusCode')
discrepancies.forEach(({ id, nomsId, crn, statusCode }) => {
  console.log(`CSV:${id},${nomsId},${crn},${statusCode}`)
})
