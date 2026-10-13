import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'
import type { AccountStatus, TransactionStatus } from '../api/types'
import type { SavedAccount } from '../hooks/useSavedAccounts'
import { formatAccountNumber } from '../utils/format'

export function Card({ title, children, actions }: { title: string; children: ReactNode; actions?: ReactNode }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        {actions}
      </div>
      {children}
    </section>
  )
}

interface FieldProps {
  label: string
  error?: string
  children: ReactNode
}

export function Field({ label, error, children }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      {children}
      {error && <span className="mt-1 block text-sm text-red-600">{error}</span>}
    </label>
  )
}

const inputClass =
  'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100'

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={inputClass} />
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={inputClass} />
}

export function Button({ variant = 'primary', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' }) {
  const styles =
    variant === 'primary'
      ? 'bg-indigo-600 text-white hover:bg-indigo-700'
      : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
  return (
    <button
      {...props}
      className={`rounded-lg px-4 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${styles}`}
    />
  )
}

export function Alert({ kind, children }: { kind: 'error' | 'success' | 'info'; children: ReactNode }) {
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-800',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    info: 'border-sky-200 bg-sky-50 text-sky-800',
  }[kind]
  return <div className={`rounded-lg border px-4 py-3 text-sm ${styles}`}>{children}</div>
}

const statusStyles: Record<AccountStatus | TransactionStatus, string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-800',
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  PENDING: 'bg-amber-100 text-amber-800',
  BLOCKED: 'bg-red-100 text-red-800',
  FAILED: 'bg-red-100 text-red-800',
  CLOSED: 'bg-slate-200 text-slate-700',
}

const statusLabels: Record<AccountStatus | TransactionStatus, string> = {
  ACTIVE: 'Aktiv',
  BLOCKED: 'Gesperrt',
  CLOSED: 'Geschlossen',
  PENDING: 'In Bearbeitung',
  COMPLETED: 'Abgeschlossen',
  FAILED: 'Fehlgeschlagen',
}

export function StatusBadge({ status }: { status: AccountStatus | TransactionStatus }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${statusStyles[status]}`}>
      {status === 'PENDING' && <span className="size-2 animate-pulse rounded-full bg-amber-500" />}
      {statusLabels[status]}
    </span>
  )
}

// Hesap numarasi girisi + bu tarayicida acilan hesaplardan secim
export function AccountInput({
  id,
  value,
  onChange,
  savedAccounts,
}: {
  id: string
  value: string
  onChange: (value: string) => void
  savedAccounts: SavedAccount[]
}) {
  return (
    <>
      <Input
        list={`${id}-list`}
        value={value}
        onChange={(e) => onChange(e.target.value.replace(/\s/g, ''))}
        placeholder="12-stellige Kontonummer"
        inputMode="numeric"
        required
      />
      <datalist id={`${id}-list`}>
        {savedAccounts.map((account) => (
          <option key={account.accountNumber} value={account.accountNumber}>
            {account.accountHolderName} - {formatAccountNumber(account.accountNumber)}
          </option>
        ))}
      </datalist>
    </>
  )
}
