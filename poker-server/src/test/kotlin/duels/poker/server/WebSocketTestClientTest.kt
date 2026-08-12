package duels.poker.server

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebSocketTestClientTest {
    @Test
    fun theTestClientCanOpenAWebSocket() = testApplication {
        application {
            module()
            routing {
                webSocket("/echo") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) send(Frame.Text(frame.readText()))
                    }
                }
            }
        }

        val testClient = createClient { install(WebSockets) }
        testClient.webSocket("/echo") {
            send(Frame.Text("ping"))
            val response = (incoming.receive() as Frame.Text).readText()
            assertEquals("ping", response)
        }
    }

    @Test
    fun twoSocketsAreIndependent() = testApplication {
        application {
            module()
            routing {
                webSocket("/echo") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) send(Frame.Text(frame.readText()))
                    }
                }
            }
        }

        val testClient = createClient { install(WebSockets) }

        testClient.webSocket("/echo") {
            send(Frame.Text("one"))
            val response1 = (incoming.receive() as Frame.Text).readText()
            assertEquals("one", response1)
        }

        testClient.webSocket("/echo") {
            send(Frame.Text("two"))
            val response2 = (incoming.receive() as Frame.Text).readText()
            assertEquals("two", response2)
        }
    }
}
