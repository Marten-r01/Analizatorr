# Analizator: техническая документация по проекту

Этот документ описывает текущую реализацию проекта по состоянию исходников в репозитории. Его цель не заменить чтение кода, а дать опорную карту: что делает проект, как устроен код, где контракты, где ограничения, где техдолг и как безопасно расширять систему.

## 1. Что это за проект

`Analizator` - full stack приложение на Kotlin для загрузки FASTA-файла, валидации последовательности, базового биоинформатического анализа, сохранения результата в БД и отображения результата в web UI.

Текущая реализованная цепочка выглядит так:

1. Пользователь загружает один FASTA-файл через frontend.
2. Frontend отправляет multipart-запрос на backend.
3. Backend:
   - извлекает файл из multipart,
   - парсит FASTA,
   - валидирует заголовок и последовательность,
   - считает статистику по нуклеотидам и GC,
   - ищет ORF,
   - транслирует ORF в аминокислотные последовательности,
   - сохраняет результат в PostgreSQL,
   - отдает клиенту JSON-отчет.
4. Frontend показывает отчет, список ORF, белковые последовательности и историю последних анализов.

## 2. Что проект делает сейчас и чего не делает

### Делает

- принимает один FASTA-файл;
- принимает только ДНК-алфавит `A/T/G/C`;
- нормализует последовательность в uppercase;
- поддерживает многострочную последовательность внутри одной записи;
- ищет ORF в прямой цепи по 3 forward reading frames;
- сохраняет результаты анализа в реляционной БД;
- дает историю последних анализов;
- позволяет повторно открыть анализ по `experimentId`.

### Не делает

- не поддерживает несколько FASTA-записей в одном файле;
- не поддерживает RNA (`U`) и неоднозначные IUPAC-символы (`N`, `R`, `Y` и т.д.);
- не ищет ORF на reverse complement;
- не делает выравнивание, поиск мутаций, аннотацию генов, BLAST и т.п.;
- не использует миграции схемы БД;
- не имеет общей shared-модели DTO между backend и frontend;
- не имеет полноценных frontend-тестов;
- не имеет production-grade auth, rate limit, audit, observability.

Это важно для защиты проекта: проект корректно позиционировать как MVP/учебный full stack сервис для базового анализа FASTA, а не как полнофункциональную биоинформатическую платформу.

## 3. Стек и версии

### Root / Build

- Gradle wrapper: `8.10.2`
- Kotlin plugins: `2.3.20`

### Backend

- Kotlin/JVM
- Ktor `3.4.1`
- Exposed `1.1.1`
- PostgreSQL driver `42.7.5`
- Logback `1.5.18`
- Test DB: H2 `2.3.232`
- JVM toolchain: Java `17`

### Frontend

- Kotlin Multiplatform (Wasm)
- Compose Multiplatform plugin `1.7.3`
- Kotlin Compose plugin `2.3.20`
- kotlinx.serialization-json `1.10.0`
- kotlinx.coroutines-core `1.10.2`

## 4. Структура проекта

```text
backend/
  src/main/kotlin/analizator/
    Application.kt
    SequenceAnalysisService.kt
    FastaParser.kt
    FastaValidator.kt
    GcAnalyzer.kt
    OrfFinder.kt
    DnaTranslator.kt
    ExposedAnalysisRepository.kt
    DatabaseFactory.kt
    *Table.kt
    dto/

  src/test/kotlin/analizator/
    *Test.kt

frontend/
  src/wasmJsMain/kotlin/analizator/frontend/
    Main.kt
    App.kt
    ApiClient.kt
    Models.kt
    BrowserFilePicker.kt
    SequenceVisualization.kt
    ReportCard.kt
    FrontendFormatting.kt
```

## 5. Как запускать проект локально

### Требования

- JDK 17+
- PostgreSQL
- Gradle wrapper из репозитория

### Переменные окружения backend

Если переменные не заданы, backend использует значения по умолчанию:

- `DB_URL=jdbc:postgresql://localhost:5432/analizator`
- `DB_USER=postgres`
- `DB_PASSWORD=postgres`
- `DB_DRIVER=org.postgresql.Driver`

### Команды запуска

Backend:

```bash
./gradlew :backend:run
```

Frontend dev server:

```bash
./gradlew :frontend:wasmJsBrowserDevelopmentRun
```

Проверка backend:

```bash
curl http://localhost:8080/health
```

### Что важно знать про запуск

