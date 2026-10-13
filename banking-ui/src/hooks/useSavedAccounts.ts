import { useCallback, useState } from 'react'

// Bu tarayicida acilan hesaplar - formlarda hizli secim icin localStorage'da tutulur
export interface SavedAccount {
  accountNumber: string
  accountHolderName: string
}

const STORAGE_KEY = 'banking-ui.saved-accounts'

function load(): SavedAccount[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

export function useSavedAccounts() {
  const [accounts, setAccounts] = useState<SavedAccount[]>(load)

  const addAccount = useCallback((account: SavedAccount) => {
    setAccounts((current) => {
      const next = [account, ...current.filter((a) => a.accountNumber !== account.accountNumber)]
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  return { accounts, addAccount }
}
