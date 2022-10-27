package com.raycoarana.sleuth.otel.baggage

import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

const val MY_BAGGAGE = "my-baggage"
const val MY_BAGGAGE_VALUE = "my-baggage-value"

@SpringBootApplication
class LoginOidc(
    private val environment: Environment,
    private val webClient: WebClient.Builder
) : CommandLineRunner {
    override fun run(vararg args: String) {
        runBlocking {
            webClient.build()
                .get()
                .uri("http://localhost:${environment.getProperty("local.server.port")}/")
                .header(MY_BAGGAGE, MY_BAGGAGE_VALUE)
                .awaitExchange {
                    if (!it.statusCode().is2xxSuccessful) {
                        println("FAILED!")
                        return@awaitExchange
                    }
                    if (it.headers().asHttpHeaders().getFirst(MY_BAGGAGE) != MY_BAGGAGE_VALUE) {
                        error("no baggage header in response")
                    }
                    it.awaitBody(String::class)
                }
        }.also { println("Success! $it") }
    }
}

fun main(args: Array<String>) {
    runApplication<LoginOidc>(*args)
}
