/**
 * Configuración de producción (`ng build`).
 * apiUrl: base de la API. Con '/api' el servidor que publica el frontend (nginx, ver
 * backend/docs/jobready-nginx.conf) reenvía las peticiones al backend. Si el backend está en
 * otro dominio, usa su URL completa, p. ej. 'https://api.midominio.com/api'.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
