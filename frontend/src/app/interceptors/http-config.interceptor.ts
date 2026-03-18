import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class HttpConfigInterceptor implements HttpInterceptor {

  constructor(private injector: Injector) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const modifiedRequest = request.clone({ withCredentials: true });

    return next.handle(modifiedRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        // Evitar bucle: no interceptar el propio endpoint de login/logout
        const isAuthEndpoint = request.url.includes('/api/auth/login')
          || request.url.includes('/api/auth/logout');

        if ((error.status === 401 || error.status === 403) && !isAuthEndpoint) {
          sessionStorage.removeItem('currentUser');
          const router = this.injector.get(Router);
          router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }
}
