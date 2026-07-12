package com.hiresplayer.cloud.yandex

import java.io.IOException

/** Сигнал для интерфейса о необходимости повторной авторизации без аварийного завершения. */
class YandexAuthenticationRequiredException : IOException(
    "Требуется повторная авторизация в Яндекс.Диске"
)
