export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
}

export interface GoogleAuthConfig {
  enabled: boolean;
  clientId?: string;
}

export interface ConnectedIdentity {
  provider: 'GOOGLE';
  connectedAt: string;
}
