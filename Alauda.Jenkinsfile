// https://jenkins.io/doc/book/pipeline/syntax/
@Library('alauda-cicd-debug') _

// global variables for pipeline
def GIT_BRANCH
def GIT_COMMIT
def FOLDER = "."
// image can be used for promoting...
def IMAGE
def CURRENT_VERSION
def code_data
def DEBUG = false
def RELEASE_VERSION
def TEST_IMAGE
def hpiRelease

pipeline {
	// 运行node条件
	// 为了扩容jenkins的功能一般情况会分开一些功能到不同的node上面
	// 这样每个node作用比较清晰，并可以并行处理更多的任务量
	agent { label 'golang && java' }

	// (optional) 流水线全局设置
	options {
		// 保留多少流水线记录（建议不放在jenkinsfile里面）
		buildDiscarder(logRotator(numToKeepStr: '10'))

		// 不允许并行执行
		disableConcurrentBuilds()
	}

	parameters {
				booleanParam(name: 'DEBUG', defaultValue: false, description: 'DEBUG the pipeline')
	}

	//(optional) 环境变量
	environment {
		// for building an scanning
		JENKINS_IMAGE = "jenkins/jenkins:lts"
		REPOSITORY = "configuration-as-code-plugin"
    PLUGIN_NAME = "configuration-as-code"
		OWNER = "alauda"
		IMAGE_TAG = "dev"
		// sonar feedback user
		// needs to change together with the credentialsID
		SCM_FEEDBACK_ACCOUNT = "alaudabot"
		SONARQUBE_SCM_CREDENTIALS = "alaudabot"
		DINGDING_BOT = "devops-chat-bot"
		TAG_CREDENTIALS = "alaudabot-github"
	}

	// stages
	stages {
		stage('Checkout') {
			steps {
				script {
					DEBUG = params.DEBUG
					// checkout code
					def scmVars = checkout scm
					// extract git information
					env.GIT_COMMIT = scmVars.GIT_COMMIT
					env.GIT_BRANCH = scmVars.GIT_BRANCH
					GIT_COMMIT = "${scmVars.GIT_COMMIT}"
					GIT_BRANCH = "${scmVars.GIT_BRANCH}"

					hpiRelease = deploy.HPIRelease(scmVars)
					hpiRelease.debug = DEBUG
					hpiRelease.calculate()

					RELEASE_VERSION = hpiRelease.releaseVersion

					echo "RELEASE_VERSION ${RELEASE_VERSION}"
				}
			}
		}

		stage('CI'){
			steps {
				script {
					container('java'){
            sh """
						mvn clean install -U -Dmaven.test.skip=true
            """
					}

          archiveArtifacts 'plugin/target/*.hpi'
				}
			}
		}

		stage("Code Scan"){
			steps{
				container("tools"){
					script{
					  if(env.BRANCH_NAME == "alauda"){
					    env.BRANCH_NAME = "master"
						  deploy.scan().startACPSonar(null, "-D sonar.projectVersion=${RELEASE_VERSION}")
					  } else {
					    env.BRANCH_NAME = "alauda"
					  }
					}
				}
			}
		}

		stage('Deploy to Nexus') {
			steps{
				script{
					hpiRelease.deploy("-Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true")
					if(hpiRelease.deployToUC){
						hpiRelease.triggerBackendIndexing(RELEASE_VERSION)
						hpiRelease.waitUC(PLUGIN_NAME, RELEASE_VERSION, 15)
					}
				}
			}
		}
		// after build it should start deploying
		stage('Tag Git') {
			// limit this stage to master or release only
			when {
				expression { hpiRelease.shouldTag }
			}
			steps {
				script {
					// adding tag to the current commit
					withCredentials([usernamePassword(credentialsId: TAG_CREDENTIALS, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
						sh "git tag -l | xargs git tag -d" // clean local tags
						sh """
							git config --global user.email "alaudabot@alauda.io"
							git config --global user.name "Alauda Bot"
								"""
						def repo = "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${OWNER}/${REPOSITORY}.git"
						sh "git fetch --tags ${repo}" // retrieve all tags
						sh("git tag -a ${hpiRelease.tag} -m 'auto add release tag by jenkins'")
						sh("git push ${repo} --tags")
					}
				}
			}
		}

		stage("Delivery Jenkins") {
			when {
				expression { hpiRelease.deliveryJenkins }
			}
			steps {
			script {
					hpiRelease.triggerJenkins(PLUGIN_NAME, "io.alauda.jenkins.plugins;${RELEASE_VERSION}")
				}
			}
		}
	}

	// (optional)
	// happens at the end of the pipeline
	post {
		// 成功
		success {
			echo "Horay!"
			script {
				deploy.notificationSuccess(REPOSITORY, DINGDING_BOT, "流水线完成了", RELEASE_VERSION)
			}
		}
		// 失败
		failure {
			script{
			 deploy.notificationFailed(REPOSITORY, DINGDING_BOT, "流水线失败了", RELEASE_VERSION)
			}
		}
		always { junit allowEmptyResults: true, testResults: "**/target/surefire-reports/**/*.xml" }
	}
}

