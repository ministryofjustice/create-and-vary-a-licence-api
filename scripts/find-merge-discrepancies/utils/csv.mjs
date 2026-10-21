import { readFileSync } from 'node:fs'

export const readLicences = (path) => {
  const text = readFileSync(path, 'utf8').trim()
  if (!text) return []

  return text.split('\n').map((line) => {
    const [id, nomsId, crn, statusCode, forename, surname] = line.split(',')
    return { id, nomsId, crn, statusCode, forename, surname }
  })
}