- backend при старте сам подключается к БД и пытается создать недостающие таблицы;
- frontend по умолчанию ходит в `http://localhost:8080`;
- README сейчас не является полным operational guide, потому что в репозитории нет `docker-compose` файла, хотя README его упоминает.

### Текущее состояние тестовой команды

Базовая backend-команда проверки:

```bash
./gradlew :backend:test
```

Но на текущем состоянии исходников она не проходит из-за рассинхронизации части тестов с production-кодом. Это описано ниже в разделе про подводные камни.

## 6. Архитектура и разделение ответственности

Архитектура простая, двухмодульная:

- `backend` - REST API, доменная логика анализа, persistence;
- `frontend` - Compose/Wasm UI, сетевой клиент, пользовательские сценарии.

### Backend-архитектура

В backend уже просматривается понятное разделение:

- `Application.kt` - composition root и HTTP-routing;
- `SequenceAnalysisService.kt` - orchestration доменного пайплайна;
- `FastaParser`, `FastaValidator`, `GcAnalyzer`, `OrfFinder`, `DnaTranslator` - бизнес-логика;
- `AnalysisRepository` - boundary для хранения;
- `ExposedAnalysisRepository` - реализация persistence через Exposed/JDBC;
- `*Table.kt` - схема таблиц;
- `dto/*` и `ApiMappings.kt` - API-контракты и маппинг.

### Frontend-архитектура

Frontend сейчас устроен проще:

- `Main.kt` - вход в Compose/Wasm;
- `App.kt` - основной экран и почти весь state management;
- `ApiClient.kt` - вызовы backend API;
- `Models.kt` - frontend DTO;
- `ReportCard.kt`, `SequenceVisualization.kt` - отображение результатов;
- `BrowserFilePicker.kt` - bridge к browser file input.

Это рабочая, но предельно плоская структура. Для MVP она приемлема; при росте функциональности state и network-flow лучше выносить из `App.kt`.

## 7. Backend: жизненный цикл запроса

### 6.1 Entry point

Главная точка входа - `backend/src/main/kotlin/analizator/Application.kt`.

При старте:

1. поднимается Netty на `0.0.0.0:8080`;
2. читаются переменные окружения БД;
3. вызывается `DatabaseFactory.connect(...)`;
4. вызывается `DatabaseFactory.createSchema()`;
5. создается `ExposedAnalysisRepository`;
6. регистрируются Ktor routes.

### 6.2 Конвейер анализа

Основной сценарий живет в `SequenceAnalysisService.analyzeAndSave(...)`.

Пайплайн:

1. `FastaParser.parse(lines)`
2. `GcAnalyzer.analyze(sequence)`
3. `OrfFinder.find(sequence)`
4. `DnaTranslator.translate(orfs)`
5. сборка `SequenceReport`
6. `repository.save(report, originalFileName)`

Сервис не содержит знания о HTTP и БД-таблицах, только координирует доменные компоненты.

## 8. Доменная модель

Ключевые сущности backend:

- `FastaRecord` - результат парсинга FASTA;
- `SequenceStats` - длина, A/T/G/C count, GC%;
- `Orf` - найденный ORF;
- `ProteinTranslation` - аминокислотная последовательность для ORF;
- `SequenceReport` - полный отчет по анализу;
- `AnalysisSummary` - сокращенная запись для истории.

### Важный инвариант

`SequenceReport.experimentId` до сохранения может быть `null`, после сохранения должен быть заполнен. Это не просто поле: `ApiMappings.toDto()` жестко требует non-null `experimentId`.

Следствие: `SequenceReport`, который еще не прошел через `repository.save(...)`, нельзя безопасно отдавать наружу как API response.

## 9. Алгоритмы и биоинформатические допущения

### 8.1 FASTA parsing

`FastaParser`:

- trim'ит строки;
- удаляет пустые строки;
- берет первую строку как header;
- склеивает остальные строки в одну последовательность;
- переводит последовательность в uppercase.

`FastaValidator`:

- требует непустой файл;
- требует `>` в первой строке;
- требует ровно одну FASTA-запись;
- разрешает только `A/T/G/C`.

### 8.2 GC-анализ

`GcAnalyzer` считает:

- длину;
- количество `A`, `T`, `G`, `C`;
- `gcPercent = (G + C) / length * 100`;
- результат округляется до 2 знаков через `HALF_UP`.

### 8.3 Поиск ORF

`OrfFinder`:

- ищет только в прямой цепи;
- проходит reading frames `0..2`;
- старт-кодон: `ATG`;
- стоп-кодоны: `TAA`, `TAG`, `TGA`;
- для каждого найденного старта берет первый подходящий стоп в том же frame.

