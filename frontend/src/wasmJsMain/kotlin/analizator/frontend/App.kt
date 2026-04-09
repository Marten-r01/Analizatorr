package analizator.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val historyLimit = 10

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            val scope = rememberCoroutineScope()

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

            LaunchedEffect(backendUrl) {
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

            val selectedFileTooLarge = selectedFile != null &&
                    uploadConfig != null &&
                    selectedFile!!.sizeBytes > uploadConfig!!.maxFileSizeBytes.toLong()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Analizator",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Week 8: history list, final MVP polish, demo-ready flow",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (isConfigLoading || isUploading || isLoadingSaved || isLoadingHistory) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                SectionCard(title = "Подключение к backend") {
                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        label = { Text("Base URL backend") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                backendUrl = normalizeBackendUrl(backendUrlInput)
                                infoMessage = null
                                errorMessage = null
                            }
                        ) {
                            Text("Подключить")
                        }

                        TextButton(
                            onClick = {
                                backendUrlInput = "http://localhost:8080"
                                backendUrl = "http://localhost:8080"
                                infoMessage = null
                                errorMessage = null
                            }
                        ) {
                            Text("Сбросить")
                        }
                    }

                    Text("Текущий backend: $backendUrl")

                    if (uploadConfig != null) {
                        Text("Лимит файла: ${uploadConfig!!.maxFileSizeMb} МБ")
                        Text("Поле multipart: ${uploadConfig!!.fileFieldName}")
                        Text("Тип запроса: ${uploadConfig!!.acceptedRequestContentType}")
                    }
                }

                SectionCard(title = "Загрузка FASTA") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                pickSingleFastaFile { file ->
                                    selectedFile = file?.let {
                                        SelectedFile(
                                            file = it,
                                            name = it.name,
                                            sizeBytes = it.size.toLong()
                                        )
                                    }
                                    infoMessage = null
                                    errorMessage = null
                                }
                            }
                        ) {
                            Text("Выбрать файл")
                        }

                        Button(
                            enabled = selectedFile != null && !selectedFileTooLarge && !isUploading,
                            onClick = {
                                val file = selectedFile?.file ?: return@Button
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
                        ) {
                            Text("Загрузить и проанализировать")
                        }

                        TextButton(
                            onClick = {
                                selectedFile = null
                                report = null
                                savedAnalysisId = ""
                                infoMessage = null
                                errorMessage = null
                            }
                        ) {
                            Text("Очистить")
                        }
                    }

                    if (selectedFile == null) {
                        Text("Файл не выбран")
                    } else {
                        Text("Имя файла: ${selectedFile!!.name}")
                        Text("Размер файла: ${formatBytes(selectedFile!!.sizeBytes)}")
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
                        onValueChange = { savedAnalysisId = it },
                        label = { Text("experimentId") },
                        modifier = Modifier.widthIn(min = 220.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = savedAnalysisId.toIntOrNull() != null && !isLoadingSaved,
                            onClick = {
                                val experimentId = savedAnalysisId.toIntOrNull() ?: return@Button
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
                        ) {
                            Text("Получить по id")
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    infoMessage = null
                                    errorMessage = null
                                    refreshHistory()
                                }
                            }
                        ) {
                            Text("Обновить историю")
                        }
                    }
                }

                HistoryCard(
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

                if (infoMessage != null) {
                    MessageCard(
                        title = "Статус",
                        text = infoMessage!!,
                        isError = false
                    )
                }

                if (errorMessage != null) {
                    MessageCard(
                        title = "Ошибка",
                        text = errorMessage!!,
                        isError = true
                    )
                }

                if (report != null) {
                    ReportCard(report = report!!)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
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
    SectionCard(title = "История последних анализов") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Text("createdAt=${item.createdAtEpochMs}")

                        Button(
                            onClick = { onOpen(item) }
                        ) {
                            Text("Открыть")
                        }
                    }
                }
            }
        }
    }
}