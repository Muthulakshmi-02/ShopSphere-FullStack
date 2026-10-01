
export enum Role {
  ADMIN = 'ADMIN',
  USER = 'USER'
}

export interface RegisterUserDto {
  userName: string;
  email: string;
  password: string;
  // Not sent by the register form anymore - the backend always assigns
  // USER on self-registration, regardless of what's sent here.
  role?: Role;
}

export interface LoginUserDto {
  email: string;
  password: string;
}

export interface LoginResponseDTO {
  token: string;
  userName: string;
  role: string;
}

export interface UserResponseDTO {
  userId: number;
  userName: string;
  email: string;
  role: Role;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}