Важно понимать точную семантику:

- ORF координаты `start` и `end` в модели 1-based;
- `end` фактически соответствует позиции конца stop-codon включительно;
- `length = endExclusive - startIndexInZeroBased`;
- в ORF sequence включается и stop-codon;
- ORF сортируются по `frame`, затем по `start`.

#### Подводный камень

Алгоритм не ищет "лучший" ORF и не выбирает самый длинный. Он ищет каждый старт и привязывает его к первому стопу в рамке. Из-за этого возможны перекрывающиеся ORF, и это нормальное поведение для текущей реализации.

### 8.4 Трансляция

`DnaTranslator`:

- принимает ORF, длина которых кратна 3;
- идет по codon table;
- прекращает трансляцию на `*`;
- stop-codon в белковую строку не попадает.

Практический вывод: белковая последовательность короче ORF на один кодон, если ORF оканчивается стоп-кодоном, что и происходит в текущем пайплайне.

## 10. Persistence и схема БД

Persistence реализован через `ExposedAnalysisRepository`.

### Таблицы

#### `experiments`

- `id`
- `original_file_name`
- `status`
- `created_at_epoch_ms`

#### `sequences`

- `id`
- `experiment_id`
- `header`
- `raw_sequence`
- `normalized_sequence`
- `length`
- `gc_content`

#### `orfs`

- `id`
- `sequence_id`
- `frame`
- `start_pos`
- `end_pos`
- `length`
- `sequence`

#### `proteins`

- `id`
- `orf_id`
- `amino_acid_sequence`

### Как сохраняется отчет

При `save(...)`:

1. создается `experiment`;
2. создается `sequence`;
3. для каждого ORF создается запись в `orfs`;
4. если для ORF есть белок по тому же индексу, создается запись в `proteins`.

### Важные особенности persistence

- `header` режется до `512` символов перед сохранением;
- `status` всегда сохраняется как `"COMPLETED"`;
- `rawSequence` и `normalizedSequence` сейчас одинаковые;
- retrieval не восстанавливает `originalFileName` обратно в `SequenceReport`, потому что в domain-модели этого поля нет.

### Query pattern и техдолг

Текущая реализация читает данные просто и прозрачно, но не оптимально:

- `findByExperimentId(...)` делает отдельный запрос на белок для каждого ORF;
- `findLatest(...)` делает отдельные запросы на sequence и ORF count для каждого experiment.

Это N+1 pattern. Для учебного проекта и малого объема данных это приемлемо, но при росте нагрузки станет узким местом.

## 11. HTTP API

### `GET /health`

Возвращает:

```json
{ "status": "UP" }
```

### `GET /api/v1/upload-config`

Возвращает ограничения upload:

- максимальный размер;
- имя multipart-поля;
- ожидаемый content type.

### `GET /api/v1/analyses?limit=N`

Возвращает список последних анализов.

Ограничения:

- `limit` по умолчанию `10`;
- допустимый диапазон `1..100`.

### `GET /api/v1/analysis/{id}`

Возвращает один сохраненный анализ по `experimentId`.

### `POST /api/v1/analyze`

Принимает JSON:

```json
{
  "fastaContent": ">seq\nATGC",
  "originalFileName": "sample.fasta"
}
```

### `POST /api/v1/analyze-upload`

Принимает `multipart/form-data` с файлом в поле `file`.

### Ошибки

Backend использует `StatusPages` и возвращает:

- `400 Bad Request` для `IllegalArgumentException`;
- `413 Payload Too Large` для `IOException` из upload flow;
- `500 Internal Server Error` для прочих исключений.

## 12. Upload flow и ограничения

Upload logic живет в `MultipartFastaExtractor`.

Что важно:

- backend читает multipart;
- ищет первый `FileItem` в поле `file`;
- копирует содержимое во временный файл;
- затем читает весь файл в память;
- затем преобразует bytes в `UTF-8` string.

### Подводные камни

1. Размер ограничен `10 MB`, но файл все равно полностью читается в temp file и затем в память.
2. Это решение простое, но не streaming-friendly.
3. Если в будущем размер файла будет больше, нужна переработка upload path, а не простое увеличение лимита.

## 13. Frontend: как устроен UI

Frontend сейчас одноэкранный.

Основной flow в `App.kt`:

1. пользователь задает `backendUrl`;
2. frontend запрашивает `upload-config` и историю анализов;
3. пользователь выбирает FASTA-файл;
4. frontend отправляет upload;
5. frontend показывает отчет;
6. пользователь может открыть старый отчет по `experimentId` или из истории.

