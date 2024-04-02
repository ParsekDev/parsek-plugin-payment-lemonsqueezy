package co.statu.rule.plugins.payment.lemonsqueezy.route

import co.statu.parsek.Main
import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.error.BadRequest
import co.statu.parsek.error.NoPermission
import co.statu.parsek.model.*
import co.statu.parsek.util.DateUtil
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.plugins.payment.api.PaymentCallbackHandler
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import co.statu.rule.plugins.payment.lemonsqueezy.LemonSqueezyConfig
import co.statu.rule.plugins.payment.lemonsqueezy.PaymentLemonSqueezyPlugin
import com.google.gson.Gson
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import org.slf4j.Logger
import java.util.*

@Endpoint
class CheckoutCallbackAPI(
    private val paymentLemonSqueezyPlugin: PaymentLemonSqueezyPlugin,
    private val logger: Logger,
) : Api() {
    override val paths = listOf(Path("/checkout/callback/lemonsqueezy", RouteType.POST))

    private val environmentType by lazy {
        paymentLemonSqueezyPlugin.environmentType
    }

    private val pluginConfigManager by lazy {
        paymentLemonSqueezyPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<LemonSqueezyConfig>
    }

    private val paymentCallbackHandler by lazy {
        paymentLemonSqueezyPlugin.pluginBeanContext.getBean(PaymentCallbackHandler::class.java)
    }

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler? =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty(
                            "meta",
                            objectSchema()
                                .requiredProperty(
                                    "custom_data",
                                    objectSchema()
                                        .requiredProperty("userId", stringSchema())
                                        .requiredProperty("purchaseId", stringSchema())
                                )
                        )
                        .requiredProperty(
                            "data",
                            objectSchema()
                                .requiredProperty(
                                    "attributes",
                                    objectSchema()
                                        .requiredProperty(
                                            "urls",
                                            objectSchema()
                                                .optionalProperty("receipt", stringSchema())
                                                .optionalProperty("update_payment_method", stringSchema())
                                        )
                                )
                        )
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        if (environmentType != Main.Companion.EnvironmentType.RELEASE) {
            logger.info(context.body().asString())
            logger.info("PAYMENT RECEIVED")
        }

        val request = context.request()

        val xSignature = request.getHeader("x-signature")
        val xEventName = request.getHeader("x-event-name")

        val metaJsonObject = data.getJsonObject("meta")
        val dataJsonObject = data.getJsonObject("data")

        val customDataJsonObject = metaJsonObject.getJsonObject("custom_data")
        val attributesJsonObject = dataJsonObject.getJsonObject("attributes")
        val urlsJsonObject = attributesJsonObject.getJsonObject("urls")
        val receiptUrl = urlsJsonObject.getString("receipt")
//        val updatePaymentMethodUrl = urlsJsonObject.getString("update_payment_method")

        validateInput(customDataJsonObject)

        if (xSignature.isNullOrBlank() || xEventName.isNullOrBlank()) {
            throw NoPermission()
        }

        val event = LemonSqueezyEvents.entries.find { it.value == xEventName } ?: throw NoPermission()

        val body = context.body().asString()

        val lemonSqueezyConfig = pluginConfigManager.config
        val secret = lemonSqueezyConfig.secret

        if (SecurityUtil.encodeSha256HMAC(secret, body) != xSignature) {
            throw NoPermission()
        }

        val customData = CustomData.fromJson(customDataJsonObject.toString())

        paymentCallbackHandler.validatePurchase(
            customData.purchaseId,
            customData.userId,
            if (event == LemonSqueezyEvents.ORDER_CREATED)
                PurchaseStatus.PENDING
            else
                PurchaseStatus.SUCCESS
        )

        val createdAtString = attributesJsonObject.getString("created_at")
        val updatedAtString = attributesJsonObject.getString("updated_at")
        val renewsAtString = attributesJsonObject.getString("renews_at")
        val endsAtString = attributesJsonObject.getString("ends_at")
        val taxAsLong = attributesJsonObject.getLong("tax")

        val createdAt = if (createdAtString != null) DateUtil.convertISO8601ToMillis(createdAtString) else null
        val updatedAt = if (updatedAtString != null) DateUtil.convertISO8601ToMillis(updatedAtString) else null
        val renewsAt = if (renewsAtString != null) DateUtil.convertISO8601ToMillis(renewsAtString) else null
        val endsAt = if (endsAtString != null) DateUtil.convertISO8601ToMillis(endsAtString) else null

        when (event) {
            LemonSqueezyEvents.ORDER_CREATED -> {
                paymentCallbackHandler.orderCreated(
                    customData.purchaseId,
                    receiptUrl,
                    createdAt,
                    updatedAt,
                    renewsAt,
                    endsAt,
                    taxAsLong
                )
            }

            LemonSqueezyEvents.ORDER_REFUNDED -> {
                val externalOrderId = dataJsonObject.getString("id").toLong()

                paymentCallbackHandler.orderRefunded(
                    customData.purchaseId,
                    externalOrderId
                )
            }

            else -> Unit
        }

        if (environmentType != Main.Companion.EnvironmentType.RELEASE) {
            logger.info("PAYMENT SUCCESS")
        }

        return Successful()
    }

    private fun validateInput(customDataJsonObject: JsonObject) {
        val userId = customDataJsonObject.getString("userId")
        val purchaseId = customDataJsonObject.getString("purchaseId")

        try {
            UUID.fromString(userId)
            UUID.fromString(purchaseId)
        } catch (e: Exception) {
            throw BadRequest()
        }
    }

    companion object {
        private enum class LemonSqueezyEvents(val value: String) {
            SUBSCRIPTION_CREATED("subscription_created"),
            SUBSCRIPTION_CANCELLED("subscription_cancelled"),
            SUBSCRIPTION_RESUMED("subscription_resumed"),
            SUBSCRIPTION_PAYMENT_SUCCESS("subscription_payment_success"),
            ORDER_CREATED("order_created"),
            ORDER_REFUNDED("order_refunded")
        }

        private data class CustomData(val userId: UUID, val purchaseId: UUID) {
            companion object {
                private val gson by lazy {
                    Gson()
                }

                fun CustomData.toJson() = gson.toJson(this)

                fun fromJson(json: String) = gson.fromJson(json, CustomData::class.java)
            }
        }
    }
}