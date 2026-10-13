import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { Transaction } from '../api/types'
import type { SavedAccount } from '../hooks/useSavedAccounts'
import { formatAccountNumber, formatMoney } from '../utils/format'
import { AccountInput, Alert, Button, Card, Field, Input, StatusBadge } from './ui'

// Transfer asenkron tamamlanir (SAGA). PENDING oldugu surece her saniye durumu sorgula.
const POLL_INTERVAL_MS = 1000
const MAX_POLLS = 20

interface Props {
  savedAccounts: SavedAccount[]
  onShowAccount: (accountNumber: string) => void
}

export function Transfer({ savedAccounts, onShowAccount }: Props) {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [tracked, setTracked] = useState<{ transaction: Transaction; polls: number } | null>(null)

  useEffect(() => {
    if (!tracked || tracked.transaction.status !== 'PENDING' || tracked.polls >= MAX_POLLS) return
    let cancelled = false
    const timer = setTimeout(() => {
      api
        .getTransaction(tracked.transaction.id)
        .then((transaction) => {
          if (!cancelled) setTracked({ transaction, polls: tracked.polls + 1 })
        })
        .catch(() => {
          if (!cancelled) setTracked({ ...tracked, polls: tracked.polls + 1 })
        })
    }, POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [tracked])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})
    setTracked(null)
    try {
      const transaction = await api.transfer({ fromAccountNumber: from, toAccountNumber: to, amount: Number(amount) })
      setTracked({ transaction, polls: 0 })
      setAmount('')
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
      <Card title="Überweisung">
        <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
          <Field label="Auftraggeberkonto" error={fieldErrors.fromAccountNumber}>
            <AccountInput id="from" value={from} onChange={setFrom} savedAccounts={savedAccounts} />
          </Field>
          <Field label="Empfängerkonto" error={fieldErrors.toAccountNumber}>
            <AccountInput id="to" value={to} onChange={setTo} savedAccounts={savedAccounts} />
          </Field>
          <Field label="Betrag (€)" error={fieldErrors.amount}>
            <Input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          </Field>

          {error && (
            <div className="sm:col-span-2">
              <Alert kind="error">{error}</Alert>
            </div>
          )}
          <div className="sm:col-span-2">
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Wird gesendet...' : 'Überweisen'}
            </Button>
          </div>
        </form>
      </Card>

      {tracked && <TransferStatus {...tracked} onShowAccount={onShowAccount} />}
    </div>
  )
}

function TransferStatus({
  transaction,
  polls,
  onShowAccount,
}: {
  transaction: Transaction
  polls: number
  onShowAccount: (accountNumber: string) => void
}) {
  const steps = [
    { label: 'Vom Auftraggeberkonto abgebucht', done: true },
    { label: 'Betrugsprüfung', done: transaction.status !== 'PENDING' },
    {
      label: transaction.status === 'FAILED' ? 'Betrag dem Auftraggeber zurückerstattet' : 'Dem Empfängerkonto gutgeschrieben',
      done: transaction.status !== 'PENDING',
    },
  ]

  return (
    <Card title="Überweisungsstatus" actions={<StatusBadge status={transaction.status} />}>
      <p className="text-sm text-slate-600">
        <span className="font-mono">{formatAccountNumber(transaction.fromAccountNumber)}</span> →{' '}
        <span className="font-mono">{formatAccountNumber(transaction.toAccountNumber)}</span> :{' '}
        <span className="font-semibold text-slate-900">{formatMoney(transaction.amount)}</span>
      </p>

      <ol className="mt-4 space-y-2">
        {steps.map((step) => (
          <li key={step.label} className="flex items-center gap-2 text-sm">
            <span
              className={`flex size-5 items-center justify-center rounded-full text-xs ${
                step.done ? 'bg-emerald-500 text-white' : 'bg-slate-200 text-slate-500'
              }`}
            >
              {step.done ? '✓' : '…'}
            </span>
            {step.label}
          </li>
        ))}
      </ol>

      <div className="mt-4 space-y-3">
        {transaction.status === 'COMPLETED' && <Alert kind="success">Überweisung erfolgreich abgeschlossen.</Alert>}
        {transaction.status === 'FAILED' && <Alert kind="error">Überweisung fehlgeschlagen: {transaction.failureReason}</Alert>}
        {transaction.status === 'PENDING' && polls >= MAX_POLLS && (
          <Alert kind="info">Die Überweisung wird noch bearbeitet. Den Status können Sie später in den Kontoumsätzen prüfen.</Alert>
        )}
        <p className="text-xs text-slate-400">Transaktionsnr.: {transaction.id}</p>
        <Button variant="secondary" onClick={() => onShowAccount(transaction.fromAccountNumber)}>
          Auftraggeberkonto anzeigen
        </Button>
      </div>
    </Card>
  )
}
