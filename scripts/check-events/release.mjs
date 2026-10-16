import {getLicences} from "./utils/client.mjs";
import {release as releaseFromPrison} from "./utils/prisonApi.mjs";
import {delay} from "./utils/sleep.mjs";

const logNonInactiveLicenceStatuses = async (token, prisonNumber, label) => {
    const licences = await getLicences(token, prisonNumber)
    const nonInactiveLicences = licences.filter(licence => licence.statusCode !== 'INACTIVE')

    console.log(`${label} for prisoner '${prisonNumber}':\n${JSON.stringify(
        nonInactiveLicences.map(({id, statusCode}) => ({id, statusCode})),
        null,
        2,
    )}`)
}

export async function release(token, prisonNumber) {

    await logNonInactiveLicenceStatuses(token, prisonNumber, 'Licence statuses before release')

    console.log(`Releasing prisoner '${prisonNumber}'`)
    await releaseFromPrison(token, prisonNumber)

    console.log(`Waiting for release to be processed`)
    await delay(15000)

    await logNonInactiveLicenceStatuses(token, prisonNumber, 'Licence statuses after release')
}
