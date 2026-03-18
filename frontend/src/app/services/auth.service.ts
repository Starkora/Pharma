import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Usuario, LoginRequest, LoginResponse } from '../models/auth.model';

interface ValidateSessionResponse {
  success: boolean;
  message?: string;
  usuario?: Usuario;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject: BehaviorSubject<Usuario | null>;
  public currentUser: Observable<Usuario | null>;

  constructor(private http: HttpClient) {
    // Intentar cargar usuario desde sessionStorage (más seguro que localStorage)
    const storedUser = sessionStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<Usuario | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): Usuario | null {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    const loginRequest: LoginRequest = { username, password };
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, loginRequest, { withCredentials: true })
      .pipe(
        tap(response => {
          if (response.success && response.usuario) {
            this.setCurrentUser(response.usuario);
          }
        })
      );
  }

  validateSession(): Observable<Usuario | null> {
    if (!this.currentUserSubject.value) {
      return of(null);
    }

    return this.http.get<ValidateSessionResponse>(`${this.apiUrl}/validate`, { withCredentials: true })
      .pipe(
        map(response => response.usuario ?? null),
        tap(usuario => {
          if (usuario) {
            this.setCurrentUser(usuario);
            return;
          }

          this.clearSession();
        }),
        catchError(() => {
          this.clearSession();
          return of(null);
        })
      );
  }

  logout(): Observable<any> {
    this.clearSession();
    return this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true });
  }

  clearSession(): void {
    sessionStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  getCurrentUser(): Usuario | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: string): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.includes(role) : false;
  }

  private setCurrentUser(usuario: Usuario): void {
    sessionStorage.setItem('currentUser', JSON.stringify(usuario));
    this.currentUserSubject.next(usuario);
  }
}
