package co.statu.rule.plugins.payment.lemonsqueezy.event

import co.statu.parsek.api.event.RouterEventListener
import co.statu.parsek.model.Route
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin
import co.statu.rule.plugins.payment.lemonsqueezy.route.CheckoutCallbackAPI

class RouterEventHandler : RouterEventListener {
    override fun onInitRouteList(routes: MutableList<Route>) {
        val pluginConfigManager = PaymentLemonSqueezyPlugin.pluginConfigManager
        val logger = PaymentLemonSqueezyPlugin.logger
        val paymentCallbackHandler = PaymentLemonSqueezyPlugin.paymentCallbackHandler
        val environmentType = PaymentLemonSqueezyPlugin.INSTANCE.context.environmentType

        routes.addAll(
            listOf(
                CheckoutCallbackAPI(logger, pluginConfigManager, paymentCallbackHandler, environmentType)
            )
        )
    }
}