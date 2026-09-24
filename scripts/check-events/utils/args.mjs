// Parses CLI args in the form:
//   <mode> --from-prison=MDI --to-prison=LEI --prison_number=A1234AA
// Returns { mode, fromPrison, toPrison, prisonNumber }

const FLAG_TO_KEY = {
  'from-prison': 'fromPrison',
  'to-prison': 'toPrison',
  prison_number: 'prisonNumber',
}

export const parseArgs = (argv) => {
  const [mode, ...flagArgs] = argv

  const parsed = flagArgs.reduce((acc, arg) => {
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

  return { mode, ...parsed }
}
