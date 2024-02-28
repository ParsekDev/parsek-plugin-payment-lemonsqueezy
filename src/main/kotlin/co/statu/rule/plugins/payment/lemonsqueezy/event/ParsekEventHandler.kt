package co.statu.rule.plugins.payment.lemonsqueezy.event

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.ParsekEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.plugins.payment.lemonsqueezy.LemonSqueezyConfig
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin.Companion.logger

class ParsekEventHandler : ParsekEventListener {
    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        PaymentLemonSqueezyPlugin.pluginConfigManager = PluginConfigManager(
            configManager,
            PaymentLemonSqueezyPlugin.INSTANCE,
            LemonSqueezyConfig::class.java,
            logger,
            listOf()
        )

        logger.info("Initialized plugin config")
    }
}