import { apiFetch } from './api';

let accessToken: string | null = null;

export function getAccessToken() {
  return accessToken;
}

export async function login(email: string, password: string) {
  const response = await apiFetch('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      email,
      password,
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message ?? 'No se pudo iniciar sesión.');
  }

  accessToken = data.token;

  return accessToken;
}

export async function refresh() {
  const response = await apiFetch('/api/auth/refresh', {
    method: 'POST',
  });

  if (!response.ok) {
    accessToken = null;
    return null;
  }

  const data = await response.json();

  accessToken = data.token;

  return accessToken;
}

export async function logout() {
  await apiFetch('/api/auth/logout', {
    method: 'POST',
  });

  accessToken = null;
}

export async function authFetch(path: string, options: RequestInit = {}) {
  if (!accessToken) {
    await refresh();
  }

  if (!accessToken) {
    return null;
  }

  let response = await apiFetch(path, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (response.status === 401) {
    const nuevoToken = await refresh();

    if (!nuevoToken) {
      return null;
    }

    response = await apiFetch(path, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Bearer ${nuevoToken}`,
      },
    });
  }

  return response;
}

export async function esAdmin() {
  const response = await authFetch('/api/usuarios');

  return response?.status === 200;
}
