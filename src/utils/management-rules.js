export function selectionFlags(length) {
  return { single: length !== 1, multiple: length === 0 }
}

export function resolveDeleteIds(row, selectedIds, key = 'id') {
  const rowId = row && typeof row === 'object' ? row[key] : undefined
  const candidates = rowId !== undefined && rowId !== null && rowId !== ''
    ? [rowId]
    : selectedIds

  return (Array.isArray(candidates) ? candidates : [candidates])
    .filter((id) => id !== undefined && id !== null && id !== '')
}

export function signedAmount(operationType, amount) {
  const value = Number(amount || 0)
  return ['subtract', '0', '减'].includes(String(operationType)) ? -value : value
}

export function isOrderActionDisabled(status, action) {
  if (action === 'ship') return status !== '1'
  if (action === 'receive') return status !== '2'
  if (action === 'cancel') return status === '4'
  return true
}

export function expandDateRanges(query, searchFields) {
  const params = { ...query }
  searchFields.forEach((field) => {
    if (field.type !== 'daterange' || !field.prop) return
    const range = params[field.prop]
    params[field.startProp || 'startTime'] = Array.isArray(range) ? range[0] : null
    params[field.endProp || 'endTime'] = Array.isArray(range) ? range[1] : null
    delete params[field.prop]
  })
  return params
}