### Что хранится в UI state

В `App.kt` хранятся:

- адрес backend;
- конфигурация upload;
- выбранный файл;
- текущий `savedAnalysisId`;
- текущий открытый `report`;
- список `history`;
- `infoMessage` и `errorMessage`;
- флаги загрузки.

Это удобно для MVP, но state уже заметно разросся.

### Компоненты UI

- `SectionCard` - базовая секция;
- `ReportCard` - детальный отчет;
- `HistoryCard` - список последних анализов;
- `ColorizedSequence` - раскраска последовательности;
- `NucleotideDistributionChart` - диаграмма A/T/G/C;
- `OrfList`, `ProteinList` - списки ORF и белков.

### Frontend data contract

Frontend хранит собственные DTO в `frontend/.../Models.kt`.

Это важно: shared contract между модулями отсутствует. Сейчас поля backend и frontend согласованы вручную. Это значит, что любое изменение backend DTO нужно синхронно переносить во frontend.

## 14. Ключевые подводные камни проекта

Это не гипотезы "на будущее", а реальные точки внимания по текущему коду.

### 13.1 Несинхронность тестов и кода

Состояние backend-тестов сейчас неполностью актуально.

Проверка `:backend:test` показала:

- `SequenceAnalysisServiceTest` использует старый API (`analyze(...)`, отсутствие `repository`);
- `ApplicationApiTest` не компилируется из-за `bodyAsText` в текущем наборе зависимостей/импортов.

Следствие: тестовый слой сейчас нельзя считать полностью надежным индикатором корректности проекта.

### 13.2 Автосоздание схемы через deprecated API

`DatabaseFactory.createSchema()` использует `SchemaUtils.createMissingTablesAndColumns(...)`.

Компилятор/библиотека уже предупреждают, что такой способ может приводить к непредсказуемому состоянию схемы при частичном сбое. Для production-эволюции схемы нужен migration tool.

### 13.3 Нет shared domain/API model между frontend и backend

DTO дублируются в двух местах:

- `backend/src/main/kotlin/analizator/dto`
- `frontend/src/wasmJsMain/kotlin/analizator/frontend/Models.kt`

Это источник drift при расширении API.

### 13.4 Вероятный CORS-риск при отдельном запуске frontend dev server

Наблюдение по коду: backend не устанавливает Ktor CORS plugin и frontend делает browser `fetch` на явный `backendUrl`.

Вывод по исходникам: при запуске frontend dev server на отдельном origin браузер, вероятно, упрется в CORS, если этот вопрос не решен через dev-server proxy или отдельную настройку окружения.

Это именно вывод из текущего кода, а не зафиксированная в репозитории настройка.

### 13.5 README не полностью соответствует репозиторию

В README есть упоминание `docker-compose logs -f backend`, но compose-файл в репозитории отсутствует.

Следствие: README сейчас нельзя считать полным operational source of truth.

### 13.6 Domain limitations легко принять за баг

Если не знать проектных ограничений, можно ошибочно считать багами такие вещи:

- один FASTA entry на файл;
- только `A/T/G/C`;
- только forward ORF;
- отсутствие reverse complement;
- stop codon не входит в amino acid string;
- `createdAt` в UI показан как raw epoch ms, без форматирования.

Это не обязательно баги. Для текущего scope это сознательные или допустимые упрощения.

## 15. Как читать код в правильном порядке

Если нужно быстро войти в проект и действительно понимать его, порядок чтения такой:

1. `README.md`
2. `backend/build.gradle.kts`
3. `frontend/build.gradle.kts`
4. `backend/src/main/kotlin/analizator/Application.kt`
5. `backend/src/main/kotlin/analizator/SequenceAnalysisService.kt`
6. `FastaParser.kt`, `FastaValidator.kt`
7. `GcAnalyzer.kt`
8. `OrfFinder.kt`
9. `DnaTranslator.kt`
10. `AnalysisRepository.kt`
11. `ExposedAnalysisRepository.kt`
12. `DatabaseFactory.kt` и `*Table.kt`
13. `frontend/.../ApiClient.kt`
14. `frontend/.../App.kt`
15. `frontend/.../ReportCard.kt`
16. backend tests

Это даст сначала понимание behavior, потом internals, потом UI.

## 16. Как защищать проект

Удобная формулировка:

