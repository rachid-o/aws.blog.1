package eu.luminis.blog

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL


class RestClient {

    fun post(url: String, message: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 300_000
        connection.doOutput = true

        val postData = message.toByteArray()

        connection.setRequestProperty("charset", "utf-8")
        connection.setRequestProperty("Content-length", postData.size.toString())
        connection.setRequestProperty("Content-Type", "application/json")

        try {
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.write(postData)
            outputStream.flush()
        } catch (exception: Exception) {
            println("Exception occurred: $exception")

        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK && connection.responseCode != HttpURLConnection.HTTP_CREATED) {
            try {
                val output = connection.errorStream.bufferedReader().use { it.readText() }
                println("There was error while connecting: $output")
            } catch (exception: Exception) {
                throw Exception("Exception while reading the error: $exception.message")
            }
        }

    }
}