export const API_URL = import.meta.env.PUBLIC_API_URL ?? 'http://localhost:8080';

export async function apiFetch(path: string, options: RequestInit = {}) {
  return fetch(`${API_URL}${path}`, {
    ...options,
    credentials: 'include',
  });
}
