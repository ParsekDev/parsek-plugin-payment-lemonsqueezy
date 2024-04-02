package co.statu.rule.plugins.payment.lemonsqueezy.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.CoreEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.plugins.payment.lemonsqueezy.LemonSqueezyConfig
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin
import org.slf4j.Logger

@EventListener
class CoreEventHandler(
    private val paymentLemonSqueezyPlugin: PaymentLemonSqueezyPlugin,
    private val logger: Logger
) : CoreEventListener {
    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        val pluginConfigManager = PluginConfigManager(
            configManager,
            paymentLemonSqueezyPlugin,
            LemonSqueezyConfig::class.java,
            listOf(),
            listOf("payment-lemonsqueezy")
        )

        paymentLemonSqueezyPlugin.pluginBeanContext.beanFactory.registerSingleton(
            pluginConfigManager.javaClass.name,
            pluginConfigManager
        )

        logger.info("Initialized plugin config")
    }
}