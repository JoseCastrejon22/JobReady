/**
 * Configuración de desarrollo (`ng serve`). Reemplaza a environment.ts según angular.json.
 * apiUrl: con '/api' el proxy de Angular (proxy.conf.json) reenvía las peticiones a http://localhost:8080.
 */
export const environment = {
  production: false,
  apiUrl: '/api',
};
