package com.llamatik.app.feature.debugmenu.repositories

import com.llamatik.app.localization.AvailableLanguages
import com.llamatik.app.localization.Localization
import com.llamatik.app.platform.Environment
import com.llamatik.app.platform.ServerEnvironment
import com.russhwolf.settings.Settings

const val ENVIRONMENT_KEY = "ENVIRONMENT_KEY"
const val LANGUAGE_KEY = "LANGUAGE_KEY"
const val MOCKED_CONTENT_KEY = "MOCKED_CONTENT_KEY"
const val MOCKED_USER_KEY = "MOCKED_USER_KEY"
const val SERVER_PORT_KEY = "SERVER_PORT_KEY"
const val SERVER_PROVIDER_KEY = "SERVER_PROVIDER_KEY"
const val SERVER_ENABLED_KEY = "SERVER_ENABLED_KEY"

class GlobalAppSettingsRepository(
    private val settings: Settings,
    private val localization: Localization
) {
    fun getCurrentEnvironment(): Environment {
        return when (settings.getString(ENVIRONMENT_KEY, ServerEnvironment.PRODUCTION.name)) {
            ServerEnvironment.PRODUCTION.name -> {
                ServerEnvironment.PRODUCTION
            }

            ServerEnvironment.PREPRODUCTION.name -> {
                ServerEnvironment.PREPRODUCTION
            }

            else -> {
                ServerEnvironment.LOCALHOST
            }
        }
    }

    fun getCurrentLanguage(): AvailableLanguages {
        return when (settings.getString(LANGUAGE_KEY, AvailableLanguages.EN.name)) {
            AvailableLanguages.EN.name -> AvailableLanguages.EN
            AvailableLanguages.ES.name -> AvailableLanguages.ES
            AvailableLanguages.FR.name -> AvailableLanguages.FR
            AvailableLanguages.IT.name -> AvailableLanguages.IT
            AvailableLanguages.DE.name -> AvailableLanguages.DE
            else -> {
                AvailableLanguages.EN
            }
        }
    }

    fun isMockedContentEnabled(): Boolean {
        return settings.getBoolean(MOCKED_CONTENT_KEY, false)
    }

    fun setMockedContentCheckStatus(checked: Boolean) {
        settings.putBoolean(MOCKED_CONTENT_KEY, checked)
    }

    fun setSelectedEnvironment(environment: Environment) {
        settings.putString(ENVIRONMENT_KEY, environment.name)
    }

    fun setSelectedLanguage(language: AvailableLanguages) {
        settings.putString(LANGUAGE_KEY, language.name)
    }

    fun isMockedUserEnabled(): Boolean {
        return settings.getBoolean(MOCKED_USER_KEY, false)
    }

    fun getServerPort(): Int {
        return settings.getInt(SERVER_PORT_KEY, 8080)
    }

    fun setServerPort(port: Int) {
        settings.putInt(SERVER_PORT_KEY, port)
    }

    fun getServerProvider(): String {
        return settings.getString(SERVER_PROVIDER_KEY, "BOTH")
    }

    fun setServerProvider(provider: String) {
        settings.putString(SERVER_PROVIDER_KEY, provider)
    }

    fun isServerEnabled(): Boolean {
        return settings.getBoolean(SERVER_ENABLED_KEY, false)
    }

    fun setServerEnabled(enabled: Boolean) {
        settings.putBoolean(SERVER_ENABLED_KEY, enabled)
    }
}
