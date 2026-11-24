# JobReady

Aplicación para la preparación de entrevistas profesionales. Este repositorio reúne el backend Spring Boot y el frontend Angular.

```text
JobReady/
  backend/                 API en Java y Spring Boot, scripts de base de datos y documentación
  frontend/                Aplicación Angular y su documentación
  .github/workflows/       Publicación del frontend en GitHub Pages
```

## Para trabajar en equipo

Clona este repositorio una vez. Abre `backend/` en IntelliJ IDEA y `frontend/` en WebStorm, o usa el editor que prefieras. Ambos directorios pertenecen al mismo repositorio Git: realiza los cambios desde la raíz y coordina con tu equipo el uso de ramas y revisiones.

### Frontend

Necesitas Node.js 24 y npm. En la terminal, desde `frontend/`:

```powershell
npm ci
npm start
```

Abre `http://localhost:4200/`. La landing lleva al banco de preguntas; también están las pantallas de ingreso y registro. Los servicios Angular usan `/api`; el proxy local reenvía esas solicitudes a `http://localhost:8080` para desarrollar sin CORS en el navegador. Encontrarás más detalles en [frontend/README.md](frontend/README.md).

### Backend

Necesitas Java 21, Maven o el Maven Wrapper y una base de datos PostgreSQL accesible. Usa [application-example.properties](backend/src/main/resources/application-example.properties) como referencia para configurar la conexión y las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `FRONTEND_URL` en tu entorno local. No publiques contraseñas ni el archivo local `application.properties`.

Desde `backend/`, ejecuta `./mvnw spring-boot:run` en macOS/Linux o `.\mvnw.cmd spring-boot:run` en PowerShell. El frontend llama a los endpoints de autenticación y preguntas. En desarrollo, el proxy evita una llamada directa entre orígenes. En producción, cuando se despliegue la API en HTTPS, configura `FRONTEND_URL` con el origen de GitHub Pages y actualiza la URL base de la API en Angular.

### US11 · Recuperación de contraseña

- **Frontend.** El enlace **Olvidé mi contraseña** abre el formulario de recuperación. El enlace recibido por correo abre el formulario **Nueva contraseña**.
- **API.** `POST /api/auth/recuperar-password` solicita el envío al correo registrado. `POST /api/auth/restablecer-password` valida el token y guarda la nueva contraseña con BCrypt.
- **Base de datos.** La tabla `tokens_recuperacion` guarda el hash del token, su vencimiento de 15 minutos y su estado de uso. Una nueva solicitud reemplaza el enlace anterior.

Para enviar los enlaces de recuperación, incorpora la sección de correo de `application-example.properties` a tu configuración y define `SMTP_HOST`, `SMTP_USERNAME` y `SMTP_PASSWORD`. El ejemplo usa el puerto 587 con STARTTLS y el usuario SMTP como remitente. Usa `SMTP_FROM` si tu proveedor requiere otro remitente autorizado. Define `RESET_FRONTEND_URL=http://localhost:4200` para el desarrollo con `npm start`. En producción usa la URL pública completa del frontend, incluido el subdirectorio `/JobReady/` si corresponde.

## Publicación

GitHub Actions compila y publica el frontend cuando se actualiza `main`, si el origen de GitHub Pages está configurado como **GitHub Actions**. El sitio previsto es https://josespgclan-ops.github.io/JobReady/. GitHub Pages solo aloja el frontend; el backend y la base de datos necesitan ejecutarse o desplegarse por separado.

Los historiales de frontend y backend se incorporaron al historial de este repositorio unificado.
