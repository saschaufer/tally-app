pipeline {

    agent any

    tools {
        maven 'maven'
        jdk 'temurin-25-jdk-amd64'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    parameters {
        booleanParam(name: 'RELEASE', defaultValue: false, description: 'Build a release')
    }

    stages{

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
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh 'mvn -B dependency-check:aggregate'
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                }
            }
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
                    expression { env.GIT_BRANCH ==~ '^main(-\\d+\\.\\d+\\.\\d+)?$' }
                    expression { params.RELEASE == false }
                }
            }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        env.ARTIFACT_ID = readArtifactId('.ci-friendly-pom.xml').replace('-', '')
                        env.GROUP_ID = readGroupId('.ci-friendly-pom.xml')
                        env.RELEASE_VERSION = readReleaseVersion('.ci-friendly-pom.xml') + '-' + env.BUILD_NUMBER
                    }
                    sh '''
                       mvn -B deploy \
                       -DskipTests \
                       -Dmaven.install.skip=true
                    '''
                    sh '''
                       bash sbom-merge.sh \
                       -n $ARTIFACT_ID \
                       -g $GROUP_ID \
                       -v $RELEASE_VERSION \
                       -s "./tallyapp/target/sbom/sbom.json ./frontend/target/sbom/sbom.json"
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
                    expression { params.RELEASE == true }
                }
            }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        env.ARTIFACT_ID = readArtifactId('.ci-friendly-pom.xml').replace('-', '')
                        env.GROUP_ID = readGroupId('.ci-friendly-pom.xml')
                        env.RELEASE_VERSION = readReleaseVersion('.ci-friendly-pom.xml')
                    }
                    sh '''
                       mvn -B clean deploy \
                       -Drevision=$RELEASE_VERSION \
                       -DskipTests \
                       -Dmaven.install.skip=true
                    '''
                    sh '''
                       bash sbom-merge.sh \
                       -n $ARTIFACT_ID \
                       -g $GROUP_ID \
                       -v $RELEASE_VERSION \
                       -s "./tallyapp/target/sbom/sbom.json ./frontend/target/sbom/sbom.json"
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
