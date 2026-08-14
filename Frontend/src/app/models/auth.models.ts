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

export interface MessageResponse {
  message: string;
}

export interface EmailRequest {
  email: string;
}

export interface OtpVerificationRequest extends EmailRequest {
  code: string;
}

export interface PasswordResetRequest extends OtpVerificationRequest {
  password: string;
}

export interface GoogleAuthConfig {
  enabled: boolean;
  clientId?: string;
}

export interface ConnectedIdentity {
  provider: 'GOOGLE';
  connectedAt: string;
}
