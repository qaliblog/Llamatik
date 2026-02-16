package com.llamatik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.llamatik.app.feature.debugmenu.viewmodel.AppSettingsSideEffects
import com.llamatik.app.feature.debugmenu.viewmodel.AppSettingsViewModel
import com.llamatik.app.feature.server.ServerProvider
import com.llamatik.app.localization.AvailableLanguages
import com.llamatik.app.localization.SetLanguage
import com.llamatik.app.localization.getCurrentLanguage
import com.llamatik.app.localization.getCurrentLocalization
import com.llamatik.app.ui.components.ColoredSnackBarHost
import com.llamatik.app.ui.components.Picker
import com.llamatik.app.ui.components.PickerModel
import com.llamatik.app.ui.components.PickerOption
import com.llamatik.app.ui.icon.LlamatikIcons
import com.llamatik.app.ui.theme.LlamatikTheme
import com.llamatik.app.ui.theme.Typography

class AppSettingsScreen : Screen {

    @Composable
    override fun Content() {
        LlamatikTheme {
            val currentNavigator = LocalNavigator.currentOrThrow
            val snackbarHostState = remember { SnackbarHostState() }
            val showingModal = remember { mutableStateOf(false) }

            val viewModel = koinScreenModel<AppSettingsViewModel>()

            SetupSideEffects(viewModel)
            AppSettingsContentView(viewModel, snackbarHostState, showingModal) {
                currentNavigator.pop()
            }
        }
    }

    @Composable
    private fun SetupSideEffects(
        viewModel: AppSettingsViewModel
    ) {
        val coroutineScope = rememberCoroutineScope()
        val sideEffects = viewModel.sideEffects.collectAsState(
            AppSettingsSideEffects.Initial,
            coroutineScope.coroutineContext
        )
        when (sideEffects.value) {
            AppSettingsSideEffects.Initial -> {}
        }
    }

    @Composable
    fun AppSettingsContentView(
        viewModel: AppSettingsViewModel,
        snackbarHostState: SnackbarHostState,
        showingModal: MutableState<Boolean>,
        onClose: () -> Unit
    ) {
        val localization = getCurrentLocalization()
        val state by viewModel.state.collectAsState()

        if (state.currentLanguage != getCurrentLanguage()) {
            SetLanguage(state.currentLanguage)
        }

        val pickerModel = remember {
            mutableStateOf(
                PickerModel(
                    localization.chooseLanguage,
                    null,
                    AvailableLanguages.toPickerList { language ->
                        viewModel.onSelectedLanguage(language)
                        showingModal.value = false
                    }
                )
            )
        }

        LlamatikTheme {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        snackbar = {
                            ColoredSnackBarHost(snackbarHostState)
                        }
                    )
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Settings",
                                style = Typography.get().bodyLarge
                            )
                        },
                        colors = TopAppBarDefaults.mediumTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        navigationIcon = {
                            IconButton(onClick = { onClose.invoke() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .padding(it).padding(bottom = 46.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row {
                                Text(
                                    text = localization.language
                                )
                                Spacer(Modifier.size(16.dp))
                                Text(
                                    text = getCorrectLanguageName(state.currentLanguage.toString()),
                                    style = Typography.get().bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                pickerModel.value = PickerModel(
                                    localization.chooseLanguage,
                                    null,
                                    AvailableLanguages.toPickerList { language ->
                                        viewModel.onSelectedLanguage(language)
                                        showingModal.value = false
                                    }
                                )
                                showingModal.value = true
                            }
                        ) {
                            Text(
                                text = localization.change
                            )
                        }
                    }

                    Spacer(Modifier.size(32.dp))

                    Text(
                        text = "AI Server",
                        style = Typography.get().bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    Spacer(Modifier.size(8.dp))

                    LabelledSwitch(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                        label = "Enable Server",
                        checked = state.isServerRunning
                    ) {
                        viewModel.onServerToggle(it)
                    }

                    Spacer(Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Provider")
                        Button(
                            onClick = {
                                pickerModel.value = PickerModel(
                                    "Choose Provider",
                                    null,
                                    listOf(
                                        PickerOption("OpenAI", null) {
                                            viewModel.onServerProviderChanged(ServerProvider.OPENAI)
                                            showingModal.value = false
                                        },
                                        PickerOption("Ollama", null) {
                                            viewModel.onServerProviderChanged(ServerProvider.OLLAMA)
                                            showingModal.value = false
                                        },
                                        PickerOption("Both", null) {
                                            viewModel.onServerProviderChanged(ServerProvider.BOTH)
                                            showingModal.value = false
                                        }
                                    )
                                )
                                showingModal.value = true
                            }
                        ) {
                            Text(text = state.serverProvider.name)
                        }
                    }

                    Spacer(Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Port")
                        Button(
                            onClick = {
                                pickerModel.value = PickerModel(
                                    "Choose Port",
                                    null,
                                    listOf(8080, 11434, 4000).map { port ->
                                        PickerOption(port.toString(), null) {
                                            viewModel.onServerPortChanged(port)
                                            showingModal.value = false
                                        }
                                    }
                                )
                                showingModal.value = true
                            }
                        ) {
                            Text(text = state.serverPort.toString())
                        }
                    }
                }
            }
            if (showingModal.value) {
                Picker(
                    modifier = Modifier.padding(bottom = 0.dp),
                    pickerModel = pickerModel.value
                ) { showingModal.value = false }
            }
        }
    }

    @Composable
    fun LabelledCheckbox(
        modifier: Modifier = Modifier,
        label: String,
        checked: Boolean,
        enabled: Boolean = true,
        colors: CheckboxColors = CheckboxDefaults.colors(),
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = modifier.height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = colors
            )
            Spacer(Modifier.width(32.dp))
            Text(label)
        }
    }

    @Composable
    fun LabelledSwitch(
        label: String,
        modifier: Modifier = Modifier,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = LlamatikIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

private fun AvailableLanguages.Companion.toPickerList(action: (AvailableLanguages) -> Unit): List<PickerOption> {
    val pickerList = mutableListOf<PickerOption>()
    this.languages.map {
        pickerList.add(
            PickerOption(getCorrectLanguageName(it.name), null) {
                action.invoke(it)
            }
        )
    }
    return pickerList
}

fun getCorrectLanguageName(name: String): String {
    return when (name) {
        "DE" -> "German"
        "EN" -> "English"
        "ES" -> "Spanish"
        "IT" -> "Italian"
        "FR" -> "French"
        "RU" -> "Russian"
        "CN" -> "Chinese"
        else -> "English"
    }
}
