pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    parameters {
        string(name: 'LzName', defaultValue: 'bakery', description: 'The name of the Landing Zones target')
        choice( name: 'StackName', choices: ['subscription', 'network', 'keyvault'], description: 'Stack to run')
        booleanParam(name: 'TfInit', defaultValue: true, description: 'Terraform Init')
        booleanParam(name: 'TfPlan', defaultValue: true, description: 'Terraform Plan')
        booleanParam(name: 'TfApply', defaultValue: false, description: 'Terraform Apply')
        booleanParam(name: 'TfPlanDestroy', defaultValue: false, description: 'Terraform Plan Destroy')
        booleanParam(name: 'TfDestroy', defaultValue: false, description: 'Terraform Destroy')
        booleanParam(name: 'Storage Uses AzureAD', defaultValue: true, description: 'Storage uses Microsoft Entra for ID for Auth')
    }

    environment {
        ARM_SVP = 'SpokeClientSecret'
        ARM_TENANT_ID = credentials('SpokeTenantId')
        ARM_SUBSCRIPTION_ID = credentials('SpokeSubId')
        ARM_USE_AZUREAD = "${params['Storage Uses AzureAD']}"
        ARM_DEPLOY_LOCATION = 'uksouth'
        BACKEND_STORAGE_SUBSCRIPTION_ID = credentials('SpokeSubId')
        BACKEND_STORAGE_ACCOUNT_NAME = credentials('SpokeSaName')
        ARM_ENVIRONMENT = 'public'
        TF_VAR_lz_name = "${params['LzName']}"
    }

    stages {
        stage('Init') {
            when {
                expression { TfInit }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        withCredentials([usernamePassword(credentialsId: ARM_SVP, usernameVariable: 'ARM_CLIENT_ID', passwordVariable: 'ARM_CLIENT_SECRET')]) {
                            pwsh """
                            pwsh -File Run-Terraform.ps1 `
                            -RunTerraformInit true `
                            -RunTerraformPlan false `
                            -RunTerraformPlanDestroy false `
                            -RunTerraformApply false `
                            -RunTerraformDestroy false `
                            -BackendStorageSubscriptionId ${env.BACKEND_STORAGE_SUBSCRIPTION_ID} `
                            -BackendStorageAccountName ${env.BACKEND_STORAGE_ACCOUNT_NAME} `
                            -LzName $params.LzName `
                            -StackName $params.StackName 
                            """
                        }
                    }
                }
            }
        }

        stage('Plan') {
            when {
                expression { TfPlan && TfInit }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        withCredentials([usernamePassword(credentialsId: ARM_SVP, usernameVariable: 'ARM_CLIENT_ID', passwordVariable: 'ARM_CLIENT_SECRET')]) {
                            pwsh """
                            pwsh -File Run-Terraform.ps1 `
                            -RunTerraformInit true `
                            -RunTerraformPlan true `
                            -RunTerraformPlanDestroy false `
                            -RunTerraformApply false `
                            -RunTerraformDestroy false `
                            -BackendStorageSubscriptionId ${env.BACKEND_STORAGE_SUBSCRIPTION_ID} `
                            -BackendStorageAccountName ${env.BACKEND_STORAGE_ACCOUNT_NAME} `
                            -LzName $params.LzName `
                            -StackName $params.StackName
                            """
                        }
                    }
                }
            }
        }

        stage('Plan Destroy') {
            when {
                expression { TfPlanDestroy && TfInit }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        withCredentials([usernamePassword(credentialsId: ARM_SVP, usernameVariable: 'ARM_CLIENT_ID', passwordVariable: 'ARM_CLIENT_SECRET')]) {
                            pwsh """
                            pwsh -File Run-Terraform.ps1 `
                            -RunTerraformInit true `
                            -RunTerraformPlan false `
                            -RunTerraformPlanDestroy true `
                            -RunTerraformApply false `
                            -RunTerraformDestroy false `
                            -BackendStorageSubscriptionId ${env.BACKEND_STORAGE_SUBSCRIPTION_ID} `
                            -BackendStorageAccountName ${env.BACKEND_STORAGE_ACCOUNT_NAME} `
                            -LzName $params.LzName `
                            -StackName $params.StackName
                            """
                        }
                    }
                }
            }
        }

        stage('Apply') {
            when {
                expression { TfApply && TfPlan && TfInit }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        timeout(time: 0.5, unit: 'DAYS') {
                            input id: 'ApproveDeployment', message: 'Are you happy to proceed with the deployment?', ok: 'Approve'
                        }

                        withCredentials([usernamePassword(credentialsId: ARM_SVP, usernameVariable: 'ARM_CLIENT_ID', passwordVariable: 'ARM_CLIENT_SECRET')]) {
                            pwsh """
                            pwsh -File Run-Terraform.ps1 `
                            -RunTerraformInit true `
                            -RunTerraformPlan true `
                            -RunTerraformPlanDestroy false `
                            -RunTerraformApply true `
                            -RunTerraformDestroy false `
                            -BackendStorageSubscriptionId ${env.BACKEND_STORAGE_SUBSCRIPTION_ID} `
                            -BackendStorageAccountName ${env.BACKEND_STORAGE_ACCOUNT_NAME} `
                            -LzName $params.LzName `
                            -StackName $params.StackName
                            """
                        }
                    }
                }
            }
        }

        stage('Apply Destroy') {
            when {
                expression { TfDestroy && TfPlanDestroy && TfInit }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        timeout(time: 0.5, unit: 'DAYS') {
                            input id: 'ApproveDeployment', message: 'Are you happy to proceed with the deployment?', ok: 'Approve'
                        }

                        withCredentials([usernamePassword(credentialsId: ARM_SVP, usernameVariable: 'ARM_CLIENT_ID', passwordVariable: 'ARM_CLIENT_SECRET')]) {
                            pwsh """
                            pwsh -File Run-Terraform.ps1 `
                            -RunTerraformInit true `
                            -RunTerraformPlan false `
                            -RunTerraformPlanDestroy true `
                            -RunTerraformApply false `
                            -RunTerraformDestroy true `
                            -BackendStorageSubscriptionId ${env.BACKEND_STORAGE_SUBSCRIPTION_ID} `
                            -BackendStorageAccountName ${env.BACKEND_STORAGE_ACCOUNT_NAME} `
                            -LzName $params.LzName `
                            -StackName $params.StackName
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            deleteDir()
        }
    }
}
