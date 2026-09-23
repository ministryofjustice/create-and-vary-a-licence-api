import {parseArgs} from './utils/args.mjs'
import {transfer} from "./transfer.mjs";
import {getToken} from "./utils/client.mjs";

const {mode, fromPrison, toPrison, prisonNumber} = parseArgs(process.argv.slice(2))

const token = await getToken()

switch (mode) {
    case 'transfer':
        await transfer(token, prisonNumber, fromPrison, toPrison)
        break;
    default:
        throw new Error(`Unknown mode: ${mode}`)
}
