import {browser} from "k6/browser";
import {b64encode} from "k6/encoding";
import {expect} from "https://jslib.k6.io/k6-testing/0.5.0/index.js";
import http from 'k6/http';
import secrets from 'k6/secrets'
import {check, fail} from 'k6';
import {test} from "k6/execution";

const AUTH_URL = __ENV.AUTH_URL;
const CVL_API_URL = __ENV.CVL_API_URL;
const CVL_URL = __ENV.CVL_URL;
const HOME_URL = CVL_URL + "/";
const AUTH_CREDENTIALS = b64encode(`${__ENV.SYSTEM_CLIENT_ID}:${__ENV.SYSTEM_CLIENT_SECRET}`);

const LICENCE_APPOINTMENT_NAME = 'SMOKE_TEST_APPOINTMENT';
const LICENCE_ADDRESS_LINE_1 = 'SMOKE_TEST_ADDRESS_LINE_1';
const LICENCE_ADDRESS_LINE_2 = 'SMOKE_TEST_ADDRESS_LINE_2';
const LICENCE_TOWN = 'SMOKE_TEST_TOWN';
const LICENCE_POSTCODE = 'SMOKE_TEST_POSTCODE';
const LICENCE_CONTACT_PHONE_NUMBER = '0123455666';
/*
 * Test config, see https://grafana.com/docs/k6/latest/usingPOSTCODE-k6/k6-options/
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
    deactivateCurrentLicences(await secrets.get('nomisId'))
    await createLicence(await secrets.get('nomisId'))
}

async function signIn(username, password) {
    let checkData;
    const page = await browser.newPage();
    try {
        await page.goto(CVL_URL);

        checkData = await page.locator('h1').textContent();
        check(page, {
            "Sign in page is available": checkData === 'Sign in',
        });

        await page.locator('input[data-element-id=username]').type(username)
        await page.locator('input[data-element-id=password]').type(password)

        const continueButton = page.locator('button[data-element-id=continue-button]')
        await continueButton.waitFor()
        await continueButton.click()
        await page.waitForURL(HOME_URL)
        return page
    } catch (error) {
        await page.screenshot({path: 'sign-in-failed.png'});
        await page.close();
        test.abort(`Failed to sign in: ${error.message}`);
    }
}

async function comSignIn() {
    return await signIn(await secrets.get('com_username'), await secrets.get('com_password'))
}

async function createLicence(nomisId) {
    let page = await comSignIn()
    const creatEditLicenceLink = page.locator('#createLicenceCard a')
    await creatEditLicenceLink.waitFor()
    await creatEditLicenceLink.click()

    await page.waitForURL(/\/licence\/create\/caseload/);
    const createLicenceLink = page.locator(`a[href="/licence/create/nomisId/${nomisId}/confirm"]`)
    await createLicenceLink.waitFor()
    await createLicenceLink.click()

    await page.waitForURL(`${CVL_URL}/licence/create/nomisId/${nomisId}/confirm`)

    await page.locator('#answer').click()
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/initial-meeting-name/);
    checkLicenceIsInProgress(nomisId)

    await page.locator('#contactName').type(LICENCE_APPOINTMENT_NAME)
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/initial-meeting-place/);
    await page.locator('#addressLine1').type(LICENCE_ADDRESS_LINE_1)
    await page.locator('#addressLine2').type(LICENCE_ADDRESS_LINE_2)
    await page.locator('#addressTown').type(LICENCE_TOWN)
    await page.locator('#addressPostcode').type(LICENCE_POSTCODE)
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/initial-meeting-contact/);
    await page.locator('#telephone').type(LICENCE_CONTACT_PHONE_NUMBER)
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/initial-meeting-time/);
    await page.locator('#appointmentTimeType-2').click()
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/additional-licence-conditions-question/);
    await page.locator('#radio-option-2').click()
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/bespoke-conditions-question/);
    await page.locator('#radio-option-2').click()
    await page.locator('button[data-qa=continue]').click()

    await page.waitForURL(/check-your-answers/);
    await page.screenshot({path: 'end.png'});

    checkLicenceCreated(nomisId)
}

function apiHeaders() {
    return {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getToken()}`
        },
    };
}

function getLicenceById(licenceId) {
    const url = `${CVL_API_URL}/licence/id/${licenceId}`;
    const res = http.get(url, apiHeaders());
    if (res.status !== 200) {
        fail(`could not get licence by id: ${res.status}`);
    }
    return JSON.parse(res.body)
}

function getLicencesForNomisId(nomsId, statuses) {
    const url = `${CVL_API_URL}/licence/match`;
    const payload = {
        "nomsId": [
            nomsId,
        ],
        "status": statuses
    }
    const res = http.post(url, JSON.stringify(payload), apiHeaders());
    if (res.status !== 200) {
        fail(`could not get current licences: ${res.status}`);
    }

    return JSON.parse(res.body)
}

function checkLicenceIsInProgress(nomisId) {
    const licences = getLicencesForNomisId(nomisId, [
        "IN_PROGRESS",
    ])
    const status = licences.map(licence => licence.licenceStatus)
    check(licences, {
        "An IN PROGRESS licence has been created": (licences) => licences.length === 1
    });
}

function checkLicenceCreated(nomisId) {
    const licences = getLicencesForNomisId(nomisId, [
        "IN_PROGRESS",
    ])
    check(licences, {
        "Licence creation complete": (licences) => licences.length === 1,
    });

    const licence = getLicenceById(licences[0].licenceId)
    check(licence, {
        "Licence has correct nomis id": (licence) => licence.nomsId === nomisId,
        "Licence has correct appointment name": (licence) => licence.appointmentPerson === LICENCE_APPOINTMENT_NAME,
        "Licence has correct appointment address": (licence) => licence.appointmentAddress === `${LICENCE_ADDRESS_LINE_1}, ${LICENCE_ADDRESS_LINE_2}, ${LICENCE_TOWN}, , ${LICENCE_POSTCODE}`,
        "Licence has correct phone number": (licence) => licence.appointmentContact === LICENCE_CONTACT_PHONE_NUMBER,
        "Licence has no additional conditions": (licence) => licence.additionalLicenceConditions.length === 0,
    });
}

function updateLicenceStatus(licenceId, status) {
    const url = `${CVL_API_URL}/licence/id/${licenceId}/status`;
    const payload = {
        status,
        username: 'SMOKE_TEST',
        fullName: 'SMOKE_TEST',
    }
    const res = http.put(url, JSON.stringify(payload), apiHeaders());
    if (res.status !== 200) {
        fail(`could not update status of licence ${licenceId} to ${status}, response code: ${res.status}`);
    }
}

function deactivateCurrentLicences(nomisId) {
    const licences = getLicencesForNomisId(nomisId, [
        "IN_PROGRESS",
        "SUBMITTED",
        "APPROVED",
        "ACTIVE",
        "VARIATION_IN_PROGRESS",
        "VARIATION_SUBMITTED",
        "VARIATION_APPROVED",
        "TIMED_OUT"
    ])
    const licenceIds = licences.map(licence => licence.licenceId)
    licenceIds.forEach(licenceId => updateLicenceStatus(licenceId, 'INACTIVE'))
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
