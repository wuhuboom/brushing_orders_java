export const tradeTypeOptions = Object.freeze([
  { label: "赠送", value: "zs" },
  { label: "扣款", value: "kk" },
  { label: "充值", value: "cz" },
  { label: "提现中", value: "txz" },
  { label: "提现解冻", value: "txjd" },
  { label: "提现", value: "tx" },
  { label: "提现驳回", value: "txbh" },
  { label: "任务", value: "rw" },
  { label: "本金返还", value: "bjfh" },
  { label: "返佣", value: "fy" },
  { label: "下级返佣", value: "xjfy" },
  { label: "签到", value: "qd" },
  { label: "手续费", value: "sxf" },
  { label: "存款", value: "ck" },
  { label: "奖金", value: "jj" },
  { label: "底薪", value: "dx" },
  { label: "援助金", value: "yzj" },
  { label: "注册赠送", value: "zczs" },
  { label: "商品分润", value: "spfr" },
  { label: "任务奖励", value: "rwjl" },
  { label: "余额宝转出", value: "yebzc" },
  { label: "余额宝转入", value: "yebzr" },
  { label: "工作奖励", value: "gzjl" },
  { label: "升级奖励", value: "sjjl" },
  { label: "其他", value: "qt" },
])

const legacyTransactionTypes = Object.freeze({
  bonus: "jj",
  deduction: "kk",
  recharge: "cz",
  withdrawing: "txz",
  withdrawalUnfreeze: "txjd",
  withdrawal: "tx",
  task: "rw",
  principalReturn: "bjfh",
  rebate: "fy",
  subRebate: "xjfy",
  signIn: "qd",
  fee: "sxf",
  deposit: "ck",
  baseSalary: "dx",
  aid: "yzj",
  registerBonus: "zczs",
  productShare: "spfr",
  taskReward: "rwjl",
  balanceOut: "yebzc",
  balanceIn: "yebzr",
  workBonus: "gzjl",
  upgradeBonus: "sjjl",
  other: "qt",
})

const allowedValues = new Set(tradeTypeOptions.map(option => option.value))
const DEFAULT_RECHARGE_GIFT_TRANSACTION_TYPE = "zs"

export function normalizeRechargeBonusTradeType(value) {
  if (value === null || value === undefined || value === "") {
    return DEFAULT_RECHARGE_GIFT_TRANSACTION_TYPE
  }
  const raw = String(value).trim()
  const normalized = legacyTransactionTypes[raw] || raw
  return allowedValues.has(normalized)
    ? normalized
    : DEFAULT_RECHARGE_GIFT_TRANSACTION_TYPE
}
