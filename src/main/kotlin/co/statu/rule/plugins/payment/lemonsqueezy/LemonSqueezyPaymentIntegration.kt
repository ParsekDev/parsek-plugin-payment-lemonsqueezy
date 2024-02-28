package co.statu.rule.plugins.payment.lemonsqueezy

import co.statu.parsek.error.InternalServerError
import co.statu.parsek.util.DateUtil
import co.statu.rule.auth.db.model.User
import co.statu.rule.plugins.payment.api.Checkout
import co.statu.rule.plugins.payment.api.CheckoutResponse
import co.statu.rule.plugins.payment.api.PaymentMethodIntegration
import co.statu.rule.plugins.payment.util.TextUtil.toCurrencyFormat
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.kotlin.coroutines.await
import java.util.*

class LemonSqueezyPaymentIntegration : PaymentMethodIntegration {
    private val webClient by lazy {
        PaymentLemonSqueezyPlugin.webClient
    }

    private val pluginConfigManager by lazy {
        PaymentLemonSqueezyPlugin.pluginConfigManager
    }

    private val lemonSqueezyConfig by lazy {
        pluginConfigManager.config
    }

    private fun prepareBody(
        user: User,
        purchaseId: UUID,
        testMode: Boolean,
        title: String,
        description: String,
        price: Long,
        checkout: Checkout
    ): JsonObject {
        val billDetail = checkout.billDetail

        val zipCode = billDetail.zipCode.ifBlank { "000000" }
        val taxNumber = billDetail.taxOrIdNum.ifBlank { "11111111111" }

        val data = JsonObject(
            mapOf(
                "data" to mapOf(
                    "type" to "checkouts",
                    "attributes" to mapOf(
                        "custom_price" to price,
                        "product_options" to mapOf(
                            "name" to title,
                            "description" to description,
                            "enabled_variants" to listOf(lemonSqueezyConfig.singlePaymentId)
                        ),
                        "checkout_data" to mapOf(
                            "name" to user.fullName,
                            "email" to user.email,
                            "tax_number" to taxNumber,
                            "billing_address" to mapOf(
                                "country" to "GE",
                                "zip" to user.fullName,
                            ),
                            "custom" to mapOf(
                                "purchaseId" to purchaseId,
                                "userId" to user.id
                            )
                        ),
                        "checkout_options" to mapOf(
                            "embed" to true,
                            "media" to false,
                            "logo" to false,
                            "desc" to false,
                            "discount" to false,
                            "subscription_preview" to false
                        ),
                        "preview" to testMode,
                        "expires_at" to DateUtil.convertMillisToISO8601(checkout.expireDate),
                    ),
                    "relationships" to mapOf(
                        "store" to mapOf(
                            "data" to mapOf(
                                "type" to "stores",
                                "id" to lemonSqueezyConfig.storeId
                            )
                        ),
                        "variant" to mapOf(
                            "data" to mapOf(
                                "type" to "variants",
                                "id" to lemonSqueezyConfig.singlePaymentId
                            )
                        )
                    )
                )
            )
        )

        data.getJsonObject("data").getJsonObject("attributes").put("custom_price", price)

        return data
    }

    override suspend fun sendCheckoutRequest(
        user: User,
        purchaseId: UUID,
        amount: Long,
        price: Long,
        title: String,
        description: String,
        checkout: Checkout
    ): CheckoutResponse {
        val testMode = checkout.isTestMode()
        val token = lemonSqueezyConfig.token

        val request = webClient
            .post(
                443,
                "api.lemonsqueezy.com",
                "/v1/checkouts"
            )
            .putHeader("Authorization", "Bearer $token")
            .ssl(true)

        val body = prepareBody(
            user,
            purchaseId,
            testMode,
            title,
            description,
            price,
            checkout
        )

        val response: HttpResponse<Buffer?>

        try {
            response = request
                .sendJson(body)
                .await()
        } catch (exception: Exception) {
            checkout.failPurchase(purchaseId, exception.message)

            throw InternalServerError()
        }

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            checkout.failPurchase(purchaseId, response.bodyAsString())

            throw InternalServerError()
        }

        val responseJsonObject = response.bodyAsJsonObject()
        val responseData = responseJsonObject.getJsonObject("data")
        val responseDataAttributes = responseData.getJsonObject("attributes")
        val externalCheckoutUrl = responseDataAttributes.getString("url")

        val externalId = responseData.getString("id")

        val claims = JsonObject()

        claims.put("externalId", externalId)
        claims.put("testMode", testMode)
        claims.put("externalCheckoutUrl", externalCheckoutUrl)
        claims.put("method", getName())

        checkout.updateClaims(purchaseId, claims)

        val formattedPrice = price.toCurrencyFormat()

        return CheckoutResponse(formattedPrice, externalCheckoutUrl)
    }
}