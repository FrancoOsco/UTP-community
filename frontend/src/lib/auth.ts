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

let pendingRefresh: Promise<string | null> | null = null;

export async function refresh(): Promise<string | null> {
  // Varios componentes pueden iniciar al mismo tiempo: un único refresh
  // evita consumir dos veces el token rotativo del servidor.
  if (pendingRefresh) return pendingRefresh;
  pendingRefresh = (async () => {
    try {
      const response = await apiFetch('/api/auth/refresh', { method: 'POST' });
      if (!response.ok) { accessToken = null; return null; }
      const data = await response.json();
      accessToken = data.token;
      return accessToken;
    } catch {
      accessToken = null;
      return null;
    }
  })();
  try { return await pendingRefresh; }
  finally { pendingRefresh = null; }
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
