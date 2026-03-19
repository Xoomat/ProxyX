# ProxyX

Android-приложение для запуска локального Telegram proxy на телефоне.

[![Скачать Latest APK](https://img.shields.io/badge/Download-Latest%20APK-2ea44f?style=for-the-badge&logo=android)](https://github.com/Xoomat/ProxyX/releases/latest)
[![Latest Release](https://img.shields.io/github/v/release/Xoomat/ProxyX?style=for-the-badge)](https://github.com/Xoomat/ProxyX/releases/latest)

## Установка

1. Откройте страницу `Latest APK` (кнопка выше).
2. Скачайте файл `.apk` из последнего релиза.
3. Установите APK на ваш телефон.
4. Если телефон попросит разрешение: включите установку из браузера/файлового менеджера.

## Как пользоваться

1. Запустите `proxyX`.
2. На вкладке `Главная` нажмите кнопку запуска прокси.
3. Нажмите `Добавить proxy в телеграм`.
4. В Telegram проверьте, что прокси включен.

## Настройки (Для знающих)

Во вкладке `Настройки` можно изменить:
- IP-адрес прокси
- Порт
- DC → IP маппинги (по одному на строку, формат `DC:IP`)
- Подробное логирование

Кнопки:
- `Сохранить`
- `Сбросить настройки`

Важно: изменения применяются после перезапуска прокси.

## Обновления

- Кнопка `Проверить обновления` находится во вкладке `Настройки`.



# Для разработчиков

Исходный код Android находится в папке `android/`.

Быстрый старт:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

Debug APK:

`android/app/build/outputs/apk/debug/app-debug.apk`

Release-сборка:

```powershell
cd android
.\gradlew.bat :app:assembleRelease
```

Подпись релиза:
- Скопируйте `android/keystore.properties.example` в `android/keystore.properties`
- Укажите параметры keystore
