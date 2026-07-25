# 📅 CalendarApp

Una aplicación de calendario **premium para Android** construida con Jetpack Compose y Material 3. Diseñada con un enfoque en personalización visual, rendimiento y experiencia de usuario.

---

## ✨ Características

### 📆 Vistas de Calendario
- **Vista mensual** con indicadores de calor (heat map) según cantidad de eventos
- **Vista semanal** con navegación horizontal
- **Agenda** con búsqueda en tiempo real, agrupación por semana/mes y sección "Hoy" fija
- Soporte para eventos de todo el día, recurrentes y con hora de fin

### 🎨 Tema Totalmente Personalizable
- **ThemeBuilder** con más de 20 parámetros ajustables en tiempo real
- Color de acento, texto, fondo, días de fin de semana
- 8 tipografías distintas (Minimal, Elegant, Geometric, Playful, Rounded, Thin, Heavy, Mono)
- 5 estilos de borde (0dp a completamente redondeado)
- Fondos de imagen por mes o global con soporte EXIF
- Escala de texto global
- Modo claro/oscuro automático

### 🗓️ Gestión de Eventos
- Crear, editar y eliminar eventos con animación
- Recurrencia: diaria, semanal, mensual, anual (con fix de año bisiesto)
- Notificaciones configurables (5 min, 15 min, 30 min, 1h antes)
- Ubicación y URL por evento
- Foto adjunta al evento
- Detección de conflictos de horario
- Swipe para eliminar con opción "Deshacer"

### 🔔 Notificaciones
- Recordatorios programados con `AlarmManager`
- Acciones rápidas: Posponer 10 min / Descartar
- Canal de notificaciones dedicado

### 🏠 Widgets para el Escritorio (Glance)
- **Today Widget** — fecha actual + próximo evento
- **Week Widget** — semana actual con indicadores de eventos
- **Month Widget** — mes completo con heat map
- **Upcoming Events Widget** — próximos 5 eventos
- Todos respetan el tema (colores de acento y texto)
- Actualización automática a medianoche via `AlarmManager`

### 📤 Importar / Exportar
- **ICS** — compatible con Google Calendar, Apple Calendar, Outlook
- **CSV** — para hojas de cálculo
- **Backup/Restore** de la base de datos completa (SQLite)
- Importación de cumpleaños desde Contactos del dispositivo
- Sincronización con Google Calendar

### 🌐 Bilingüe
- Español e Inglés (seleccionable en Ajustes)
- Detección automática del idioma del sistema

---

## 🛠️ Stack Tecnológico

| Categoría | Tecnología |
|-----------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Arquitectura** | MVVM + Repository Pattern |
| **Base de Datos** | Room (SQLite) con KSP |
| **Asincronía** | Kotlin Coroutines + Flow |
| **Navegación** | Navigation Compose |
| **Widgets** | Glance AppWidget 1.1.0 |
| **Imágenes** | Coil 2.6.0 |
| **Calendario** | Kizitonwose Calendar Compose 2.6.0 |
| **Red** | Retrofit 2.9.0 + OkHttp 4.12.0 |
| **Auth** | Google Sign-In (Play Services 21.2.0) |
| **Fuentes** | Google Fonts via Compose |

---

## 📋 Requisitos

- **Android mínimo**: API 24 (Android 7.0 Nougat)
- **Android objetivo**: API 36
- **Kotlin**: 1.9+
- **Android Studio**: Hedgehog o superior
- **JDK**: 17+

---

## 🚀 Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/CalendarApp.git
cd CalendarApp
```

### 2. Abrir en Android Studio
Abre el proyecto con **File → Open** en Android Studio.

### 3. Configurar Google Services (opcional)
Si quieres usar sincronización con Google Calendar:
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un proyecto y descarga `google-services.json`
3. Colócalo en `app/`

> Sin `google-services.json`, la app compila igualmente pero la sincronización de Google Calendar no estará disponible.

### 4. Compilar y ejecutar
```bash
./gradlew assembleDebug
```
O usa el botón **Run** en Android Studio.

---

## 🏗️ Estructura del Proyecto

```
app/src/main/java/com/example/calendarapp/
├── MainActivity.kt                  # Punto de entrada, navegación principal
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Configuración de Room
│   │   ├── EventDao.kt              # Queries SQL
│   │   └── EventRepository.kt       # Repositorio de datos
│   └── model/
│       └── CalendarEvent.kt         # Modelo de evento
├── notifications/
│   └── EventNotificationReceiver.kt # Receptor de alarmas y notificaciones
├── ui/
│   ├── components/
│   │   ├── BottomNavigationBar.kt   # Barra de navegación inferior
│   │   ├── CalendarDay.kt           # Celda de día del calendario
│   │   ├── EventDetailSheet.kt      # Hoja de detalles del evento
│   │   └── EventDialog.kt           # Diálogo crear/editar evento
│   ├── screens/
│   │   ├── AgendaScreen.kt          # Vista de agenda con búsqueda
│   │   ├── CalendarScreen.kt        # Vista principal del calendario
│   │   ├── SettingsScreen.kt        # Ajustes de la app
│   │   └── ThemeBuilderScreen.kt    # Constructor de tema visual
│   ├── theme/                       # Tema Compose base
│   └── viewmodel/
│       ├── CalendarViewModel.kt     # Lógica de eventos y recurrencia
│       ├── SettingsViewModel.kt     # Preferencias de usuario
│       └── ThemeViewModel.kt        # Estado del tema en tiempo real
├── utils/
│   ├── AppStrings.kt                # Strings bilingüe (ES/EN)
│   ├── DataManager.kt               # Importar/exportar ICS, CSV, backup
│   ├── GoogleCalendarSync.kt        # Sincronización Google Calendar
│   └── HolidayProvider.kt          # Festivos por país
└── widget/
    ├── CalendarWidget.kt            # Los 4 widgets (Today, Week, Month, Upcoming)
    └── WidgetDailyUpdater.kt        # Alarma de actualización a medianoche
```

---

## ⚙️ Permisos Requeridos

| Permiso | Uso |
|---------|-----|
| `RECEIVE_BOOT_COMPLETED` | Reprogramar alarmas tras reinicio |
| `SCHEDULE_EXACT_ALARM` | Notificaciones exactas |
| `USE_EXACT_ALARM` | Alternativa a `SCHEDULE_EXACT_ALARM` |
| `POST_NOTIFICATIONS` | Mostrar notificaciones (API 33+) |
| `READ_CONTACTS` | Importar cumpleaños |
| `READ_CALENDAR` | Sincronización con Google Calendar |

---

## 🤝 Contribuir

1. Haz fork del repositorio
2. Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. Haz commit: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Ver [LICENSE](LICENSE) para más detalles.

---

<div align="center">
Hecho con ❤️ usando Jetpack Compose
</div>