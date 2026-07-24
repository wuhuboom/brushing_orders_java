const legacyRootLabels = {
  系统管理: '系统管理[system]',
  客户管理: '用户管理[marketing]',
  积分商城: '积分商城[points]',
  活动管理: '活动[activities]',
  官网管理: '官网[websites]'
}

export function decorateLegacyResourceTree(nodes = [], depth = 0) {
  return nodes.map(node => ({
    ...node,
    label: depth === 0 ? (legacyRootLabels[node.label] || node.label) : node.label,
    children: decorateLegacyResourceTree(node.children || [], depth + 1)
  }))
}
