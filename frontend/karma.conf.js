// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html
const process = require('process');
process.env.CHROME_BIN = require('puppeteer').executablePath();

module.exports = function (config) {
    config.set({
        basePath: '',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-spec-reporter'),
            require('karma-coverage'),
            require('karma-sonarqube-unit-reporter'),
            require('@angular-devkit/build-angular/plugins/karma')
        ],
        client: {
            clearContext: false // leave Jasmine Spec Runner output visible in browser
        },
        reporters: [
            'spec',
            'coverage',
            'sonarqubeUnit'
        ],
        specReporter: {
            showSpecTiming: true,
            suppressSkipped: false,
            suppressSummary: false,
            suppressErrorSummary: false,
            prefixes: {
                success: '     OK: ',
                failure: ' FAILED: ',
                skipped: 'SKIPPED: '
            }
        },
        coverageReporter: {
            dir: require('path').join(__dirname, './target/coverage-report/'),
            subdir: '.',
            reporters: [
                {type: 'lcovonly'},
                {type: 'text-summary'}
            ]
        },
        sonarQubeUnitReporter: {
            outputFile: 'target/test-report/unit-test-result.xml',
            overrideTestDescription: true,
            testPaths: ['./src'],
            testFilePattern: '.spec.ts',
            useBrowserName: false,
            prependTestFileName: 'frontend'
        },
        reportSlowerThan: 100, // Mark tests, which run longer than 100ms.
        port: 9876,
        colors: true,
        logLevel: config.LOG_WARN,
        autoWatch: true,
        customLaunchers: {
            ChromeHeadlessCI: {
                base: 'ChromeHeadless',
                flags: [
                    '--no-sandbox',
                    '--headless'
                ]
            }
        },
        singleRun: false,
        restartOnFileChange: true
    });
};
