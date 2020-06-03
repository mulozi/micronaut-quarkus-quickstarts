package com.elvaliev.k8s_aws_plugin.task

import com.elvaliev.k8s_aws_plugin.PluginConstant
import com.elvaliev.k8s_aws_plugin.PluginConstant.Companion.Openshift
import com.elvaliev.k8s_aws_plugin.extension.KubernetesPluginExtension
import org.gradle.api.tasks.TaskAction

open class OpenshiftDeployTask : DeployDefaultTask() {

    @TaskAction
    fun run() {
        val extension = project.extensions.findByName(Openshift) as KubernetesPluginExtension
        println("${PluginConstant.ANSI_GREEN}Start task: ${extension.print()}${PluginConstant.ANSI_RESET}")
        checkForClient(Client.oc)
        extension.path?.let { checkFile(it) }
        executeCommand("oc create -f ${extension.path} | oc apply -f-")
        executeCommand("oc tag ${extension.image} ${extension.application}:latest")
    }
}