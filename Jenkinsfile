pipeline {

    agent any

    tools {
        maven 'maven'
        jdk 'temurin-21-jdk-amd64'
    }

    options {
        skipDefaultCheckout(true)
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages{

        stage('Checkout SCM') {
            steps {
                checkout scm
                script {
                    env.GIT_BRANCH = sh (
                      script: "git branch --show-current",
                      returnStdout: true
                    ).trim()
                    env.GIT_COMMIT_MESSAGE = sh (
                      script: "git show -s --format=%s",
                      returnStdout: true
                    ).trim()
                }
                echo "Checked out branch: \"${env.GIT_BRANCH}\" on commit message \"${env.GIT_COMMIT_MESSAGE}\""
            }
        }

        stage('Test') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh '''
                       mvn -B clean verify
                    '''
                }
            }
        }

        stage ('Dependency-Check') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh 'mvn -B dependency-check:aggregate'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh 'mvn -B sonar:sonar'
                }
            }
        }

        stage('Build & Deploy Snapshot (optional)') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH ==~ '^develop(-\\d+\\.\\d+\\.\\d+)?$' }
                }
            }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        env.RELEASE_VERSION = readReleaseVersion() + '-' + env.BUILD_NUMBER
                    }
                    sh '''
                       mvn -B clean deploy \
                       -DskipTests \
                       -Dmaven.install.skip=true
                    '''
                    sh '''
                       mvn -B exec:exec@archive-and-deploy \
                       -Darchive.version=$RELEASE_VERSION \
                       -Darchive.repository=$NEXUS_RAW_APPS_SNAPSHOTS
                    '''
                }
            }
        }

        stage('Build & Deploy Release (optional)') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH ==~ '^main(-\\d+\\.\\d+\\.\\d+)?$' }
                }
            }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        env.RELEASE_VERSION = readReleaseVersion()
                    }
                    sh '''
                       mvn -B clean deploy \
                       -Drevision=$RELEASE_VERSION \
                       -DskipTests \
                       -Dmaven.install.skip=true
                    '''
                    sh '''
                        mvn -B ci-friendly-flatten:scmTag -Drevision=$RELEASE_VERSION -Dtag=$RELEASE_VERSION
                    '''
                    sh '''
                       mvn -B exec:exec@archive-and-deploy \
                       -Darchive.version=$RELEASE_VERSION \
                       -Darchive.repository=$NEXUS_RAW_APPS_RELEASES
                    '''
                }
            }
        }
    }
}

String readReleaseVersion() {

    def matcher = readFile('.ci-friendly-pom.xml') =~ '<version>(.+?)</version>'
    version = matcher ? matcher[0][1] : null

    if (version == null || version.trim().isEmpty()) {
        throw new Exception("Couldn't read version from POM.")
    }

    return version.trim().replace('-SNAPSHOT', '')
}
