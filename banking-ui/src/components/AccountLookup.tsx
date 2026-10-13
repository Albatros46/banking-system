import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import type { Account, Transaction } from '../api/types'
import { api } from '../api/client'
import type { SavedAccount } from '../hooks/useSavedAccounts'
import { formatAccountNumber, formatDate, formatMoney } from '../utils/format'
import { AccountInput, Alert, Button, Card, StatusBadge } from './ui'

const accountTypeLabels = { SAVING: 'Sparkonto', CURRENT: 'Girokonto', FIXED_DEPOSIT: 'Festgeldkonto' }

interface LookupResult {
  key: string
  account?: Account
  transactions?: Transaction[]
  error?: string
}

interface Props {
  initialAccountNumber?: string
  savedAccounts: SavedAccount[]
}

export function AccountLookup({ initialAccountNumber = '', savedAccounts }: Props) {
  const [input, setInput] = useState(initialAccountNumber)
  const [accountNumber, setAccountNumber] = useState(initialAccountNumber)
  const [reloadCount, setReloadCount] = useState(0)
  const [result, setResult] = useState<LookupResult | null>(null)

  // Her arama / yenileme icin benzersiz anahtar; sonuc bu anahtarla eslesmiyorsa yukleniyor demektir
  const requestKey = `${accountNumber}#${reloadCount}`
  const loading = accountNumber !== '' && result?.key !== requestKey

  useEffect(() => {
    if (!accountNumber) return
    let cancelled = false
    Promise.all([api.getAccount(accountNumber), api.getTransactionsByAccount(accountNumber)])
      .then(([account, transactions]) => {
        if (!cancelled) setResult({ key: requestKey, account, transactions })
      })
      .catch((err: Error) => {
        if (!cancelled) setResult({ key: requestKey, error: err.message })
      })
    return () => {
      cancelled = true
    }
  }, [accountNumber, requestKey])

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    setAccountNumber(input.trim())
    setReloadCount((c) => c + 1)
  }

  const shown = result?.key === requestKey ? result : null

  return (
    <div className="space-y-6">
      <Card title="Kontoabfrage">
        <form onSubmit={handleSearch} className="flex gap-3">
          <div className="flex-1">
            <AccountInput id="lookup" value={input} onChange={setInput} savedAccounts={savedAccounts} />
          </div>
          <Button type="submit" disabled={loading}>
            {loading ? 'Wird geladen...' : 'Abfragen'}
          </Button>
        </form>
      </Card>

      {shown?.error && <Alert kind="error">{shown.error}</Alert>}

      {shown?.account && (
        <>
          <Card
            title={shown.account.accountHolderName}
            actions={
              <Button variant="secondary" onClick={() => setReloadCount((c) => c + 1)} disabled={loading}>
                Aktualisieren
              </Button>
            }
          >
            <dl className="grid gap-4 sm:grid-cols-3">
              <Info label="Kontonummer" value={<span className="font-mono">{formatAccountNumber(shown.account.accountNumber)}</span>} />
              <Info label="Kontostand" value={<span className="text-xl font-semibold">{formatMoney(shown.account.balance)}</span>} />
              <Info label="Status" value={<StatusBadge status={shown.account.status} />} />
              <Info label="Kontoart" value={accountTypeLabels[shown.account.accountType]} />
              <Info label="Tageslimit" value={formatMoney(shown.account.dailyTransactionLimit)} />
              <Info label="E-Mail" value={shown.account.email} />
            </dl>
          </Card>

          <Card title="Kontoumsätze">
            {shown.transactions?.length ? (
              <TransactionTable transactions={shown.transactions} accountNumber={shown.account.accountNumber} />
            ) : (
              <p className="text-sm text-slate-500">Noch keine Umsätze.</p>
            )}
          </Card>
        </>
      )}
    </div>
  )
}

function Info({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm text-slate-900">{value}</dd>
    </div>
  )
}

function TransactionTable({ transactions, accountNumber }: { transactions: Transaction[]; accountNumber: string }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="py-2 pr-4">Datum</th>
            <th className="py-2 pr-4">Gegenkonto</th>
            <th className="py-2 pr-4 text-right">Betrag</th>
            <th className="py-2 pr-4">Status</th>
            <th className="py-2">Hinweis</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {transactions.map((tx) => {
            const outgoing = tx.fromAccountNumber === accountNumber
            // Basarisiz transferde para iade edildigi icin bakiyeye etkisi yoktur
            const amountClass = tx.status === 'FAILED' ? 'text-slate-400 line-through' : outgoing ? 'text-red-600' : 'text-emerald-600'
            return (
              <tr key={tx.id}>
                <td className="py-2 pr-4 whitespace-nowrap text-slate-600">{formatDate(tx.createdAt)}</td>
                <td className="py-2 pr-4 font-mono">
                  {outgoing ? '→ ' : '← '}
                  {formatAccountNumber(outgoing ? tx.toAccountNumber : tx.fromAccountNumber)}
                </td>
                <td className={`py-2 pr-4 text-right font-medium whitespace-nowrap ${amountClass}`}>
                  {outgoing ? '-' : '+'}
                  {formatMoney(tx.amount)}
                </td>
                <td className="py-2 pr-4">
                  <StatusBadge status={tx.status} />
                </td>
                <td className="py-2 text-slate-600">{tx.failureReason ?? ''}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
