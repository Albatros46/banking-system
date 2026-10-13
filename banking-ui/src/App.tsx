import { useState } from 'react'
import { AccountLookup } from './components/AccountLookup'
import { CreateAccount } from './components/CreateAccount'
import { Transfer } from './components/Transfer'
import { useSavedAccounts } from './hooks/useSavedAccounts'

type Tab = 'transfer' | 'lookup' | 'create'

const tabs: { id: Tab; label: string }[] = [
  { id: 'transfer', label: 'Überweisung' },
  { id: 'lookup', label: 'Kontoabfrage' },
  { id: 'create', label: 'Konto eröffnen' },
]

function App() {
  const [tab, setTab] = useState<Tab>('transfer')
  // Baska ekrandan "hesabi goruntule" ile gelindiginde sorgulanacak hesap
  const [lookup, setLookup] = useState({ accountNumber: '', version: 0 })
  const { accounts, addAccount } = useSavedAccounts()

  function showAccount(accountNumber: string) {
    setLookup((current) => ({ accountNumber, version: current.version + 1 }))
    setTab('lookup')
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-4">
          <h1 className="text-xl font-bold text-indigo-700">Banking System</h1>
          <nav className="flex gap-1">
            {tabs.map((t) => (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
                  tab === t.id ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                {t.label}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-8">
        {tab === 'transfer' && <Transfer savedAccounts={accounts} onShowAccount={showAccount} />}
        {tab === 'lookup' && (
          // key degisince bilesen sifirlanir ve yeni hesap otomatik sorgulanir
          <AccountLookup
            key={lookup.version}
            initialAccountNumber={lookup.accountNumber}
            savedAccounts={accounts}
          />
        )}
        {tab === 'create' && <CreateAccount onCreated={addAccount} onShowAccount={showAccount} />}
      </main>
    </div>
  )
}

export default App
