import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { Account, AccountType } from '../api/types'
import type { SavedAccount } from '../hooks/useSavedAccounts'
import { formatAccountNumber, formatMoney } from '../utils/format'
import { Alert, Button, Card, Field, Input, Select } from './ui'

const accountTypes: { value: AccountType; label: string }[] = [
  { value: 'SAVING', label: 'Sparkonto (Tageslimit 100.000 €)' },
  { value: 'CURRENT', label: 'Girokonto (Tageslimit 200.000 €)' },
  { value: 'FIXED_DEPOSIT', label: 'Festgeldkonto (Tageslimit 200.000 €)' },
]

const emptyForm = {
  accountHolderName: '',
  email: '',
  phone: '',
  accountType: 'SAVING' as AccountType,
  initialDeposit: '',
}

interface Props {
  onCreated: (account: SavedAccount) => void
  onShowAccount: (accountNumber: string) => void
}

export function CreateAccount({ onCreated, onShowAccount }: Props) {
  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [created, setCreated] = useState<Account | null>(null)

  const update = (field: keyof typeof form) => (value: string) => setForm((f) => ({ ...f, [field]: value }))

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})
    try {
      const account = await api.createAccount({ ...form, initialDeposit: Number(form.initialDeposit) })
      setCreated(account)
      setForm(emptyForm)
      onCreated({ accountNumber: account.accountNumber, accountHolderName: account.accountHolderName })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(err.fieldErrors)
      } else {
        setError('Ein unerwarteter Fehler ist aufgetreten.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      {created && (
        <Alert kind="success">
          <p className="font-medium">Konto eröffnet: {created.accountHolderName}</p>
          <p className="mt-1">
            Kontonr.: <span className="font-mono">{formatAccountNumber(created.accountNumber)}</span> - Kontostand:{' '}
            {formatMoney(created.balance)}
          </p>
          <button className="mt-2 font-medium underline" onClick={() => onShowAccount(created.accountNumber)}>
            Konto anzeigen
          </button>
        </Alert>
      )}

      <Card title="Neues Konto eröffnen">
        <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
          <Field label="Vor- und Nachname" error={fieldErrors.accountHolderName}>
            <Input value={form.accountHolderName} onChange={(e) => update('accountHolderName')(e.target.value)} required />
          </Field>
          <Field label="E-Mail" error={fieldErrors.email}>
            <Input type="email" value={form.email} onChange={(e) => update('email')(e.target.value)} required />
          </Field>
          <Field label="Telefonnummer" error={fieldErrors.phone}>
            <Input type="tel" value={form.phone} onChange={(e) => update('phone')(e.target.value)} required />
          </Field>
          <Field label="Kontoart" error={fieldErrors.accountType}>
            <Select value={form.accountType} onChange={(e) => update('accountType')(e.target.value)}>
              {accountTypes.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Ersteinzahlung (€)" error={fieldErrors.initialDeposit}>
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.initialDeposit}
              onChange={(e) => update('initialDeposit')(e.target.value)}
              required
            />
          </Field>

          {error && (
            <div className="sm:col-span-2">
              <Alert kind="error">{error}</Alert>
            </div>
          )}
          <div className="sm:col-span-2">
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Wird eröffnet...' : 'Konto eröffnen'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
