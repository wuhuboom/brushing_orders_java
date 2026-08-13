export const notificationKinds = [
  { key: "gift", label: "赠送" },
  { key: "deduction", label: "扣款" },
  { key: "recharge", label: "充值" },
  { key: "withdrawing", label: "提现中" },
  { key: "withdrawalUnfreeze", label: "提现解冻" },
  { key: "withdrawal", label: "提现" },
  { key: "task", label: "任务" },
  { key: "principalReturn", label: "本金返回" },
  { key: "rebate", label: "返佣" },
  { key: "subRebate", label: "下级返佣" },
  { key: "signIn", label: "签到" },
  { key: "fee", label: "手续费" },
  { key: "deposit", label: "存款" },
  { key: "bonus", label: "奖金" },
  { key: "baseSalary", label: "底薪" },
  { key: "aid", label: "援助金" },
  { key: "registerBonus", label: "注册赠送" },
  { key: "productShare", label: "商品分润" },
  { key: "taskReward", label: "任务奖励" },
  { key: "balanceOut", label: "余额宝转出" },
  { key: "balanceIn", label: "余额宝转入" },
  { key: "workBonus", label: "工作奖金" },
  { key: "upgradeBonus", label: "升级奖金" },
  { key: "subsidy", label: "补贴" },
  { key: "abnormalDeposit", label: "资金异常存款" },
  { key: "activity", label: "活动" },
  { key: "pointsExchange", label: "积分兑换" },
  { key: "loginSignIn", label: "登录签到" },
  { key: "taskSignIn", label: "完成任务签到" },
  { key: "creditScore", label: "信誉分" },
  { key: "other", label: "其他" },
];

export function createNotificationTemplate(source = {}) {
  const title = String(source.title || "");
  const content = String(source.content || "");
  const isComplete = title.trim() !== "" && content.trim() !== "";

  return {
    enabled: Number(source.enabled ?? 0) === 1 && isComplete ? 1 : 0,
    formatAmount: Number(source.formatAmount ?? 0),
    title,
    content,
  };
}
