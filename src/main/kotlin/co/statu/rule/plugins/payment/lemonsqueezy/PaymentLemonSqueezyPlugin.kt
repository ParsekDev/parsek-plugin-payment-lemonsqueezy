package co.statu.rule.plugins.payment.lemonsqueezy

import co.statu.parsek.api.ParsekPlugin
import co.statu.parsek.api.PluginContext
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.plugins.payment.api.PaymentCallbackHandler
import co.statu.rule.plugins.payment.lemonsqueezy.event.ParsekEventHandler
import co.statu.rule.plugins.payment.lemonsqueezy.event.PaymentEventHandler
import co.statu.rule.plugins.payment.lemonsqueezy.event.RouterEventHandler
import io.vertx.ext.web.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PaymentLemonSqueezyPlugin(pluginContext: PluginContext) : ParsekPlugin(pluginContext) {
    companion object {
        internal val logger: Logger = LoggerFactory.getLogger(PaymentLemonSqueezyPlugin::class.java)

        internal lateinit var pluginConfigManager: PluginConfigManager<LemonSqueezyConfig>

        internal lateinit var INSTANCE: PaymentLemonSqueezyPlugin

        internal lateinit var webClient: WebClient

        internal lateinit var paymentCallbackHandler: PaymentCallbackHandler
    }

    init {
        INSTANCE = this

        logger.info("Initialized instance")

        context.pluginEventManager.register(this, PaymentEventHandler())
        context.pluginEventManager.register(this, ParsekEventHandler())
        context.pluginEventManager.register(this, RouterEventHandler())

        logger.info("Registered events")

        webClient = WebClient.create(context.vertx)

        logger.info("Webclient created")
    }
}