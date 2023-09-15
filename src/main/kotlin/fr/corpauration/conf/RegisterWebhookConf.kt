package fr.corpauration.conf

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithConverter
import org.eclipse.microprofile.config.spi.Converter


@ConfigMapping(prefix = "webhook.register")
interface RegisterWebhookConf {
    @WithConverter(EmptyStringConverter::class)
    fun student(): String

    @WithConverter(EmptyStringConverter::class)
    fun professor(): String
}

class EmptyStringConverter : Converter<String> {

    companion object

    private

    fun readResolve(): Any = EmptyStringConverter
    override fun convert(value: String?): String {
        return value ?: ""
    }
}
