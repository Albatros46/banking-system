const currency = new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' })
const dateTime = new Intl.DateTimeFormat('de-DE', { dateStyle: 'short', timeStyle: 'medium' })

export const formatMoney = (value: number) => currency.format(value)

export const formatDate = (value: string | null) => (value ? dateTime.format(new Date(value)) : '-')

// 123456789012 -> 1234 5678 9012
export const formatAccountNumber = (value: string) => value.replace(/(\d{4})(?=\d)/g, '$1 ')
