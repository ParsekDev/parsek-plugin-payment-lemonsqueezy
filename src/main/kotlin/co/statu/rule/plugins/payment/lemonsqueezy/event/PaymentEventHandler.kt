package co.statu.rule.plugins.payment.lemonsqueezy.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.rule.plugins.payment.PaymentSystem
import co.statu.rule.plugins.payment.event.PaymentEventListener
import co.statu.rule.plugins.payment.lemonsqueezy.LemonSqueezyPaymentIntegration
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin

@EventListener
class PaymentEventHandler(
    private val paymentLemonSqueezyPlugin: PaymentLemonSqueezyPlugin,
) : PaymentEventListener {
    private val lemonSqueezyPaymentIntegration by lazy {
        paymentLemonSqueezyPlugin.pluginBeanContext.getBean(LemonSqueezyPaymentIntegration::class.java)
    }

    override fun onPaymentSystemInit(paymentSystem: PaymentSystem) {
        paymentSystem.register(lemonSqueezyPaymentIntegration)
    }
}