> Проект реализует полный пользовательский цикл базового анализа FASTA: от загрузки и проверки данных до вычисления биоинформатических характеристик, сохранения результата и его визуализации в web UI. Архитектурно система разделена на backend на Ktor с PostgreSQL/Exposed и frontend на Compose Multiplatform Web/Wasm.

На защите полезно четко уметь объяснить:

1. почему выбран Kotlin на обоих концах;
2. что такое FASTA и почему в MVP поддерживается только один entry;
3. как именно ищется ORF;
4. почему GC-content считается на нормализованной последовательности;
5. как domain result превращается в строки таблиц БД;
6. чем `SequenceReport` отличается от `AnalysisSummary`;
7. почему frontend хранит DTO отдельно и чем это рискованно;
8. какие ограничения проекта осознанные, а какие являются техдолгом.

### Хороший честный ответ про ограничения

Сильная защита проекта обычно не в том, чтобы скрыть упрощения, а в том, чтобы показать осознанность:

- scope ограничен учебным MVP;
- алгоритмы прозрачны и объяснимы;
- архитектура достаточно простая для демонстрации end-to-end flow;
- дальнейшее развитие понятно и локализовано.

## 17. Где безопасно расширять проект

### Если нужно добавить новый тип анализа

Безопасная точка:

- добавить новый доменный компонент в backend;
- встроить его в `SequenceAnalysisService`;
- расширить `SequenceReport`;
- расширить DTO;
- обновить frontend Models и UI.

### Если нужно расширить формат ввода

Смотри:

- `FastaValidator`
- `FastaParser`
- frontend file upload UX

Например, поддержка нескольких FASTA-записей затронет не только парсер, но и всю доменную модель, API response, persistence и UI.

### Если нужно улучшить производительность

Смотри:

- `MultipartFastaExtractor`
- `ExposedAnalysisRepository.findByExperimentId`
- `ExposedAnalysisRepository.findLatest`

### Если нужно сделать production-ready БД

Смотри:

- `DatabaseFactory.createSchema()`
- миграции вместо автосоздания
- индексы
- явные constraints
- стратегию статусов `experiment`

### Если нужно улучшить frontend maintainability

Следующие шаги логичны:

- вынести network/state orchestration из `App.kt`;
- ввести единый UI state model;
- убрать дублирование DTO через shared KMP module или codegen;
- добавить frontend tests.

## 18. Что нельзя менять без понимания последствий

Особенно осторожно относиться к:

- контрактам JSON DTO;
- координатам `start/end` у ORF;
- факту, что `sequence` в API уже нормализована;
- контракту `UploadConstraints.FILE_FIELD_NAME = "file"`;
- логике `toDto()`, где `experimentId` обязателен;
- схеме таблиц и способу связывания ORF/Protein по индексу при сохранении;
- входному предположению, что анализ идет по одной последовательности.

## 19. Практический чек-лист перед доработкой

Перед любым расширением проекта ответь на вопросы:

1. Это меняет только backend, только frontend или сквозной контракт?
2. Нужны ли изменения DTO?
3. Нужны ли изменения схемы БД?
4. Это сохраняет поддержку одного FASTA entry или ломает предположение?
5. Это влияет на ORF coordinates?
6. Нужны ли новые тесты или сначала надо починить текущие?
7. Не появится ли drift между frontend Models и backend dto?

## 20. Текущее состояние качества

По коду видно, что проект хорош как учебный full stack MVP:

- бизнес-логика отделена от transport layer;
- persistence вынесен за interface;
- API-слой относительно чистый;
- frontend демонстрирует полный пользовательский сценарий.

Но по engineering maturity еще есть понятный техдолг:

- backend-тесты не полностью актуальны;
- frontend без отдельного state holder;
- схема БД без миграций;
- DTO дублируются;
- производительность запросов пока не оптимизирована;
- operational docs в README неполные.

## 21. Краткий вывод

Проект уже ценен тем, что показывает цельный продуктовый сценарий: файл -> анализ -> БД -> API -> UI.

Если цель - защищать проект, тебе нужно особенно хорошо знать:

- ограничения текущего FASTA-парсинга;
- семантику ORF поиска;
- как формируется `SequenceReport`;
- как сохраняются данные в таблицы;
- где DTO и доменная модель различаются;
- какие части проекта уже production-like, а какие пока учебные.

Если цель - безопасно развивать проект дальше, то приоритетные инженерные задачи такие:

1. синхронизировать и починить тесты;
2. решить вопрос migration strategy;
3. убрать drift между backend/frontend DTO;
4. решить CORS/dev-runtime story;
5. при росте данных оптимизировать repository queries.
