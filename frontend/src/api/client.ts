export interface ApiError {
  code: string
  message: string
  details: string[]
}

export class ApiRequestError extends Error {
  status: number
  apiError: ApiError

  constructor(status: number, apiError: ApiError) {
    super(apiError.message)
    this.status = status
    this.apiError = apiError
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('Accept', 'application/json')
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (response.status === 204) {
    return undefined as T
  }

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    const apiError: ApiError = body ?? {
      code: 'UNKNOWN_ERROR',
      message: `Erro inesperado (HTTP ${response.status}).`,
      details: [],
    }
    throw new ApiRequestError(response.status, apiError)
  }

  return body as T
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
}
