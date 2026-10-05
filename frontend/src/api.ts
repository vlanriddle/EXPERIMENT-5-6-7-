import axios, { type InternalAxiosRequestConfig } from 'axios'

export type PostStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED'

export interface ApiResponse<T> {
  timestamp: string
  status: number
  message: string
  data: T
  errors: string[]
}

export interface AuthUser {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  userId: string
  email: string
  displayName: string
}

export interface PostItem {
  id: string
  content: string
  mediaUrl: string | null
  status: PostStatus
  scheduledAt: string | null
  authorId: string
  authorName: string
  createdAt: string
  updatedAt: string
}

export interface PostPage {
  content: PostItem[]
  totalElements: number
  totalPages: number
  pageNumber: number
  pageSize: number
  last: boolean
}

export interface PostInput {
  content: string
  mediaUrl: string | null
  status: PostStatus
  scheduledAt: string | null
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
})

let accessToken: string | null = null
let refreshPromise: Promise<string | null> | null = null
let sessionRefreshPromise: Promise<AuthUser> | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  config.headers.set('X-Correlation-ID', crypto.randomUUID())
  if (accessToken) config.headers.set('Authorization', `Bearer ${accessToken}`)
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) return Promise.reject(error)
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    if (!original || error.response?.status !== 401 || original._retry || original.url?.includes('/auth/')) {
      return Promise.reject(error)
    }

    original._retry = true
    refreshPromise ??= axios
      .post<ApiResponse<AuthUser>>(`${http.defaults.baseURL}/auth/refresh`, {}, { withCredentials: true })
      .then((response) => {
        const token = response.data.data.accessToken
        setAccessToken(token)
        return token
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshPromise = null
      })

    const refreshedToken = await refreshPromise
    if (!refreshedToken) return Promise.reject(error)
    original.headers.set('Authorization', `Bearer ${refreshedToken}`)
    return http(original)
  },
)

async function data<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  return (await request).data.data
}

export const authApi = {
  login: (email: string, password: string) =>
    data(http.post<ApiResponse<AuthUser>>('/auth/login', { email, password })),
  register: (email: string, displayName: string, password: string) =>
    data(http.post<ApiResponse<AuthUser>>('/auth/register', { email, displayName, password })),
  refresh: () => {
    sessionRefreshPromise ??= data(http.post<ApiResponse<AuthUser>>('/auth/refresh'))
      .then((session) => {
        setAccessToken(session.accessToken)
        return session
      })
      .finally(() => { sessionRefreshPromise = null })
    return sessionRefreshPromise
  },
  logout: () => data(http.post<ApiResponse<null>>('/auth/logout')),
}

export const postsApi = {
  list: (params: { page: number; size: number; status?: PostStatus; sortBy?: string; sortDir?: 'asc' | 'desc' }) =>
    data(http.get<ApiResponse<PostPage>>('/posts', { params })),
  trending: () => data(http.get<ApiResponse<PostItem[]>>('/posts/trending')),
  create: (input: PostInput) => data(http.post<ApiResponse<PostItem>>('/posts', input)),
  update: (id: string, input: PostInput) => data(http.put<ApiResponse<PostItem>>(`/posts/${id}`, input)),
  remove: (id: string) => data(http.delete<ApiResponse<null>>(`/posts/${id}`)),
}

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data.errors?.[0] ?? error.response?.data.message ?? 'The request could not be completed.'
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}
