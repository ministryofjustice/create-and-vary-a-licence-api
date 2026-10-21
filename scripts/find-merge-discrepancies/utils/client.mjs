const AUTH_URL = process.env.AUTH_URL
const PRISONER_SEARCH_URL = process.env.PRISONER_SEARCH_URL
const CLIENT_ID = process.env.CLIENT_ID
const CLIENT_SECRET = process.env.CLIENT_SECRET

const credentials = Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString('base64')

export const getToken = async () => {
  const response = await fetch(`${AUTH_URL}/oauth/token?grant_type=client_credentials`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': '0',
      Authorization: `Basic ${credentials}`,
    },
  })

  if (!response.ok) {
    throw new Error(`Failed to get auth token: HTTP ${response.status}`)
  }

  const { access_token: token } = await response.json()
  return token
}

const chunk = (items, size) => {
  const chunks = []
  for (let i = 0; i < items.length; i += size) chunks.push(items.slice(i, i + size))
  return chunks
}

export const findExistingPrisonNumbers = async (token, prisonNumbers) => {
  const found = new Set()

  for (const batch of chunk(prisonNumbers, 1000)) {
    const response = await fetch(`${PRISONER_SEARCH_URL}/prisoner-search/prisoner-numbers`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ prisonerNumbers: batch }),
    })

    if (!response.ok) {
      throw new Error(
        `prisoner-search lookup failed with HTTP ${response.status}: ${await response.text()}. ` +
          `Check that the client has access to prisoner-search-api.`,
      )
    }

    const prisoners = await response.json()
    prisoners.forEach((prisoner) => found.add(prisoner.prisonerNumber))
  }

  return found
}
