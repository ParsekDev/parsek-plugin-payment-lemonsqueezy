package co.statu.rule.plugins.payment.lemonsqueezy

import co.statu.parsek.api.ParsekPlugin
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions

class PaymentLemonSqueezyPlugin : ParsekPlugin() {
    override suspend fun onCreate() {
        val webclient = WebClient.create(vertx, WebClientOptions())

        pluginBeanContext.beanFactory.registerSingleton(webclient.javaClass.name, webclient)
    }
}