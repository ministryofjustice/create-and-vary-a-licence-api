// Parses CLI args in the form:
//   --input=/data/licences.csv
// Returns { input }

const FLAG_TO_KEY = {
  input: 'input',
}

export const parseArgs = (argv) => {
  const parsed = argv.reduce((acc, arg) => {
    const match = /^--([^=]+)=(.*)$/.exec(arg)
    if (!match) {
      throw new Error(`Unrecognised argument: "${arg}". Expected format: --flag-name=value`)
    }

    const [, flag, value] = match
    const key = FLAG_TO_KEY[flag]
    if (!key) {
      throw new Error(`Unknown flag: "--${flag}"`)
    }

    return { ...acc, [key]: value }
  }, {})

  const required = Object.values(FLAG_TO_KEY)
  const missing = required.filter((key) => !parsed[key])
  if (missing.length > 0) {
    throw new Error(`Missing required argument(s): ${missing.join(', ')}`)
  }

  return parsed
}
