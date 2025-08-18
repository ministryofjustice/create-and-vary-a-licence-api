import {browser} from "k6/browser";
import {b64encode} from "k6/encoding";
import {expect} from "https://jslib.k6.io/k6-testing/0.5.0/index.js";
import http from 'k6/http';
import secrets from 'k6/secrets'
import {check, fail} from 'k6';

const AUTH_URL = __ENV.AUTH_URL;
const CVL_API_URL = __ENV.CVL_API_URL;
const CVL_URL = __ENV.CVL_URL;
const AUTH_CREDENTIALS = b64encode(`${__ENV.SYSTEM_CLIENT_ID}:${__ENV.SYSTEM_CLIENT_SECRET}`);
/*
 * Test config, see https://grafana.com/docs/k6/latest/using-k6/k6-options/
 */
export const options = {
    scenarios: {
        ui: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            options: {
                browser: {
                    type: "chromium",
                },
            },
        },
    },
};

/*
 * Set up before running the test, see https://grafana.com/docs/k6/latest/using-k6/test-lifecycle/
 *
 * Currently just checks the sign in page is available
 */
export function setup() {
    let res = http.get(CVL_URL);
    expect(res.status, `Got unexpected status code ${res.status} when trying check CVL sign in page is available. Exiting.`).toBe(200);
}

/*
 * The main test function, see https://grafana.com/docs/k6/latest/using-k6/test-lifecycle/
 */
export default async function () {
    // await comSignIn()

    const licence = getLicenceById(686)
    console.log(`licence: ${JSON.stringify(licence)}`)
}

async function signIn(username, password) {
    let checkData;
    const page = await browser.newPage();
    try {
        await page.goto(CVL_URL);

        checkData = await page.locator('h1').textContent();
        check(page, {
            header: checkData === 'Sign in',
        });

        await page.locator('input[data-element-id=username]').type(username)
        await page.locator('input[data-element-id=password]').type(password)

        await page.locator('button[data-element-id=continue-button]').click()
    } catch (error) {
        fail(`Failed to sign in: ${error.message}`);
    } finally {
        await page.close();
    }
}

async function comSignIn() {
    await signIn(await secrets.get('com_username'), await secrets.get('com_password'))
}

function getLicenceById(licenceId) {
    const token = getToken()

    const url = `${CVL_API_URL}/licence/id/${licenceId}`;
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
    };

    const res = http.get(url, params);
    if (res.status !== 200) {
        fail(`could not get licence by id: ${res.status}`);
    }
    return JSON.parse(res.body)
}

function getToken() {
    const tokenUrl = `${AUTH_URL}/auth/oauth/token?grant_type=client_credentials`;
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Basic ${AUTH_CREDENTIALS}`
        },
    };

    const res = http.post(tokenUrl, null, params);
    if (res.status !== 200) {
        fail(`could not obtain access token: ${res.status}`);
    }

    return JSON.parse(res.body).access_token
}
