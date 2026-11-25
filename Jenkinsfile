pipeline {
    agent { label 'jdk8-maven' }
    options { timestamps(); buildDiscarder(logRotator(numToKeepStr: '30')) }
    stages {
        stage('Build') {
            steps { sh 'mvn -B clean install' }
        }
        stage('Coverage') {
            steps { jacoco execPattern: '**/target/jacoco.exec' }
        }
        stage('Dependency check') {
            steps { sh 'mvn -B org.owasp:dependency-check-maven:5.3.2:aggregate -DfailBuildOnCVSS=11' }
        }
        stage('Publish') {
            when { branch 'main' }
            steps { sh 'mvn -B deploy -DskipTests -DaltDeploymentRepository=nexus::default::https://nexus.corp.bofa.com/repository/maven-snapshots/' }
        }
    }
    post {
        always { junit '**/target/surefire-reports/*.xml' }
    }
}
