import assert from 'assert'
import {getLicence, getLicences} from "./utils/client.mjs";
import {transferIn, transferOut} from "./utils/prisonApi.mjs";
import {delay} from "./utils/sleep.mjs";

export async function transfer(token, prisonNumber, fromPrison, toPrison) {

    const results = await getLicences(token, prisonNumber)
    const relevantLicences = results.filter(licence => ['IN_PROGRESS', 'SUBMITTED', 'APPROVED'].includes(licence.statusCode))
    assert(relevantLicences.length > 0, `No licences found for prison number ${prisonNumber} with status IN_PROGRESS, SUBMITTED or APPROVED`)

    const licence = await getLicence(token, relevantLicences[0].id)

    assert(licence.prisonCode === fromPrison, `Licence prison code ${licence.prisonCode} does not match fromPrison ${fromPrison}`)

    {
        const {id, prisonCode, prisonDescription, prisonTelephone} = licence
        console.log(`Current location info for licence ${id}:\n${JSON.stringify({
            prisonCode,
            prisonDescription,
            prisonTelephone
        }, null, 2)}`)
    }

    console.log(`Transferring prisoner '${prisonNumber}' out from '${fromPrison}' to '${toPrison}'`)
    await transferOut(token, prisonNumber, toPrison, `Transfer from ${fromPrison} to ${toPrison}`)

    await delay(5000)

    console.log(`Transferring prisoner '${prisonNumber}' in.`)
    await transferIn(token, prisonNumber, `Transfer from ${fromPrison} to ${toPrison}`)

    console.log(`Waiting for transfer to complete`)
    await delay(15000)

    const afterTransfer = await getLicence(token, relevantLicences[0].id)
    {
        const {id, prisonCode, prisonDescription, prisonTelephone} = afterTransfer
        console.log(`Current location info for licence ${id}:\n${JSON.stringify({
            prisonCode,
            prisonDescription,
            prisonTelephone
        }, null, 2)}`)
    }
}
