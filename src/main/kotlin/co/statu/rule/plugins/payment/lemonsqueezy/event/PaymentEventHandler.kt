package co.statu.rule.plugins.payment.lemonsqueezy.event

import co.statu.rule.plugins.payment.PaymentSystem
import co.statu.rule.plugins.payment.event.PaymentEventListener
import co.statu.rule.plugins.payment.lemonsqueezy.LemonSqueezyPaymentIntegration
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin

class PaymentEventHandler : PaymentEventListener {
    override fun onPaymentSystemInit(paymentSystem: PaymentSystem) {
        paymentSystem.register(LemonSqueezyPaymentIntegration())

        PaymentLemonSqueezyPlugin.paymentCallbackHandler = paymentSystem.paymentCallbackHandler
    }
}