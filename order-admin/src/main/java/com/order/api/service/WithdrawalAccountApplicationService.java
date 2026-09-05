package com.order.api.service;

import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountResponse;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalTypeResponse;
import com.order.common.exception.ServiceException;
import com.order.common.utils.DateUtils;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderWithdrawalType;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.IOrderWithdrawalTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.order.api.service.AccountErrorCodes.ACCOUNT_MUTATION_CONFLICT;
import static com.order.api.service.AccountErrorCodes.INVALID_REQUEST;
import static com.order.api.service.AccountErrorCodes.USER_NOT_FOUND;
import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_ACCOUNT;
import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_DISABLED;

@Service
@Transactional(readOnly = true)
public class WithdrawalAccountApplicationService {
    private final GoodsWithdrawalAccountMapper accountMapper;
    private final OrderWithdrawalMapper withdrawalMapper;
    private final OrderUserMapper userMapper;
    private final IOrderWithdrawalTypeService withdrawalTypeService;
    private final IOrderConfigService configService;
    private final AccountDataCipher cipher;

    public WithdrawalAccountApplicationService(
            GoodsWithdrawalAccountMapper accountMapper,
            OrderWithdrawalMapper withdrawalMapper,
            OrderUserMapper userMapper,
            IOrderWithdrawalTypeService withdrawalTypeService,
            IOrderConfigService configService,
            AccountDataCipher cipher) {
        this.accountMapper = accountMapper;
        this.withdrawalMapper = withdrawalMapper;
        this.userMapper = userMapper;
        this.withdrawalTypeService = withdrawalTypeService;
        this.configService = configService;
        this.cipher = cipher;
    }

    public List<WithdrawalTypeResponse> listTypes() {
        return withdrawalTypeService.selectOrderWithdrawalTypeList(new OrderWithdrawalType()).stream()
                .sorted(Comparator.comparing(OrderWithdrawalType::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrderWithdrawalType::getId))
                .map(type -> new WithdrawalTypeResponse(type.getId(), type.getName(), type.getType()))
                .toList();
    }

    public List<WithdrawalAccountResponse> list(Long userId) {
        return accountMapper.selectActiveByUserId(userId).stream().map(this::toResponse).toList();
    }

    public WithdrawalAccountResponse get(Long userId, Long id) {
        return toResponse(requireOwned(userId, id));
    }

    public WithdrawalAccountResponse getForEdit(Long userId, Long id) {
        return toFullResponse(cipher.reveal(requireOwned(userId, id)));
    }

    /**
     * Full-data view for the separately permissioned management console.
     * The mapper-owned list instance is deliberately retained so PageHelper metadata survives
     * decryption and the controller can still report the database total.
     */
    public List<GoodsWithdrawalAccount> listAdmin(GoodsWithdrawalAccount criteria) {
        GoodsWithdrawalAccount safeCriteria = criteria == null
                ? new GoodsWithdrawalAccount()
                : criteria;
        List<GoodsWithdrawalAccount> accounts = accountMapper.selectGoodsWithdrawalAccountList(safeCriteria);
        accounts.forEach(this::revealForAdmin);
        return accounts;
    }

