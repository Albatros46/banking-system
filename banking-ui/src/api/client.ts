import type { Account, CreateAccountRequest, Transaction, TransferRequest } from './types'

const BASE_URL = '/api/v1'

// Backend hatalari ProblemDetail formatinda gelir: { status, detail, errors? }
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(BASE_URL + path, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new ApiError(0, 'Server nicht erreichbar. Läuft das API-Gateway?')
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message =
      body?.detail ??
      (response.status === 404 ? 'Eintrag nicht gefunden.' : `Vorgang fehlgeschlagen (HTTP ${response.status}).`)
    throw new ApiError(response.status, message, body?.errors ?? {})
  }
  return response.json() as Promise<T>
}

export const api = {
  createAccount: (data: CreateAccountRequest) =>
    request<Account>('/accounts', { method: 'POST', body: JSON.stringify(data) }),

  getAccount: (accountNumber: string) =>
    request<Account>(`/accounts/${encodeURIComponent(accountNumber)}`),

  transfer: (data: TransferRequest) =>
    request<Transaction>('/transactions/transfer', { method: 'POST', body: JSON.stringify(data) }),

  getTransaction: (id: string) =>
    request<Transaction>(`/transactions/${encodeURIComponent(id)}`),

  getTransactionsByAccount: (accountNumber: string) =>
    request<Transaction[]>(`/transactions/account/${encodeURIComponent(accountNumber)}`),
}
