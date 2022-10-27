package com.raycoarana.sleuth.otel.baggage

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@OptIn(DelicateCoroutinesApi::class)
@RestController
class BaggagePropagationController(
    private val tracer: Tracer
) {
    private val logger = LoggerFactory.getLogger(BaggagePropagationController::class.java)

    @GetMapping("/")
    suspend fun receiverMethod(): String {
        if (Baggage.current().getEntryValue(MY_BAGGAGE) != MY_BAGGAGE_VALUE) {
            logger.error("no baggage value received in the request")
        } else {
            logger.info("baggage value received in the request")
        }
        val requestSpanId = Span.current().spanContext.spanId

        // Launching a coroutine with the context, when the context is restored in the coroutine thread
        // Span is properly restored, but not the baggage
        val result = GlobalScope.launch(tracer.asContextElement()) {
            if (requestSpanId == Span.current().spanContext.spanId) {
                logger.info("SpanId is propagated properly in child thread")
            }

            if (Baggage.current().getEntryValue(MY_BAGGAGE) != MY_BAGGAGE_VALUE) {
                logger.error("no baggage value propagated to child thread")
            } else {
                logger.info("baggage value propagated to child thread")
            }

        }

        // But not only that, once you make a call to a coroutine that makes the thread to suspend
        // after the continuation, the restored context also loose the baggage content
        result.join()
        if (requestSpanId == Span.current().spanContext.spanId) {
            logger.info("SpanId is kept properly after continuation")
        }
        if (Baggage.current().getEntryValue(MY_BAGGAGE) != MY_BAGGAGE_VALUE) {
            logger.error("baggage is lost after continuation")
        } else {
            logger.info("baggage is kept after continuation")
        }
        return "Hello world!"
    }
}
