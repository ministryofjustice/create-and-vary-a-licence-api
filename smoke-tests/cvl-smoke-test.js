import {browser} from "k6/browser";
import {b64encode} from "k6/encoding";
import {expect} from "https://jslib.k6.io/k6-testing/0.5.0/index.js";
import http from 'k6/http';
import {check, fail} from 'k6';

const AUTH_URL = __ENV.AUTH_URL;
const CVL_API_URL = __ENV.CVL_API_URL;
const CVL_URL = __ENV.CVL_URL;

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
    // getToken()
    await signIn()
}

const signIn = async () => {
    let checkData;
    const page = await browser.newPage();
    try {
        await page.goto(CVL_URL);

        checkData = await page.locator('h1').textContent();
        console.log(`checkData is ${checkData}`)
        check(page, {
            header: checkData === 'Sign in',
        });

        await page.locator('input[data-element-id=username]').type('username')
        await page.locator('input[data-element-id=password]').type('password')

        const user = await page.locator('input[data-element-id=username]').inputValue()
        const password = await page.locator('input[data-element-id=password]').inputValue()
        console.log(`user is ${user}`)
        console.log(`password is ${password}`)

        await page.locator('button[data-element-id=continue-button]').click()
        await page.waitForTimeout(1000);
        await page.screenshot({path: 'screenshot-test-end.png'});
    } catch (error) {
        fail(`Failed to load homepage: ${error.message}`);
    } finally {
        await page.close();
    }
}

function getToken() {
    const credentials = b64encode(`${__ENV.SYSTEM_CLIENT_ID}:${__ENV.SYSTEM_CLIENT_SECRET}`);
    const tokenUrl = `${AUTH_URL}/auth/oauth/token?grant_type=client_credentials`;
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Basic ${credentials}`
        },
    };


    const res = http.post(tokenUrl, null, params);
    if (res.status === 200) {
        console.log(`status ${res.status}`);
        console.log(`token: ${JSON.parse(res.body).access_token}`)
    }
}
