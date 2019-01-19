package eu.luminis.blog

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.beust.klaxon.Klaxon
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SlackResponseMessage(val text: String)

data class LuminisEvent(val topic: String, val type: String, val presenter: String, val date: String)

const val filename = "next-session.json"

class SessionAnnouncer : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    val s3client = AmazonS3ClientBuilder.defaultClient()
    val restClient = RestClient()

    override fun handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val today = LocalDate.now()
        getEvent(today)?.let { e ->
            val message = "Today we have a ${e.type} session about '${e.topic}' presented by ${e.presenter}."
            println("Sending the event to Slack: $message")
            val slackJsonMessage = Klaxon().toJsonString(SlackResponseMessage(message))
            // POST the announcement as a JSON payload to the Slack webhook URL.
            restClient.post(Config.slackWebhook, slackJsonMessage)
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                headers = mapOf("Content-type" to "text/plain")
                body = "Message sent to Slack: $message"
            }
        }

        val notFoundMessage = "No event found for ${today} to post to Slack"
        println(notFoundMessage)
        return APIGatewayProxyResponseEvent().apply {
            statusCode = 404
            headers = mapOf("Content-type" to "text/plain")
            body = notFoundMessage
        }
    }

    /**
     * Retrieves JSON file from S3, parse it and return the Object when it's today.
     */
    private fun getEvent(day: LocalDate): LuminisEvent? {
        getEventFromBucket()?.let {
            val event = Klaxon().parse<LuminisEvent>(it)
            event?.let { nextEvent ->
                val eventDay = LocalDate.parse(nextEvent.date, DateTimeFormatter.ISO_DATE)
                if(eventDay.equals(day)) {
                    return nextEvent
                }
            }
        }
        return null
    }

    fun getEventFromBucket(): InputStream? {
        val s3Bucket = Config.s3Bucket
        if(s3client.doesObjectExist(s3Bucket, filename)) {
            return s3client.getObject(s3Bucket, filename).objectContent
        }
        println("'$filename' does not exists in the bucket: $s3Bucket")
        return null
    }
}
