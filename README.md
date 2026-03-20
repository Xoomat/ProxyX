# ProxyX

Лёгкое Android-приложение для запуска локального Telegram proxy прямо на телефоне.

<p align="center">
  <a href="https://github.com/Xoomat/ProxyX/releases/latest"><img src="https://img.shields.io/badge/ProxyX-Download%20APK-111111?style=for-the-badge&logo=android&logoColor=white&labelColor=000000" /></a>&nbsp;&nbsp;<a href="https://github.com/Xoomat/ProxyX/releases/latest"><img src="https://img.shields.io/badge/View-Releases-222222?style=for-the-badge" /></a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/downloads/Xoomat/ProxyX/total?style=for-the-badge" />
</p>

---

## Что это вообще

ProxyX поднимает локальный proxy прямо на устройстве и даёт быстрый способ подключить его к Telegram без лишних телодвижений.

Без серверов, без лишней настройки — всё запускается на телефоне.

---

## Установка

1. Открой страницу релизов (кнопка выше)
2. Скачай последний `.apk`
3. Установи его на устройство
4. Если нужно — разреши установку из неизвестных источников

---

## Использование

1. Запусти `ProxyX`
2. На вкладке `Главная` нажми запуск прокси
3. Нажми `Добавить proxy в Telegram`
4. Проверь в Telegram, что он включился

---

## Настройки (только если понимаешь, что делаешь)

* IP-адрес
* Порт
* DC → IP (формат `DC:IP`, по одному на строку)
* Подробное логирование

Изменения применяются после перезапуска прокси.

---

## Обновления

Проверка обновлений находится во вкладке `Настройки`.

---

# Для разработчиков

Исходный код приложения находится в ветке main

---

### Debug сборка

```powershell
.\gradlew.bat :app:assembleDebug
```

Результат:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

### Release сборка

```powershell
.\gradlew.bat :app:assembleRelease
```

---

### Подпись релиза

* Скопируй `keystore.properties.example` → `keystore.properties`
* Заполни параметры keystore

---

## Примечание

Если что-то не работает — сначала попробуй перезапустить прокси.
Большинство изменений применяется только после рестарта.
