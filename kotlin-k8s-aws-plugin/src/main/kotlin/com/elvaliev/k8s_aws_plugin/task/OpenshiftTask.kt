package com.elvaliev.k8s_aws_plugin.task

import com.elvaliev.k8s_aws_plugin.PluginConstant.Companion.Openshift
import com.elvaliev.k8s_aws_plugin.extension.KubernetesPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class OpenshiftTask : DeployDefaultTask() {

    @Input
    @Optional
    @Option(option = "path", description = "Custom template file, as default used openshift.yaml")
    var templatePath: String = "openshift.yaml"

    @Input
    @Optional
    @Option(option = "image", description = "Docker registry reference: <user_name>/<image_name>:<tag>")
    var dockerImage: String? = null

    @TaskAction
    fun run() {
        val extension = project.extensions.findByName(Openshift) as? KubernetesPluginExtension
        val template = parseValue(extension?.path, templatePath, "template")
        val image = parseValue(extension?.image, dockerImage, "image")
        checkForClient(Client.oc)
        template?.let {
            val kubernetesTemplate = getKubernetesTemplate(template)
            val app = parseValue(kubernetesTemplate.application, project.name, "application")
            val imageStream = kubernetesTemplate.imageStreamApplication
            when (checkDeployments("oc get  deploymentConfig $app")) {
                true -> buildDeployment(app, imageStream, image)
                false -> createDeployment(template, app, imageStream, image)
            }
        }
    }

    private fun createDeployment(
        template: String?,
        app: String?,
        imageStream: String?,
        image: String?
    ) {
        executeCommand("oc create -f $template", continueOnError = true)
        buildDeployment(app, imageStream, image)
        executeCommand("oc expose svc/$app")
        executeCommand("oc get route $app -o jsonpath --template={.spec.host}")
    }

    private fun buildDeployment(app: String?, imageStream: String?, image: String?) {
        if (checkBinaryBuild("oc get buildConfig $app -o jsonpath --template={.spec.source.type}"))
            executeCommand("oc start-build $app --from-dir build\\libs\\${project.name}-${project.version}.jar --follow")
        imageStream?.let {
            executeCommand("oc tag $image $imageStream")
        }
    }
}