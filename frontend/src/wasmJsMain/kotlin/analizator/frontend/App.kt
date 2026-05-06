@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package analizator.frontend

import analizator.dto.AnalysisSummaryDto
import analizator.dto.AnalyzeResponseDto
import analizator.dto.UploadConfigResponseDto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val historyLimit = 10

private enum class AppScreen {
    Analysis,
    History
}

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()

            var currentUser by remember { mutableStateOf<String?>(null) }
            var loginInput by remember { mutableStateOf("") }
            var passwordInput by remember { mutableStateOf("") }
            var authError by remember { mutableStateOf<String?>(null) }
            var activeScreen by remember { mutableStateOf(AppScreen.Analysis) }

            var backendUrlInput by remember { mutableStateOf("http://localhost:8080") }
            var backendUrl by remember { mutableStateOf("http://localhost:8080") }
            var uploadConfig by remember { mutableStateOf<UploadConfigResponseDto?>(null) }
            var selectedFile by remember { mutableStateOf<SelectedFile?>(null) }
            var savedAnalysisId by remember { mutableStateOf("") }
            var report by remember { mutableStateOf<AnalyzeResponseDto?>(null) }
            var history by remember { mutableStateOf<List<AnalysisSummaryDto>>(emptyList()) }
            var infoMessage by remember { mutableStateOf<String?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isConfigLoading by remember { mutableStateOf(false) }
            var isUploading by remember { mutableStateOf(false) }
            var isLoadingSaved by remember { mutableStateOf(false) }
            var isLoadingHistory by remember { mutableStateOf(false) }

            suspend fun refreshHistory() {
                isLoadingHistory = true
                runCatching {
                    ApiClient.getLatestAnalyses(backendUrl, historyLimit)
                }.onSuccess {
                    history = it
                }.onFailure {
                    errorMessage = it.message ?: "Не удалось загрузить историю анализов"
                }
                isLoadingHistory = false
            }

            LaunchedEffect(backendUrl, currentUser) {
                if (currentUser == null) return@LaunchedEffect

                isConfigLoading = true
                errorMessage = null
                uploadConfig = null

                runCatching {
                    ApiClient.getUploadConfig(backendUrl)
                }.onSuccess {
                    uploadConfig = it
                }.onFailure {
                    errorMessage = it.message ?: "Не удалось получить конфигурацию загрузки"
                }

                isConfigLoading = false
                refreshHistory()
            }

            if (currentUser == null) {
                LoginScreen(
                    login = loginInput,
                    password = passwordInput,
                    error = authError,
                    onLoginChange = { loginInput = it },
                    onPasswordChange = { passwordInput = it },
                    onSubmit = {
                        val normalizedLogin = loginInput.trim()
                        if (normalizedLogin.isBlank() || passwordInput.isBlank()) {
                            authError = "Введите логин и пароль"
                        } else {
                            currentUser = normalizedLogin
                            passwordInput = ""
                            authError = null
                            activeScreen = AppScreen.Analysis
                        }
                    }
                )
                return@Surface
            }

            val selectedFileTooLarge = selectedFile?.let { file ->
                uploadConfig?.let { config ->
                    file.sizeBytes > config.maxFileSizeBytes.toLong()
                }
            } ?: false

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardHeader(
                    userName = requireNotNull(currentUser),
                    onLogout = {
                        currentUser = null
                        report = null
                        selectedFile = null
                        savedAnalysisId = ""
                        infoMessage = null
                        errorMessage = null
                    }
                )

                if (isConfigLoading || isUploading || isLoadingSaved || isLoadingHistory) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                BackendConnectionCard(
                    backendUrlInput = backendUrlInput,
                    backendUrl = backendUrl,
                    uploadConfig = uploadConfig,
                    onBackendUrlInputChange = { backendUrlInput = it },
                    onConnect = {
                        backendUrl = normalizeBackendUrl(backendUrlInput)
                        infoMessage = null
                        errorMessage = null
                    },
                    onReset = {
                        backendUrlInput = "http://localhost:8080"
                        backendUrl = "http://localhost:8080"
                        infoMessage = null
                        errorMessage = null
                    }
                )

                MainMenu(
                    activeScreen = activeScreen,
                    historyCount = history.size,
                    onScreenSelected = { activeScreen = it }
                )

                infoMessage?.let { message ->
                    MessageCard(
                        title = "Статус",
                        text = message,
                        isError = false
                    )
                }

                errorMessage?.let { message ->
                    MessageCard(
                        title = "Ошибка",
                        text = message,
                        isError = true
                    )
                }

                when (activeScreen) {
                    AppScreen.Analysis -> AnalysisScreen(
                        uploadConfig = uploadConfig,
                        selectedFile = selectedFile,
                        savedAnalysisId = savedAnalysisId,
                        report = report,
                        selectedFileTooLarge = selectedFileTooLarge,
                        isUploading = isUploading,
                        isLoadingSaved = isLoadingSaved,
                        onPickFile = {
                            pickSingleFastaFile { file ->
                                selectedFile = file?.let {
                                    SelectedFile(
                                        file = it,
                                        name = it.name,
                                        sizeBytes = it.size.toInt()
                                    )
                                }
                                infoMessage = null
                                errorMessage = null
                            }
                        },
                        onUpload = {
                            val file = selectedFile?.file
                            if (file != null) {
                                scope.launch {
                                    isUploading = true
                                    infoMessage = null
                                    errorMessage = null

                                    runCatching {
                                        ApiClient.uploadFasta(backendUrl, file)
                                    }.onSuccess {
                                        report = it
                                        savedAnalysisId = it.experimentId.toString()
                                        infoMessage = "Анализ сохранён под id=${it.experimentId}"
                                        refreshHistory()
                                    }.onFailure {
                                        errorMessage = it.message ?: "Не удалось загрузить FASTA-файл"
                                    }

                                    isUploading = false
                                }
                            }
                        },
                        onClear = {
                            selectedFile = null
                            report = null
                            savedAnalysisId = ""
                            infoMessage = null
                            errorMessage = null
                        },
                        onSavedAnalysisIdChange = { savedAnalysisId = it },
                        onLoadSaved = {
                            val experimentId = savedAnalysisId.toIntOrNull()
                            if (experimentId != null) {
                                scope.launch {
                                    isLoadingSaved = true
                                    infoMessage = null
                                    errorMessage = null

                                    runCatching {
                                        ApiClient.getAnalysisById(backendUrl, experimentId)
                                    }.onSuccess {
                                        report = it
                                        infoMessage = "Загружен сохранённый анализ id=${it.experimentId}"
                                    }.onFailure {
                                        errorMessage = it.message ?: "Не удалось получить анализ по id"
                                    }

                                    isLoadingSaved = false
                                }
                            }
                        }
                    )

                    AppScreen.History -> HistoryCard(
                        history = history,
                        onOpen = { item ->
                            scope.launch {
                                isLoadingSaved = true
                                infoMessage = null
                                errorMessage = null
                                savedAnalysisId = item.experimentId.toString()

                                runCatching {
                                    ApiClient.getAnalysisById(backendUrl, item.experimentId)
                                }.onSuccess {
                                    report = it
                                    activeScreen = AppScreen.Analysis
                                    infoMessage = "Открыт анализ из истории id=${it.experimentId}"
                                }.onFailure {
                                    errorMessage = it.message ?: "Не удалось открыть анализ из истории"
                                }

                                isLoadingSaved = false
                            }
                        },
                        onRefresh = {
                            scope.launch {
                                infoMessage = null
                                errorMessage = null
                                refreshHistory()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    login: String,
    password: String,
    error: String?,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Analizator",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Вход в систему анализа FASTA",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = login,
                    onValueChange = onLoginChange,
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти")
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    userName: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Analizator",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Пользователь: $userName",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        TextButton(onClick = onLogout) {
            Text("Выйти")
        }
    }
}

@Composable
private fun BackendConnectionCard(
    backendUrlInput: String,
    backendUrl: String,
    uploadConfig: UploadConfigResponseDto?,
    onBackendUrlInputChange: (String) -> Unit,
    onConnect: () -> Unit,
    onReset: () -> Unit
) {
    SectionCard(title = "Подключение к backend") {
        OutlinedTextField(
            value = backendUrlInput,
            onValueChange = onBackendUrlInputChange,
            label = { Text("Base URL backend") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onConnect) {
                Text("Подключить")
            }

            TextButton(onClick = onReset) {
                Text("Сбросить")
            }
        }

        Text("Текущий backend: $backendUrl")

        uploadConfig?.let { config ->
            Text("Лимит файла: ${config.maxFileSizeMb} МБ")
            Text("Поле multipart: ${config.fileFieldName}")
            Text("Тип запроса: ${config.acceptedRequestContentType}")
        }
    }
}

@Composable
private fun MainMenu(
    activeScreen: AppScreen,
    historyCount: Int,
    onScreenSelected: (AppScreen) -> Unit
) {
    SectionCard(title = "Меню") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(
                text = "Анализ",
                selected = activeScreen == AppScreen.Analysis,
                onClick = { onScreenSelected(AppScreen.Analysis) }
            )
            MenuButton(
                text = "История ($historyCount)",
                selected = activeScreen == AppScreen.History,
                onClick = { onScreenSelected(AppScreen.History) }
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(text)
        }
    }
}

@Composable
private fun AnalysisScreen(
    uploadConfig: UploadConfigResponseDto?,
    selectedFile: SelectedFile?,
    savedAnalysisId: String,
    report: AnalyzeResponseDto?,
    selectedFileTooLarge: Boolean,
    isUploading: Boolean,
    isLoadingSaved: Boolean,
    onPickFile: () -> Unit,
    onUpload: () -> Unit,
    onClear: () -> Unit,
    onSavedAnalysisIdChange: (String) -> Unit,
    onLoadSaved: () -> Unit
) {
    SectionCard(title = "Анализ FASTA") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPickFile) {
                Text("Выбрать файл")
            }

            Button(
                enabled = selectedFile != null && !selectedFileTooLarge && !isUploading,
                onClick = onUpload
            ) {
                Text("Загрузить и проанализировать")
            }

            TextButton(onClick = onClear) {
                Text("Очистить")
            }
        }

        if (selectedFile == null) {
            Text("Файл не выбран")
        } else {
            Text("Имя файла: ${selectedFile.name}")
            Text("Размер файла: ${formatBytes(selectedFile.sizeBytes)}")
        }

        if (selectedFileTooLarge) {
            Text(
                text = "Файл превышает допустимый лимит ${uploadConfig?.maxFileSizeMb ?: 10} МБ",
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    SectionCard(title = "Получение сохранённого анализа") {
        OutlinedTextField(
            value = savedAnalysisId,
            onValueChange = onSavedAnalysisIdChange,
            label = { Text("experimentId") },
            modifier = Modifier.widthIn(min = 220.dp),
            singleLine = true
        )

        Button(
            enabled = savedAnalysisId.toIntOrNull() != null && !isLoadingSaved,
            onClick = onLoadSaved
        ) {
            Text("Получить по id")
        }
    }

    if (report == null) {
        SectionCard(title = "Результат анализа") {
            Text("Результат появится после загрузки FASTA-файла или открытия записи из истории")
        }
    } else {
        ReportCard(report = report)
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            content()
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    text: String,
    isError: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = text)
        }
    }
}

@Composable
private fun HistoryCard(
    history: List<AnalysisSummaryDto>,
    onOpen: (AnalysisSummaryDto) -> Unit,
    onRefresh: () -> Unit
) {
    SectionCard(title = "История запросов") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Показано: ${history.size}")
            TextButton(onClick = onRefresh) {
                Text("Обновить")
            }
        }

        if (history.isEmpty()) {
            Text("История пока пуста")
        } else {
            history.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("id=${item.experimentId}")
                        Text("header=${item.header}")
                        Text("length=${item.sequenceLength}")
                        Text("GC=${formatDouble(item.gcPercent)}%")
                        Text("ORFs=${item.orfCount}")
                        Text("createdAt=${formatFrontendTimestamp(item.createdAtEpochMs)}")
                        Button(onClick = { onOpen(item) }) {
                            Text("Открыть результат")
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeBackendUrl(url: String): String {
    return url.trim().removeSuffix("/")
}

private fun formatBytes(bytes: Int): String {
    val kb = 1024.0
    val mb = kb * 1024.0

    return when {
        bytes >= mb -> "${formatDouble(bytes / mb)} MB"
        bytes >= kb -> "${formatDouble(bytes / kb)} KB"
        else -> "$bytes B"
    }
}
