package eu.luminis.blog

object Config {
    val s3Bucket by lazy { getRequiredEnv("S3_BUCKET") }
    val slackWebhook by lazy { getRequiredEnv("SLACK_WEBHOOK_URL") }

    private fun getRequiredEnv(name: String): String {
        println("Retrieving environment variable: $name")
        return System.getenv(name) ?: throw IllegalStateException("Missing env var '$name'!")
    }
}
