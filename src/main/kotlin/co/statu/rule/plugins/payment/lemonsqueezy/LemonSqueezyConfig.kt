package co.statu.rule.plugins.payment.lemonsqueezy

import co.statu.parsek.api.config.PluginConfig
import co.statu.parsek.util.KeyGeneratorUtil

data class LemonSqueezyConfig(
    val token: String = "",
    val secret: String = KeyGeneratorUtil.generateSecretKey(),
    val storeId: String = "",
    val singlePaymentId: String = ""
) : PluginConfig()