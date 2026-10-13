// Backend DTO'larinin karsiliklari

export type AccountType = 'SAVING' | 'CURRENT' | 'FIXED_DEPOSIT'
export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED'
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface Account {
  id: string
  accountNumber: string
  accountHolderName: string
  email: string
  phone: string
  accountType: AccountType
  status: AccountStatus
  balance: number
  dailyTransactionLimit: number
  createdAt: string | null
}

export interface CreateAccountRequest {
  accountHolderName: string
  email: string
  phone: string
  accountType: AccountType
  initialDeposit: number
}

export interface Transaction {
  id: string
  fromAccountNumber: string
  toAccountNumber: string
  amount: number
  status: TransactionStatus
  failureReason: string | null
  createdAt: string
  updatedAt: string
}

export interface TransferRequest {
  fromAccountNumber: string
  toAccountNumber: string
  amount: number
}
