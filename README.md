# ProxyX (Clean Source Export)

Автор: **Xoomat**

Это очищенная версия Android-исходников без кастомного интерфейса (минимальный UI только для запуска/остановки сервиса), чтобы разработчики могли собрать и доработать приложение под себя.

## Что внутри

- Android-проект с Chaquopy
- Kotlin-сервис `ProxyService` для запуска Python proxy
- Python-ядро в `app/src/main/python`
- Минимальный `MainActivity` без дизайнерских экранов

## Что убрано

- build-кэш, `.gradle`, `app/build`, IDE-файлы
- лишние ресурсы и дизайн-ассеты UI
- локальные секреты (`local.properties`, `keystore.properties`, keystore-файлы)

## Сборка

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

APK:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Release-подпись

1. Скопировать `keystore.properties.example` в `keystore.properties`
2. Заполнить параметры keystore
3. Запустить:

```powershell
cd android
.\gradlew.bat :app:assembleRelease
```
