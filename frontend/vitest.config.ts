import {defineConfig} from 'vitest/config'

export default defineConfig({
    test: {
        reporters: [
            'default',
            ['vitest-sonar-reporter', {outputFile: 'target/test-report/unit-test-result.xml'}],
        ],
        coverage: {
            provider: 'istanbul',
            reporter: ['lcovonly', 'text-summary'],
            reportsDirectory: './target/coverage-report/'
        },
        browser: {
            screenshotFailures: false
        }
    }
});
