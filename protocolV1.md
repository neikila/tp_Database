Описание API по курсам frontend/java
Структура API общая для всех методов:

API поддерживает GET и POST запросы
Адреса методов в формате /api/vНОМЕР_ВЕРСИИ_API/URL_МЕТОДА
Сервер всегда возвращает JSON с обязательными полями
status — Статус ответа, пересекается с HTTP статусами
body — Основное тело ответа, может быть пустым

    Общее для всех методов
    Если метод добавляет/изменяет/удаляет данные на сервере только POST (singup/score)
    Если метод требователен к безопасности только POST (signin/signout)
    Если метод только читает данные тогда GET
    Поле "status" пересекается с http статусами https://ru.wikipedia.org/wiki/Список_кодов_состояния_HTTP

    200 — OK
    400 — Bad Request — Ошибка запроса
    401 — Unauthorized — Необходима авторизация
    404 — Not Found — Данные не найдены
    405 — Method Not Allowed — Пришли с GET на метод требующий POST
    500 — Internal Server Error — Ошибка сервера
    501 — Not Implemented — Метод не реализован
    и т.д.

Обязательные методы
/api/v1/auth/signup — Регистрация
Принимает только POST запросы
Запрос: POST

{
name: String,
email: String,
password: String
}

Ответ: 200

{ 
status: 200, 
body: { 
    id: 2, 
    name: "Vasya", 
    email: "vasya@mail.ru", 
    password: //last three symbols
    } 
}

/api/v1/auth/signin — Авторизация
Запрос: POST

{
email: String,
password: String
}

Ответ: 200

{
status: 200, 
body: { 
    login: "Vasya" 
    } 
}

/api/v1/auth/check — Проверка авторизации
Запрос: GET — без параметров
Ответ: 200

{ 
status: 200, 
body: { 
    id: 2, 
    name: "Vasya", 
    email: "vasya@mail.ru", 
    password: //last three symbols
    server: "12",
    role: "User",
    score: 1000
    } 
}

Ответ: 401

{ 
status: 401, 
body: {message: "Unauthorised"} 
}


/api/v1/auth/signout — Сброс авторизации
Запрос: POST — без параметров
Ответ: 200 / 401

{ 
status: 200, 
body: {} 
}

/api/v1/scores — Игровая статистика
Запрос: GET

{
sort: {
    by: "date",
    order: "asc"
    }
}

Ответ: 200 / 401

{
"data": 
{
    "scoreList": 
        [
        {
            "score": 14,
                "login": "Vasya"
        },
        {
            "score": 12,
            "login": "Vanya"
        },
        {
            "score": 10,
            "login": "Petya"
        },
        {
            "score": 2,
            "login": "Danya"
        }
    ]
},
"status": 200
}

{ 
status: 401, 
body: {message: "Unauthorised"} 
}