    public GoodsWithdrawalAccount getAdmin(Long id) {
        try {
            return revealForAdmin(requireActive(id));
        } catch (AccountApiException exception) {
            throw new ServiceException(exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GoodsWithdrawalAccount createAdmin(GoodsWithdrawalAccount input) {
        try {
            Long userId = requireAdminUserId(input);
            return revealForAdmin(createAccount(userId, toAdminRequest(input)));
        } catch (AccountApiException exception) {
            throw new ServiceException(exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GoodsWithdrawalAccount updateAdmin(GoodsWithdrawalAccount input) {
        try {
            Long userId = requireAdminUserId(input);
            if (input.getId() == null) {
                throw AccountApiException.badRequest(INVALID_REQUEST, "Withdrawal account id is required");
            }
            return revealForAdmin(updateAccount(
                    userId, input.getId(), toAdminRequest(input), false));
        } catch (AccountApiException exception) {
            throw new ServiceException(exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAdmin(Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                throw AccountApiException.badRequest(INVALID_REQUEST, "Withdrawal account id is required");
            }

            Set<Long> uniqueIds = new HashSet<>();
            List<GoodsWithdrawalAccount> accounts = new ArrayList<>();
            for (Long id : ids) {
                if (id != null && uniqueIds.add(id)) {
                    accounts.add(requireActive(id));
                }
            }
            if (accounts.isEmpty()) {
                throw AccountApiException.badRequest(INVALID_REQUEST, "Withdrawal account id is required");
            }

            accounts.sort(Comparator.comparing(GoodsWithdrawalAccount::getUserId)
                    .thenComparing(GoodsWithdrawalAccount::getId));
            Long lockedUserId = null;
            for (GoodsWithdrawalAccount account : accounts) {
                if (!account.getUserId().equals(lockedUserId)) {
                    lockUser(account.getUserId());
                    lockedUserId = account.getUserId();
                }
            }

            Set<Long> affectedUsers = new HashSet<>();
            for (GoodsWithdrawalAccount account : accounts) {
                GoodsWithdrawalAccount existing = requireOwned(account.getUserId(), account.getId());
                ensureNoPendingWithdrawal(existing.getId());
                if (accountMapper.softDeleteOwned(existing.getId(), existing.getUserId()) != 1) {
                    throw AccountApiException.conflict(
                            ACCOUNT_MUTATION_CONFLICT,
                            "Withdrawal account was changed by another request");
                }
                affectedUsers.add(existing.getUserId());
            }
            affectedUsers.forEach(userId -> normalizeDefaultAccount(userId, null));
        } catch (AccountApiException exception) {
            throw new ServiceException(exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public WithdrawalAccountResponse create(Long userId, WithdrawalAccountRequest request) {
        return toResponse(createAccount(userId, request));
    }

    private GoodsWithdrawalAccount createAccount(Long userId, WithdrawalAccountRequest request) {
        lockUser(userId);
        GoodsWithdrawalAccount account = buildAccount(userId, null, request);
        boolean makeDefault = accountMapper.existsDefaultByUserId(userId) == 0
                || Boolean.TRUE.equals(request.isDefault());
        account.setIsDefault(makeDefault ? "0" : "1");
        account.setDeleted("0");
        account.setCreateTime(DateUtils.getNowDate());
        account.setUpdateTime(DateUtils.getNowDate());
        cipher.protect(account);
        if (accountMapper.insertGoodsWithdrawalAccount(account) != 1) {
            throw new IllegalStateException("Unable to save withdrawal account");
        }
        normalizeDefaultAccount(userId, makeDefault ? account.getId() : null);
        return requireOwned(userId, account.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WithdrawalAccountResponse update(Long userId, Long id, WithdrawalAccountRequest request) {
        return toResponse(updateAccount(userId, id, request, true));
    }

    private GoodsWithdrawalAccount updateAccount(
            Long userId,
            Long id,
            WithdrawalAccountRequest request,
            boolean enforceCustomerSetting) {
        if (enforceCustomerSetting) {
            ensureModificationAllowed();
        }
        lockUser(userId);
        requireOwned(userId, id);
        ensureNoPendingWithdrawal(id);
        GoodsWithdrawalAccount account = buildAccount(userId, id, request);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        account.setIsDefault(makeDefault ? "0" : "1");
        cipher.protect(account);
        if (accountMapper.updateOwnedAccount(account) != 1) {
            throw AccountApiException.conflict(ACCOUNT_MUTATION_CONFLICT, "Withdrawal account was changed by another request");
        }
        normalizeDefaultAccount(userId, makeDefault ? id : null);
        return requireOwned(userId, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        ensureModificationAllowed();
        lockUser(userId);
        requireOwned(userId, id);
        ensureNoPendingWithdrawal(id);
        if (accountMapper.softDeleteOwned(id, userId) != 1) {
            throw AccountApiException.conflict(ACCOUNT_MUTATION_CONFLICT, "Withdrawal account was changed by another request");
        }
        normalizeDefaultAccount(userId, null);
    }

    private GoodsWithdrawalAccount buildAccount(Long userId, Long id, WithdrawalAccountRequest request) {
        Long typeId;
        try {
            typeId = Long.valueOf(trim(request.withdrawalTypeId()));
        } catch (RuntimeException ex) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid withdrawal type");
        }
        OrderWithdrawalType type = withdrawalTypeService.selectOrderWithdrawalTypeById(typeId);
        if (type == null || (!"0".equals(type.getType()) && !"1".equals(type.getType()))) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid withdrawal type");
        }

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(id);
        account.setUserId(userId);
        account.setType(type.getType());
        account.setWithdrawalTypeId(String.valueOf(type.getId()));
        account.setWithdrawalType(type.getName());

        if ("1".equals(type.getType())) {
            account.setAccountName(trimToNull(request.accountName()));
            account.setWalletName(trimToNull(request.walletName()));
            account.setWalletAddress(required(request.walletAddress(), "walletAddress"));
            account.setAttachment(trimToNull(request.attachment()));
        } else {
            account.setBankName(required(request.bankName(), "bankName"));
            account.setBankAccount(required(request.bankAccount(), "bankAccount"));
            account.setAccountHolder(required(request.accountHolder(), "accountHolder"));
            account.setDepositType(trimToNull(request.depositType()));
            account.setBranchCode(trimToNull(request.branchCode()));
            account.setBranchName(trimToNull(request.branchName()));
            account.setAccountName(trimToNull(request.accountName()));
        }
        return account;
    }

    private GoodsWithdrawalAccount requireOwned(Long userId, Long id) {
        GoodsWithdrawalAccount account = accountMapper.selectActiveByIdAndUserId(id, userId);
        if (account == null || account.getUserId() == null) {
            throw AccountApiException.notFound(WITHDRAWAL_ACCOUNT, "Withdrawal account not found");
        }
        return account;
    }

    private GoodsWithdrawalAccount requireActive(Long id) {
        if (id == null) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Withdrawal account id is required");
        }
        GoodsWithdrawalAccount account = accountMapper.selectActiveById(id);
        if (account == null || account.getUserId() == null) {
            throw AccountApiException.notFound(WITHDRAWAL_ACCOUNT, "Withdrawal account not found");
        }
        return account;
    }

    private void ensureNoPendingWithdrawal(Long id) {
        if (withdrawalMapper.existsPendingByAccountId(id) != 0) {
            throw AccountApiException.conflict(
                    ACCOUNT_MUTATION_CONFLICT,
                    "A pending withdrawal is using this account");
        }
    }

    private void normalizeDefaultAccount(Long userId, Long preferredId) {
        Long selectedId = preferredId;
        if (selectedId == null) {
            selectedId = accountMapper.selectActiveByUserId(userId).stream()
                    .filter(account -> "0".equals(account.getIsDefault()))
                    .map(GoodsWithdrawalAccount::getId)
                    .findFirst()
                    .orElse(null);
        }

        accountMapper.clearDefaultByUserId(userId);
        if (selectedId != null) {
            if (accountMapper.setDefaultByIdAndUserId(selectedId, userId) != 1) {
                throw AccountApiException.conflict(
                        ACCOUNT_MUTATION_CONFLICT,
                        "Withdrawal account was changed by another request");
            }
        } else {
            accountMapper.setNewestActiveAsDefault(userId);
        }
    }

    private Long requireAdminUserId(GoodsWithdrawalAccount input) {
        if (input == null || input.getUserId() == null) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "User id is required");
        }
        return input.getUserId();
    }

    private WithdrawalAccountRequest toAdminRequest(GoodsWithdrawalAccount input) {
        String isDefault = input.getIsDefault();
        if (isDefault != null && !"0".equals(isDefault) && !"1".equals(isDefault)) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid default-account value");
        }
        return new WithdrawalAccountRequest(
                input.getWithdrawalTypeId(),
                "0".equals(isDefault),
                input.getBankName(),
                input.getDepositType(),
                input.getBranchCode(),
                input.getBranchName(),
                input.getBankAccount(),
                input.getAccountHolder(),
                input.getAccountName(),
                input.getWalletName(),
                input.getWalletAddress(),
                input.getAttachment());
    }

    private GoodsWithdrawalAccount revealForAdmin(GoodsWithdrawalAccount account) {
        cipher.reveal(account);
        account.setBankAccountMask(null);
        account.setAccountHolderMask(null);
        account.setAccountNameMask(null);
        account.setWalletAddressMask(null);
        return account;
    }

    private void lockUser(Long userId) {
        if (userId == null || userMapper.lockUserById(userId) == null) {
            throw AccountApiException.notFound(USER_NOT_FOUND, "User not found");
        }
    }

    private void ensureModificationAllowed() {
        Object value = configService.getConfigValue("trade", "allowModifyWithdrawalAddress").orElse("1");
        if (!isEnabled(value)) {
            throw AccountApiException.forbidden(WITHDRAWAL_DISABLED, "Withdrawal account modification is disabled");
        }
    }

    private WithdrawalAccountResponse toResponse(GoodsWithdrawalAccount account) {
        if (account == null) {
            throw AccountApiException.notFound(WITHDRAWAL_ACCOUNT, "Withdrawal account not found");
        }
        return new WithdrawalAccountResponse(
                account.getId(),
                account.getType(),
                account.getWithdrawalTypeId(),
                account.getWithdrawalType(),
                "0".equals(account.getIsDefault()),
                account.getBankName(),
                account.getDepositType(),
                account.getBranchCode(),
                account.getBranchName(),
                firstNonBlank(account.getBankAccountMask(), maskEnding(account.getBankAccount())),
                firstNonBlank(account.getAccountHolderMask(), maskName(account.getAccountHolder())),
                firstNonBlank(account.getAccountNameMask(), maskName(account.getAccountName())),
                account.getWalletName(),
                firstNonBlank(account.getWalletAddressMask(), maskWallet(account.getWalletAddress())),
                account.getAttachment(),
                account.getCreateTime(),
                account.getUpdateTime());
    }

    private WithdrawalAccountResponse toFullResponse(GoodsWithdrawalAccount account) {
        return new WithdrawalAccountResponse(
                account.getId(),
                account.getType(),
                account.getWithdrawalTypeId(),
                account.getWithdrawalType(),
                "0".equals(account.getIsDefault()),
                account.getBankName(),
                account.getDepositType(),
                account.getBranchCode(),
                account.getBranchName(),
                account.getBankAccount(),
                account.getAccountHolder(),
                account.getAccountName(),
                account.getWalletName(),
                account.getWalletAddress(),
                account.getAttachment(),
                account.getCreateTime(),
                account.getUpdateTime());
    }

    private boolean isEnabled(Object value) {
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        // Legacy trade-setting radios use 0 for enabled/yes and 1 for disabled/no.
        return "0".equals(normalized) || "yes".equals(normalized)
                || "true".equals(normalized) || "enabled".equals(normalized);
    }

    private String required(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw AccountApiException.badRequest(INVALID_REQUEST, field + " is required");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String maskEnding(String value) {
        if (value == null || value.isBlank()) return value;
        String compact = value.replaceAll("\\s+", "");
        return "****" + compact.substring(Math.max(0, compact.length() - 4));
    }

    private String maskWallet(String value) {
        if (value == null || value.isBlank()) return value;
        return value.length() <= 10 ? maskEnding(value)
                : value.substring(0, 6) + "****" + value.substring(value.length() - 4);
    }

    private String maskName(String value) {
        return value == null || value.isBlank() ? value : value.substring(0, 1) + "***";
    }
}
