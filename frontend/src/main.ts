import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi,HTTP_INTERCEPTORS,  } from '@angular/common/http';
import { routes } from './app/app.routes';
import { HttpConfigInterceptor } from './app/interceptors/http-config.interceptor';
import { environment } from './environments/environment';
import { AppLogger } from './app/services/app-logger.service';

try {
  await bootstrapApplication(AppComponent, {
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptorsFromDi()),
      {
        provide: HTTP_INTERCEPTORS,
        useClass: HttpConfigInterceptor,
        multi: true
      }
    ]
  });
} catch {
  if (!environment.production) {
    AppLogger.error('Error al iniciar la aplicacion.');
  }
}
