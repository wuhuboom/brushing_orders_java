const EDITABLE_MEMBER_FIELDS = [
  "phoneNumber",
  "email",
  "birthday",
  "vipId",
  "reputationScore",
  "avatar",
  "parentId",
  "parentInviteCode",
  "gender",
  "isEnabled",
  "allowInvite",
  "isFrozen",
  "isFake",
  "isBanned",
  "workLimit",
  "accountStatus",
  "transactionStatus",
  "withdrawalStatus",
  "assistWithdrawalStatus",
  "depositBlockWithdrawal",
  "isWithdrawalNotification",
  "web3AuthEnabled",
  "isInvalid",
  "isActivity",
  "verifyIdentityBeforeTask",
  "withdrawalPasswordFailLimit",
  "withdrawalPasswordFailCount",
  "maxSingleWithdrawal",
  "remarks",
];

const CREATE_ONLY_FIELDS = ["username", "password", "tradePassword"];

function pickDefined(source, fields) {
  return fields.reduce((result, field) => {
    if (Object.prototype.hasOwnProperty.call(source || {}, field)) {
      result[field] = source[field];
    }
    return result;
  }, {});
}

export function buildMemberSubmitPayload(source, { birthdayToTimestamp } = {}) {
  const editing = source?.id != null;
  const payload = pickDefined(source, [
    ...EDITABLE_MEMBER_FIELDS,
    ...(editing ? [] : CREATE_ONLY_FIELDS),
  ]);
  const parentInviteCode = String(payload.parentInviteCode || "").trim();

  if (Object.prototype.hasOwnProperty.call(payload, "birthday") && birthdayToTimestamp) {
    payload.birthday = birthdayToTimestamp(payload.birthday);
  }
  payload.parentInviteCode = parentInviteCode || null;
  payload.parentId = parentInviteCode ? (payload.parentId ?? null) : 0;

  if (editing) {
    payload.id = source.id;
    if (source.version != null) payload.version = source.version;
  } else {
    payload.username = String(payload.username || "").trim();
    payload.password = String(payload.password || "").trim();
    payload.tradePassword = String(payload.tradePassword || "").trim();
  }

  return payload;
}

export function buildCopyMemberForm(defaults, source, { formatBirthday } = {}) {
  const copied = pickDefined(source, ["username", ...EDITABLE_MEMBER_FIELDS]);
  return {
    ...defaults,
    ...copied,
    id: null,
    inviteCode: null,
    password: null,
    tradePassword: null,
    version: null,
    birthday: source?.birthday && formatBirthday
      ? formatBirthday(source.birthday)
      : (source?.birthday ?? null),
  };
}

export function buildModifyCountPayload(source) {
  const payload = {
    id: source?.id,
    taskProgress: source?.taskProgress,
  };
  if (source?.version != null) payload.version = source.version;
  return payload;
}

/**
 * `user_yes_no` follows the legacy contract: 0 = 是, 1 = 否. For isFake that
 * means 0 is currently a fake member and 1 is currently a real member.
 */
export function buildFakeMemberToggle(value) {
  return String(value ?? "") === "0"
    ? { value: "1", action: "设为真人" }
    : { value: "0", action: "设为假人" };
}

export const memberPayloadFields = Object.freeze({
  editable: [...EDITABLE_MEMBER_FIELDS],
  createOnly: [...CREATE_ONLY_FIELDS],
});
