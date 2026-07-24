export const FILE_REFERENCE_KINDS = [
  { value: 'USER_AVATAR', label: '用户头像', legacyValue: 1 },
  { value: 'MEMBER_AVATAR', label: '会员头像', legacyValue: 2 },
  { value: 'ADMIN_UPLOAD', label: '后台上传', legacyValue: 3 },
  { value: 'GOODS_CATEGORY', label: '商品类目', legacyValue: 4 },
  { value: 'GOODS', label: '商品', legacyValue: 5 },
  { value: 'BANNER', label: '横幅', legacyValue: 6 },
  { value: 'CUSTOMER_SERVICE', label: '客服', legacyValue: 7 },
  { value: 'MEMBER_LEVEL', label: '会员等级', legacyValue: 8 },
  { value: 'POINTS_GIFT', label: '积分礼品', legacyValue: 9 },
  { value: 'ACTIVITY', label: '活动', legacyValue: 10 }
]

const labelMap = new Map(FILE_REFERENCE_KINDS.flatMap(item => [
  [item.value, item.label],
  [String(item.legacyValue), item.label]
]))
labelMap.set('GENERAL', '后台上传')

export function fileReferenceKindLabel(value) {
  return labelMap.get(String(value ?? '')) || value || '-'
}
