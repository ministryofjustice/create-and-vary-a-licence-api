const AUTH_URL = process.env.AUTH_URL ?? 'https://sign-in-dev.hmpps.service.justice.gov.uk/auth'
const CVL_DEV_URL = process.env.CVL_DEV_URL ?? 'https://create-and-vary-a-licence-api-dev.hmpps.service.justice.gov.uk'

const CLIENT_ID = process.env.CLIENT_ID
const CLIENT_SECRET = process.env.CLIENT_SECRET
const USERNAME = process.env.TEST_PRISON_USER

const credentials = Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString('base64')

export const getToken = async () => {
    const tokenResponse = await fetch(
        `${AUTH_URL}/oauth/token?grant_type=client_credentials&username=${USERNAME}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': '0',
                Authorization: `Basic ${credentials}`,
            },
        },
    )

    const {access_token: token} = await tokenResponse.json()

    return token
}

export const getLicences = async (token, prisonNumber) => {
    const url = `${CVL_DEV_URL}/public/licence-summaries/prison-number/${prisonNumber}`
    console.log(url)
    const tokenResponse = await fetch(
        url,
        {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': '0',
                Authorization: `Bearer ${token}`,
            },
        },
    )

    return await tokenResponse.json()
}

export const getLicence = async (token, id) => {
    const url = `${CVL_DEV_URL}/licence/id/${id}`
    console.log(url)
    const tokenResponse = await fetch(
        url,
        {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': '0',
                Authorization: `Bearer ${token}`,
            },
        },
    )

    return await tokenResponse.json()
}
