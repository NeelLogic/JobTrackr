import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'jobtrackr.auth';
  private readonly state = signal<AuthResponse | null>(this.readSession());

  readonly user = computed<User | null>(() => this.state()?.user ?? null);
  readonly isAuthenticated = computed(() => Boolean(this.state()?.token));

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.persist(response)));
  }

  register(request: RegisterRequest) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap((response) => this.persist(response)));
  }

  token(): string | null {
    return this.state()?.token ?? null;
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.state.set(null);
  }

  private persist(response: AuthResponse): void {
    localStorage.setItem(this.storageKey, JSON.stringify(response));
    this.state.set(response);
  }

  private readSession(): AuthResponse | null {
    try {
      const value = localStorage.getItem(this.storageKey);
      return value ? (JSON.parse(value) as AuthResponse) : null;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
