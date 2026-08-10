// The authenticated user, as returned by /auth/login and /auth/me.
export default interface AuthUser {
  id: number
  name: string
  email: string
  role: string
  permissions: string[]
  impersonating: boolean
  impersonatorEmail?: string | null
}
