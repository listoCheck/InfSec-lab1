# Secure REST API — InfSec Lab 1

Учебный REST API на Java 17, Spring Boot, JdbcTemplate и H2. Проект демонстрирует базовые меры из OWASP Top 10 и автоматические security-проверки в GitHub Actions.

## Запуск

```bash
set JWT_SECRET=replace-with-a-random-secret-of-at-least-32-bytes
./gradlew bootRun
```

Для PowerShell: `$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-bytes"; .\gradlew.bat bootRun`.

Демонстрационная учётная запись: `student` / `SecurePass123!`. Используется только для локальной демонстрации.

## API

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/auth/login` | Вход и получение JWT |
| GET | `/api/data` | Получение списка постов (нужен JWT) |
| POST | `/api/posts` | Создание поста (нужен JWT) |

Получение токена:

```bash
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"username":"student","password":"SecurePass123!"}'
```

Защищённый запрос:

```bash
curl http://localhost:8080/api/data -H "Authorization: Bearer <TOKEN>"
curl -X POST http://localhost:8080/api/posts -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d '{"content":"My post"}'
```

Без `Authorization: Bearer <TOKEN>` endpoints `/api/*` возвращают `401 Unauthorized`.

## Реализованные меры защиты

- **SQL injection:** работа с H2 выполняется только через `JdbcTemplate` с плейсхолдерами `?`; пользовательский ввод не конкатенируется с SQL.
- **XSS:** автор и текст поста проходят `HtmlUtils.htmlEscape` перед включением в JSON-ответ. Это безопасное HTML-представление для клиентов, которые отображают поля на странице.
- **Аутентификация:** пароль хранится как bcrypt-хэш (`BCryptPasswordEncoder`); при логине выполняется `matches`, а не сравнение открытых паролей. Успешный вход выдаёт подписанный HS256 JWT с subject, временем выпуска и истечения (30 минут). Фильтр проверяет подпись и срок действия токена на каждом `/api/*` запросе.
- **Валидация:** ограничения длины и обязательности полей проверяются до обработки запроса. Ошибка входа одинакова для неизвестного пользователя и неверного пароля, что уменьшает user enumeration.
- **Секрет:** в production `JWT_SECRET` задаётся через переменную окружения и должен быть не короче 32 байт; он не хранится в коде.

## CI/CD и отчёты

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) запускается на каждом `push` и `pull request`:

1. запускает тесты Gradle;
2. запускает SAST SpotBugs с плагином Find Security Bugs;
3. запускает SCA `OWASP Dependency-Check` и публикует HTML/JSON-отчёт как artifact.

Конфигурация `config/spotbugs/exclude.xml` документирует два проверенных ложных срабатывания SpotBugs: намеренное завершение запуска при слабом JWT-секрете и безопасное DI-внедрение Spring-компонентов.

Для быстрого обновления CVE-базы создайте ключ на [NVD](https://nvd.nist.gov/developers/request-an-api-key) и добавьте его в GitHub: **Settings → Secrets and variables → Actions → New repository secret**, имя `NVD_API_KEY`. Workflow передаёт его сканеру только через переменную окружения. Без ключа первый SCA-запуск может выполняться десятки минут из-за лимитов NVD.

После публикации репозитория добавьте в этот раздел скриншоты успешного workflow и скачанного отчёта Dependency-Check, а также ссылку на последний успешный запуск GitHub Actions.

## Контрольные вопросы

1. **Почему bcrypt, а не SHA-256?** bcrypt намеренно медленный, использует соль и настраиваемую стоимость вычисления. Быстрый SHA-256 позволяет атакующему быстро перебирать пароли после утечки базы.
2. **SAST и DAST:** SAST анализирует исходный код/зависимости без запущенного приложения; DAST атакует работающее приложение извне, наблюдая его поведение.
3. **JWT:** сервер создаёт компактный токен из header, payload и подписи. Payload содержит claims (в данном проекте subject, `iat`, `exp`); сервер проверяет HMAC-подпись секретом и срок `exp` перед допуском к ресурсу.
4. **Риски неаудированных зависимостей:** библиотека может содержать известную уязвимость, через которую атакующий обойдёт защиту приложения или выполнит код. SCA сопоставляет версии библиотек с базами CVE.

