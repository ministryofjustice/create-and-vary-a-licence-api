const PRISON_API_URL = process.env.PRISON_API_URL ?? 'https://prison-api-dev.prison.service.justice.gov.uk'

const put = async (token, path, body) => {
    const response = await fetch(`${PRISON_API_URL}${path}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
    })

    if (!response.ok) {
        throw new Error(`${path} failed with status ${response.status}: ${await response.text()}`)
    }

    return response.status === 204 ? undefined : response.json()
}

export const transferIn = async (token, offenderNo, commentText = 'Test transfer in') =>
    put(token, `/api/offenders/${offenderNo}/transfer-in`, {
        receiveTime: new Date().toISOString(),
        commentText,
    })

export const transferOut = async (token, offenderNo, toLocation, commentText = 'Test transfer out') =>
    put(token, `/api/offenders/${offenderNo}/transfer-out`, {
        toLocation,
        commentText,
        escortType: 'PECS',
        movementTime: new Date().toISOString(),
        transferReasonCode: 'NOTR',
    })

export const release = async (token, offenderNo, movementReasonCode = 'CR') =>
    put(token, `/api/offenders/${offenderNo}/release`, {movementReasonCode})
