<template>
  <div class="app-container member-orderuser-page ant-pro-member-page">
    <ant-pro-table
      title="会员列表"
      column-state-key="member.orderuser.main"
      :columns="memberColumns"
      :data-source="orderuserList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{ x: 6602, y: 'calc(100vh - 440px)' }"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total, size: 'small' }"
      @page-change="handleAntPageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form
          layout="horizontal"
          :model="queryParams"
          :label-col="{ flex: '100px' }"
          :wrapper-col="{ flex: 1 }"
          class="ant-pro-query-form"
        >
          <a-row :gutter="[24, 24]">
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="关键字">
                <a-input
                  v-model:value="queryParams.keyword"
                  allow-clear
                  placeholder="用户名/手机号码/IP/邀请码"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="上级用户名">
                <a-input
                  v-model:value="queryParams.parentUsername"
                  allow-clear
                  placeholder="请输入"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col
              :xs="24"
              :sm="12"
              :md="8"
              :lg="8"
              :xxl="6"
              class="online-query-field"
              :class="{ 'online-query-field-collapsed': !advancedSearchVisible }"
            >
              <a-form-item label="是否在线">
                <a-select v-model:value="queryParams.isOnline" allow-clear placeholder="请选择">
                  <a-select-option value="1">是</a-select-option>
                  <a-select-option value="0">否</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <template v-if="advancedSearchVisible">
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="ID">
                <a-input
                  :value="queryParams.id"
                  allow-clear
                  inputmode="numeric"
                  :maxlength="19"
                  placeholder="请输入"
                  @update:value="updateMemberIdQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="VIP等级">
                <a-select v-model:value="queryParams.vipId" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in levelList" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item>
                <template #label>
                  <span>用户名列表</span>
                  <a-tooltip title="多个用户名请使用逗号分隔">
                    <QuestionCircleOutlined class="query-label-help" />
                  </a-tooltip>
                </template>
                <a-input v-model:value="queryParams.usernameList" allow-clear placeholder="请输入" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="上级邀请码">
                <a-input v-model:value="queryParams.parentInviteCode" allow-clear placeholder="请输入" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="余额">
                <a-space-compact block>
                  <a-input-number v-model:value="queryParams.balanceMin" :min="0" placeholder="请输入" class="range-input" />
                  <a-input class="range-separator" value="~" disabled />
                  <a-input-number v-model:value="queryParams.balanceMax" :min="0" placeholder="请输入" class="range-input" />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="最后登录IP">
                <a-input v-model:value="queryParams.lastLoginIp" allow-clear placeholder="请输入" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="信誉分">
                <a-space-compact block>
                  <a-input-number v-model:value="queryParams.reputationMin" :min="0" :precision="0" placeholder="请输入" class="range-input" />
                  <a-input class="range-separator" value="~" disabled />
                  <a-input-number v-model:value="queryParams.reputationMax" :min="0" :precision="0" placeholder="请输入" class="range-input" />
                </a-space-compact>
              </a-form-item>
            </a-col>
            <a-col
              v-for="field in advancedSelectFieldsBeforeWorkLimit"
              :key="field.key"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="8"
              :xxl="6"
            >
              <a-form-item :label="field.label">
                <a-select v-model:value="queryParams[field.key]" allow-clear placeholder="请选择">
                  <a-select-option v-for="option in field.options" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="工作限额">
                <a-input-number
                  v-model:value="queryParams.workLimit"
                  string-mode
                  :min="0"
                  allow-clear
                  class="full-width"
                  placeholder="请输入"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-for="field in advancedSelectFieldsAfterWorkLimit"
              :key="field.key"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="8"
              :xxl="6"
            >
              <a-form-item :label="field.label">
                <a-select v-model:value="queryParams[field.key]" allow-clear placeholder="请选择">
                  <a-select-option v-for="option in field.options" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="queryParams.createTimeRange"
                  value-format="YYYY-MM-DD"
                  class="full-width"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            </template>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xxl="6" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button type="link" class="ant-pro-expand-btn" @click="advancedSearchVisible = !advancedSearchVisible">
                  {{ advancedSearchVisible ? "收起" : "展开" }}
                  <DownOutlined :class="{ 'ant-pro-expand-icon-open': advancedSearchVisible }" />
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-button type="primary" :disabled="multiple" @click="handleUnlock" v-hasPermi="['member:orderuser:unlock']">
          <UnlockOutlined />
          登录解冻
        </a-button>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:orderuser:add']">
          <PlusOutlined />
          创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'">
          <a
            v-if="hasPermission('member:orderuser:query')"
            v-hasPermi="['member:orderuser:query']"
            class="table-action-link"
            @click="handleView(record)"
          >{{ record.id }}</a>
          <span v-else>{{ record.id }}</span>
        </template>
        <template v-else-if="column.key === 'username'">
          <span
            class="member-dot"
            :class="{ 'member-dot-online': String(record.isOnline) === '1' }"
          ></span>{{ record.username || '-' }}
        </template>
        <template v-else-if="column.key === 'phoneNumber'">
          {{ record.phoneNumber || '-' }}
        </template>
        <template v-else-if="column.key === 'vip'">
          {{ record.memberLevel?.name || '-' }}
        </template>
        <template v-else-if="column.key === 'parentInfo'">
          <div>
            <a
              v-if="record.parentInviteCode && hasPermission('member:orderuser:edit')"
              v-hasPermi="['member:orderuser:edit']"
              class="table-action-link"
              @click="handleModifyParent(record)"
            >
              上级邀请码: {{ record.parentInviteCode }}
            </a>
            <span v-else>上级邀请码: {{ record.parentInviteCode || '-' }}</span>
            <a-tooltip v-if="record.parentInviteCode" title="复制上级邀请码">
              <a-button
                type="text"
                size="small"
                class="copy-button"
                aria-label="复制"
                @click="copyText(record.parentInviteCode)"
              >
                <CopyOutlined />
              </a-button>
            </a-tooltip>
          </div>
          <div>上级用户名: {{ record.parentUsername || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'resetInfo'">
          <div>今日重置次数: {{ record.todayRest || 0 }}</div>
          <div>累计重置次数: {{ record.totalRest || 0 }}</div>
        </template>
        <template v-else-if="column.key === 'balanceInfo'">
          <div>总余额: {{ Number(record.balance || 0) + Number(record.frozenBalance || 0) }}</div>
          <div :class="{ 'negative-balance': Number(record.balance) < 0 }">
            余额: {{ record.balance ?? 0 }}
          </div>
          <div>冻结余额: {{ record.frozenBalance || 0 }}</div>
          <div>底薪: {{ record.baseSalary || 0 }}</div>
          <div>今日佣金: {{ record.todayCommission || 0 }}</div>
          <div>今日上级佣金: {{ record.todayParentCommission || 0 }}</div>
        </template>
        <template v-else-if="column.key === 'taskProgress'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="openModifyCount(record)"
          >
            {{ record.taskProgress || 0 }} / {{ record.memberLevel?.orderCountPerDay || 0 }}
          </a>
          <span v-else>{{ record.taskProgress || 0 }} / {{ record.memberLevel?.orderCountPerDay || 0 }}</span>
        </template>
        <template v-else-if="column.key === 'completeGroupNum'">
          {{ completeGroupText(record) }}
        </template>
        <template v-else-if="column.key === 'loginInfo'">
          <div>最后登录IP: {{ record.lastLoginIp || '-' }}</div>
          <div>最后登录地址: {{ record.lastLoginAddress || '-' }}</div>
          <div>最后登录时间: {{ parseTime(record.lastLoginTime) || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'signinInfo'">
          <div>
            <a
              v-if="hasPermission('member:orderuser:edit')"
              v-hasPermi="['member:orderuser:edit']"
              class="table-action-link"
              @click="handleModifySignDays(record)"
            >
              签到天数: {{ record.signDays || 0 }}
            </a>
            <span v-else>签到天数: {{ record.signDays || 0 }}</span>
          </div>
          <div>今日签到次数: {{ record.todaySignCount || 0 }}</div>
          <div>累计签到次数: {{ record.totalSignDays || 0 }}</div>
        </template>
        <template v-else-if="column.key === 'statInfo'">
          <div>直属下级数量: {{ record.directChildrenCount || 0 }}</div>
          <div>今日提现次数: {{ record.todayWithdrawalCount || 0 }}</div>
          <div>累计提现金额: {{ record.totalWithdrawalAmount || 0 }}</div>
          <div>累计充值金额: {{ record.totalRechargeAmount || 0 }}</div>
        </template>
        <template v-else-if="column.key === 'gender'">
          <a-badge
            v-if="genderBadgeStatus(record.gender)"
            class="member-status-badge"
            :status="genderBadgeStatus(record.gender)"
            :text="dictText(sys_user_sex, record.gender)"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'birthday'">
          {{ parseTime(record.birthday, '{y}-{m}-{d}') || '-' }}
        </template>
        <template v-else-if="column.key === 'reputationScore'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleModifyReputation(record)"
          >
            {{ record.reputationScore ?? 100 }}
          </a>
          <span v-else>{{ record.reputationScore ?? 100 }}</span>
        </template>
        <template v-else-if="column.key === 'inviteCode'">
          {{ record.inviteCode || '-' }}
          <a-tooltip v-if="record.inviteCode" title="复制邀请码">
            <a-button
              type="text"
              size="small"
              class="copy-button"
              aria-label="复制"
              @click="copyText(record.inviteCode)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'email'">
          {{ record.email || '-' }}
        </template>
        <template v-else-if="column.key === 'isEnabled'">
          <a-badge
            v-if="enabledBadgeStatus(record.isEnabled)"
            class="member-status-badge"
            :status="enabledBadgeStatus(record.isEnabled)"
            :text="dictText(sys_enabled, record.isEnabled)"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'allowInvite'">
          <a-badge
            v-if="yesNoBadgeStatus(record.allowInvite)"
            class="member-status-badge"
            :status="yesNoBadgeStatus(record.allowInvite)"
            :text="dictText(user_yes_no, record.allowInvite)"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'isFrozen'">
          <a-badge
            v-if="yesNoBadgeStatus(record.isFrozen)"
            class="member-status-badge"
            :status="yesNoBadgeStatus(record.isFrozen)"
            :text="dictText(user_yes_no, record.isFrozen)"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'workLimit'">
          {{ record.workLimit ?? 0 }}
        </template>
        <template v-else-if="column.key === 'isFake'">
          <a
            v-if="fakeMemberTone(record.isFake) && hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="fake-member-link"
            :class="`fake-member-${fakeMemberTone(record.isFake)}`"
            @click="handleToggleFake(record)"
          >
            {{ dictText(user_yes_no, record.isFake) }}
          </a>
          <span
            v-else-if="fakeMemberTone(record.isFake)"
            :class="`fake-member-${fakeMemberTone(record.isFake)}`"
          >
            {{ dictText(user_yes_no, record.isFake) }}
          </span>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'isBanned'">
          <a-badge
            v-if="yesNoBadgeStatus(record.isBanned)"
            class="member-status-badge"
            :status="yesNoBadgeStatus(record.isBanned)"
            :text="dictText(user_yes_no, record.isBanned)"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'productMatching'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleToggleProductMatching(record)"
          >
            {{ dictText(sys_enabled, record.productMatching) }}
          </a>
          <span v-else>{{ dictText(sys_enabled, record.productMatching) }}</span>
        </template>
        <template v-else-if="column.key === 'accountStatus'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleToggleAccountStatus(record)"
          >
            {{ dictText(sys_enabled, record.accountStatus) }}
          </a>
          <span v-else>{{ dictText(sys_enabled, record.accountStatus) }}</span>
        </template>
        <template v-else-if="column.key === 'transactionStatus'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleToggleTransactionStatus(record)"
          >
            {{ dictText(sys_enabled, record.transactionStatus) }}
          </a>
          <span v-else>{{ dictText(sys_enabled, record.transactionStatus) }}</span>
        </template>
        <template v-else-if="column.key === 'withdrawalStatus'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleToggleWithdrawalStatus(record)"
          >
            {{ dictText(sys_enabled, record.withdrawalStatus) }}
          </a>
          <span v-else>{{ dictText(sys_enabled, record.withdrawalStatus) }}</span>
        </template>
        <template v-else-if="column.key === 'assistWithdrawalStatus'">
          <a
            v-if="hasPermission('member:orderuser:edit')"
            v-hasPermi="['member:orderuser:edit']"
            class="table-action-link"
            @click="handleToggleAssistWithdrawalStatus(record)"
          >
            {{ dictText(sys_enabled, record.assistWithdrawalStatus) }}
          </a>
          <span v-else>{{ dictText(sys_enabled, record.assistWithdrawalStatus) }}</span>
        </template>
        <template v-else-if="column.dict === 'yesNo'">
          <a-badge
            v-if="memberYesNoBadgeStatus(column.dataIndex, record[column.dataIndex])"
            class="member-status-badge"
            :status="memberYesNoBadgeStatus(column.dataIndex, record[column.dataIndex])"
            :text="dictText(user_yes_no, record[column.dataIndex])"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dict === 'enabled'">
          {{ dictText(sys_enabled, record[column.dataIndex]) }}
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) || '-' }}
        </template>
        <template v-else-if="column.key === 'withdrawalBlockRemark' || column.key === 'remarks'">
          {{ record[column.dataIndex] || '-' }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-flex wrap="wrap" gap="small">
              <a-button size="small" type="primary" class="ant-action-warning" @click="handleTransaction(record)" v-hasPermi="['member:orderuser:edit']">上下分</a-button>
              <a-button size="small" type="primary" @click="handleOpenLink(record)" v-hasPermi="['member:orderlink:list']">连单设置</a-button>
              <a-button
                size="small"
                type="primary"
                danger
                :loading="isMemberActionPending(record, 'resetOrder')"
                :disabled="isMemberActionPending(record, 'resetOrder')"
                @click="handleReset(record)"
                v-hasPermi="['member:orderuser:edit']"
              >重置单数</a-button>
              <a-button size="small" type="primary" @click="handleUpdate(record)" v-hasPermi="['member:orderuser:edit']">修 改</a-button>
              <a-button size="small" type="primary" @click="openModifyCount(record)" v-hasPermi="['member:orderuser:edit']">修改单数</a-button>
              <a-button size="small" type="primary" class="ant-action-success" @click="handleOpenBonus(record)" v-hasPermi="['member:bonus:list']">彩金设置</a-button>
              <a-button size="small" type="primary" @click="handleModifyLoginPassword(record)" v-hasPermi="['member:orderuser:edit']">修改登录密码</a-button>
              <a-button size="small" type="primary" @click="handleModifyTradePassword(record)" v-hasPermi="['member:orderuser:edit']">修改交易密码</a-button>
              <a-button size="small" type="primary" @click="handleOpenFlow(record)" v-hasPermi="['member:flow:list']">查看交易流水</a-button>
              <a-button size="small" type="primary" @click="handleOpenWithdrawal(record)" v-hasPermi="['member:withdrawalAcc:list']">修改提现账户</a-button>
              <a-button size="small" type="primary" @click="handleModifyReputation(record)" v-hasPermi="['member:orderuser:edit']">修改信誉分</a-button>
              <a-button size="small" type="primary" @click="handleModifyParent(record)" v-hasPermi="['member:orderuser:edit']">修改上级</a-button>
              <a-button size="small" type="primary" @click="handleModifyVip(record)" v-hasPermi="['member:orderuser:edit']">修改等级</a-button>
              <a-dropdown
                v-if="hasAnyPermission(moreActionPermissions)"
                :trigger="['hover']"
              >
                <a-button size="small" type="primary">
                  更多
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleCopyMember(record)" v-hasPermi="['member:orderuser:add']">复制</a-menu-item>
                    <a-menu-item @click="handleGift(record)" v-hasPermi="['member:orderuser:edit']">赠送</a-menu-item>
                    <a-menu-item @click="handleSubMembers(record)" v-hasPermi="['member:orderuser:query']">查看下级会员</a-menu-item>
                    <a-menu-item @click="handleModifySignDays(record)" v-hasPermi="['member:orderuser:edit']">修改签到天数</a-menu-item>
                    <a-menu-item @click="handleOrderDetails(record)" v-hasPermi="['member:orderinfo:list']">查看订单明细</a-menu-item>
                    <a-menu-item @click="handleExtraCommission(record)" v-hasPermi="['member:extracommission:list']">额外佣金设置</a-menu-item>
                    <a-menu-item :disabled="isMemberActionPending(record, 'isFake')" @click="handleToggleFake(record)" v-hasPermi="['member:orderuser:edit']">{{ buildFakeMemberToggle(record.isFake).action }}</a-menu-item>
                    <a-menu-item :disabled="isMemberActionPending(record, 'accountStatus')" @click="handleToggleAccountStatus(record)" v-hasPermi="['member:orderuser:edit']">{{ isValueOne(record.accountStatus) ? '\u542f\u7528' : '\u7981\u7528' }}账户</a-menu-item>
                    <a-menu-item :disabled="isMemberActionPending(record, 'transactionStatus')" @click="handleToggleTransactionStatus(record)" v-hasPermi="['member:orderuser:edit']">{{ isValueOne(record.transactionStatus) ? '\u542f\u7528' : '\u7981\u7528' }}交易</a-menu-item>
                    <a-menu-item :disabled="isMemberActionPending(record, 'withdrawalStatus')" @click="handleToggleWithdrawalStatus(record)" v-hasPermi="['member:orderuser:edit']">{{ isValueOne(record.withdrawalStatus) ? '\u542f\u7528' : '\u7981\u7528' }}提现</a-menu-item>
                    <a-menu-item :disabled="isMemberActionPending(record, 'assistWithdrawalStatus')" @click="handleToggleAssistWithdrawalStatus(record)" v-hasPermi="['member:orderuser:edit']">{{ isValueOne(record.assistWithdrawalStatus) ? '\u542f\u7528' : '\u7981\u7528' }}协助金提现</a-menu-item>
                    <a-menu-item @click="handleEditIdentity(record)" v-hasPermi="['member:orderuser:edit']">编辑身份信息</a-menu-item>
                    <a-menu-item @click="handleEditContract(record)" v-hasPermi="['member:orderuser:edit']">编辑合同</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
          </a-flex>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-model:open="modifyModalVisible"
      title="修改单数"
      width="416px"
      :mask-closable="false"
      :closable="!modifySubmitting"
      :keyboard="!modifySubmitting"
      :confirm-loading="modifySubmitting"
      :cancel-button-props="{ disabled: modifySubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitModifyCount"
      @cancel="closeModifyCount"
    >
      <a-form ref="modifyFormRef" :model="modifyForm" :rules="modifyRules" layout="vertical">
        <a-form-item label="任务进度">
          <a-input
            v-model:value="modifyForm.orderCount"
            placeholder="任务进度"
            disabled
            class="full-width"
          />
        </a-form-item>
        <a-form-item label="单数" name="taskProgress">
          <a-input-number
            v-model:value="modifyForm.taskProgress"
            :min="0"
            :precision="0"
            placeholder="单数"
            class="full-width"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改登录密码对话框 -->
    <a-modal
      v-model:open="loginPasswordVisible"
      title="修改登录密码"
      width="416px"
      :mask-closable="false"
      :closable="!loginSubmitting"
      :keyboard="!loginSubmitting"
      :confirm-loading="loginSubmitting"
      :cancel-button-props="{ disabled: loginSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitLoginPassword"
      @cancel="closeLoginPassword"
    >
      <a-form ref="loginFormRef" :model="loginForm" :rules="loginRules" layout="vertical">
        <a-form-item label="登录密码" name="password">
          <a-input-password
            v-model:value="loginForm.password"
            placeholder="请输入新登录密码（不少于6位）"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改交易密码对话框 -->
    <a-modal
      v-model:open="tradePasswordVisible"
      title="修改交易密码"
      width="416px"
      :mask-closable="false"
      :closable="!tradeSubmitting"
      :keyboard="!tradeSubmitting"
      :confirm-loading="tradeSubmitting"
      :cancel-button-props="{ disabled: tradeSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitTradePassword"
      @cancel="closeTradePassword"
    >
      <a-form ref="tradeFormRef" :model="tradeForm" :rules="tradeRules" layout="vertical">
        <a-form-item label="交易密码" name="tradePassword">
          <a-input-password
            v-model:value="tradeForm.tradePassword"
            placeholder="请输入新交易密码（不少于6位）"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改上级对话框 -->
    <a-modal
      v-model:open="parentVisible"
      title="修改上级"
      width="416px"
      :mask-closable="false"
      :closable="!parentSubmitting"
      :keyboard="!parentSubmitting"
      :confirm-loading="parentSubmitting"
      :cancel-button-props="{ disabled: parentSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitModifyParent"
      @cancel="closeModifyParent"
    >
      <a-form ref="parentFormRef" :model="parentForm" :rules="parentRules" layout="vertical">
        <a-form-item label="用户层级">
          <a-radio-group v-model:value="parentTopLevel" @change="handleParentTypeChange">
            <a-radio :value="true">顶级用户</a-radio>
            <a-radio :value="false">下级用户</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="上级邀请码" name="parentInviteCode" :required="!parentTopLevel">
          <a-input
            v-model:value="parentForm.parentInviteCode"
            :disabled="parentTopLevel"
            :placeholder="parentTopLevel ? '顶级用户无需填写' : '请输入上级邀请码'"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改等级对话框 -->
    <a-modal
      v-model:open="vipVisible"
      title="修改等级"
      width="416px"
      :mask-closable="false"
      :closable="!vipSubmitting"
      :keyboard="!vipSubmitting"
      :confirm-loading="vipSubmitting"
      :cancel-button-props="{ disabled: vipSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitModifyVip"
      @cancel="closeModifyVip"
    >
      <a-form ref="vipFormRef" :model="vipForm" :rules="vipRules" layout="vertical">
        <a-form-item label="VIP等级" name="vipId">
          <a-select v-model:value="vipForm.vipId" placeholder="请选择VIP等级" allow-clear>
            <a-select-option v-for="item in levelList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改信誉分对话框 -->
    <a-modal
      v-model:open="reputationVisible"
      title="修改信誉分"
      width="416px"
      :mask-closable="false"
      :closable="!reputationSubmitting"
      :keyboard="!reputationSubmitting"
      :confirm-loading="reputationSubmitting"
      :cancel-button-props="{ disabled: reputationSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitModifyReputation"
      @cancel="closeModifyReputation"
    >
      <a-form ref="reputationFormRef" :model="reputationForm" :rules="reputationRules" layout="vertical">
        <a-form-item label="当前信誉分">
          <a-input v-model:value="reputationForm.currentReputation" disabled />
        </a-form-item>
        <a-form-item label="信誉分" name="newReputation">
          <a-input-number v-model:value="reputationForm.newReputation" :min="0" :max="100" class="full-width" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="signDaysVisible"
      title="修改签到天数"
      width="416px"
      :mask-closable="false"
      :closable="!signDaysSubmitting"
      :keyboard="!signDaysSubmitting"
      :confirm-loading="signDaysSubmitting"
      :cancel-button-props="{ disabled: signDaysSubmitting }"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitModifySignDays"
      @cancel="closeModifySignDays"
    >
      <a-form ref="signDaysFormRef" :model="signDaysForm" :rules="signDaysRules" layout="vertical">
        <a-form-item label="当前签到天数">
          <a-input-number v-model:value="signDaysForm.currentSignDays" :min="0" disabled class="full-width" />
        </a-form-item>
        <a-form-item label="签到天数" name="signDays">
          <a-input-number v-model:value="signDaysForm.signDays" :min="0" :precision="0" class="full-width" />
        </a-form-item>
        <a-form-item label="是否包含今日签到" name="includeToday">
          <a-radio-group v-model:value="signDaysForm.includeToday">
            <a-radio value="0">否</a-radio>
            <a-radio value="1">是</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 添加或修改订单用户对话框 -->
    <orderuser-form
      v-model="open"
      :title="title"
      :form-data="form"
      :level-list="levelList"
      :readonly="formReadonly"
      @success="getList"
    />
    <orderuser-transaction-modal
      v-model="transactionModalVisible"
      :user-id="transactionUserId"
      @success="getList"
    />
    <orderlink-drawer
      v-model="linkDrawerVisible"
      :user-id="linkDrawerUserId"
      @success="getList"
    />
    <orderuser-bonus-drawer
      v-model="bonusDrawerVisible"
      :user-id="bonusDrawerUserId"
      @success="getList"
    />
    <orderuser-extracommission-drawer
      v-model="extracommissionDrawerVisible"
      :user-id="extracommissionDrawerUserId"
      @success="getList"
    />
    <orderuser-orderinfo-drawer
      v-model="orderinfoDrawerVisible"
      :user-id="orderinfoDrawerUserId"
      @success="getList"
    />
    <orderuser-flow-drawer
      v-model="flowDrawerVisible"
      :user-id="flowDrawerUserId"
      @success="getList"
    />
    <orderuser-withdrawal-drawer
      v-model="withdrawalDrawerVisible"
      :user-id="withdrawalDrawerUserId"
      @success="getList"
    />
    <orderuser-gift-modal
      v-model="giftVisible"
      :user-id="giftUserId"
      @success="getList"
    />
    <orderuser-sub-drawer
      v-model="subDrawerVisible"
      :user-id="subDrawerUserId"
      @open-flow="handleOpenFlow"
      @success="getList"
    />
    <orderuser-identity-modal
      v-model="identityVisible"
      :user-id="identityUserId"
      @success="getList"
    />
    <orderuser-contract-modal
      v-model="contractVisible"
      :user-id="contractUserId"
      @success="getList"
    />
  </div>
</template>

<script setup name="Orderuser">
import {
  listOrderuser,
  getOrderuser,
  delOrderuser,
  getLevel,
  updateOrderuser,
  resetOrder,
  editPassword,
  editTradePassword,
  editParentId,
  unlockOrderusers,
} from "@/api/member/orderuser";
import {
  CopyOutlined,
  DownOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  UnlockOutlined,
} from "@ant-design/icons-vue";
import OrderuserForm from "./components/OrderuserForm.vue";
import OrderuserTransactionModal from "./components/OrderuserTransactionModal.vue";
import OrderlinkDrawer from "./components/OrderlinkDrawer.vue";
import OrderuserBonusDrawer from "./components/OrderuserBonusDrawer.vue";
import OrderuserFlowDrawer from "./components/OrderuserFlowDrawer.vue";
import OrderuserWithdrawalDrawer from "./components/OrderuserWithdrawalDrawer.vue";
import OrderuserGiftModal from "./components/OrderuserGiftModal.vue";
import OrderuserSubDrawer from "./components/OrderuserSubDrawer.vue";
import OrderuserExtracommissionDrawer from "./components/OrderuserExtracommissionDrawer.vue";
import OrderuserOrderinfoDrawer from "./components/OrderuserOrderinfoDrawer.vue";
import OrderuserIdentityModal from "./components/OrderuserIdentityModal.vue";
import OrderuserContractModal from "./components/OrderuserContractModal.vue";
import {
  buildFakeMemberToggle,
  buildCopyMemberForm,
  buildModifyCountPayload,
} from "./components/orderuserFormPayload";
import {
  enabledBadgeStatus,
  fakeMemberTone,
  genderBadgeStatus,
  memberYesNoBadgeStatus,
  yesNoBadgeStatus,
} from "./memberCellPresentation";
import { normalizeMemberIdQuery } from "./memberQueryValidation";
import useUserStore from "@/store/modules/user";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const moreActionPermissions = [
  "member:orderuser:add",
  "member:orderuser:query",
  "member:orderuser:edit",
  "member:orderinfo:list",
  "member:extracommission:list",
];

function hasPermission(permission) {
  const permissions = userStore.permissions || [];
  return permissions.includes("*:*:*") || permissions.includes(permission);
}

function hasAnyPermission(permissions) {
  return permissions.some((permission) => hasPermission(permission));
}

const { user_yes_no, sys_user_sex, sys_enabled } = proxy.useDict(
  "user_yes_no",
  "sys_user_sex",
  "sys_enabled"
);

const orderuserList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const formReadonly = ref(false);
const levelList = ref([]);

const transactionModalVisible = ref(false);
const transactionUserId = ref(null);

const flowDrawerVisible = ref(false);
const flowDrawerUserId = ref(null);

const withdrawalDrawerVisible = ref(false);
const withdrawalDrawerUserId = ref(null);

const linkDrawerVisible = ref(false);
const linkDrawerUserId = ref(null);

const bonusDrawerVisible = ref(false);
const bonusDrawerUserId = ref(null);

const extracommissionDrawerVisible = ref(false);
const extracommissionDrawerUserId = ref(null);

const orderinfoDrawerVisible = ref(false);
const orderinfoDrawerUserId = ref(null);

const subDrawerVisible = ref(false);
const subDrawerUserId = ref(null);

const modifyModalVisible = ref(false);
const modifyForm = reactive({
  id: null,
  version: null,
  orderCount: null,
  taskProgress: null,
});
const modifySubmitting = ref(false);

const giftVisible = ref(false);
const giftUserId = ref(null);
const identityVisible = ref(false);
const identityUserId = ref(null);
const contractVisible = ref(false);
const contractUserId = ref(null);
const advancedSearchVisible = ref(false);

const modifyRules = reactive({
  taskProgress: [
    { required: true, message: "单数是必填项！", trigger: "change" },
    {
      validator: (_rule, value) => value == null || value === ""
        || (Number.isInteger(value) && value >= 0)
        ? Promise.resolve()
        : Promise.reject(new Error("请输入不小于 0 的整数")),
      trigger: "change",
    },
  ],
});

const modifyFormRef = ref(null);

// 修改登录密码相关
const loginPasswordVisible = ref(false);
const loginForm = reactive({
  id: null,
  password: null,
});
const requiredTrimmedRule = (label) => ({
  required: true,
  whitespace: true,
  message: `${label}是必填项！`,
  trigger: "blur",
});
const trimmedPasswordLengthRule = (label) => ({
  validator: (_rule, value) => {
    const password = String(value || "").trim();
    return !password || password.length >= 6
      ? Promise.resolve()
      : Promise.reject(new Error(`${label}长度不能少于6位`));
  },
  trigger: "blur",
});
const loginRules = reactive({
  password: [requiredTrimmedRule("登录密码"), trimmedPasswordLengthRule("登录密码")],
});
const loginFormRef = ref(null);
const loginSubmitting = ref(false);

// 修改交易密码相关
const tradePasswordVisible = ref(false);
const tradeForm = reactive({
  id: null,
  tradePassword: null,
});
const tradeRules = reactive({
  tradePassword: [requiredTrimmedRule("交易密码"), trimmedPasswordLengthRule("交易密码")],
});
const tradeFormRef = ref(null);
const tradeSubmitting = ref(false);

// 修改上级相关
const parentVisible = ref(false);
const parentTopLevel = ref(false);
const parentForm = reactive({
  id: null,
  parentId: null,
  parentInviteCode: null,
});
const parentRules = reactive({
  parentInviteCode: [{
    validator: (_rule, value) => parentTopLevel.value || String(value || "").trim()
      ? Promise.resolve()
      : Promise.reject(new Error("请输入上级邀请码")),
    trigger: "blur",
  }],
});
const parentFormRef = ref(null);
const parentSubmitting = ref(false);

// 修改等级相关
const vipVisible = ref(false);
const vipForm = reactive({
  id: null,
  version: null,
  vipId: null,
});
const vipRules = reactive({
  vipId: [{ required: true, message: "请选择VIP等级", trigger: "change" }],
});
const vipFormRef = ref(null);
const vipSubmitting = ref(false);

// 修改信誉分相关
const reputationVisible = ref(false);
const reputationForm = reactive({
  id: null,
  version: null,
  currentReputation: null,
  newReputation: null,
});
const reputationRules = reactive({
  newReputation: [
    { required: true, message: "信誉分是必填项！", trigger: "change" },
    {
      validator: (_rule, value) => value == null || value === ""
        || (Number.isInteger(value) && value >= 0 && value <= 100)
        ? Promise.resolve()
        : Promise.reject(new Error("请输入 0 到 100 之间的整数")),
      trigger: "change",
    },
  ],
});
const reputationFormRef = ref(null);
const reputationSubmitting = ref(false);

const signDaysVisible = ref(false);
const signDaysFormRef = ref(null);
const signDaysForm = reactive({
  id: null,
  version: null,
  currentSignDays: 0,
  signDays: 0,
  includeToday: "0",
  todaySignCount: 0,
  totalSignDays: 0,
});
const nonNegativeIntegerRule = {
  validator: (_rule, value) => Number.isInteger(value) && value >= 0
    ? Promise.resolve()
    : Promise.reject(new Error("请输入不小于 0 的整数")),
  trigger: "change",
};
const signDaysRules = reactive({
  signDays: [nonNegativeIntegerRule],
  includeToday: [{ required: true, message: "请选择是否包含今日签到", trigger: "change" }],
});
const signDaysSubmitting = ref(false);
const pendingMemberActions = reactive(new Set());
let memberFormRequestSequence = 0;
let listRequestSequence = 0;

const advancedSelectFields = computed(() => [
  { key: "gender", label: "性别", options: sys_user_sex.value || [] },
  { key: "isEnabled", label: "是否启用", options: sys_enabled.value || [] },
  { key: "allowInvite", label: "允许邀请", options: user_yes_no.value || [] },
  { key: "isFrozen", label: "是否冻结", options: user_yes_no.value || [] },
  { key: "isFake", label: "是否假人", options: user_yes_no.value || [] },
  { key: "isBanned", label: "禁止工作", options: user_yes_no.value || [] },
  { key: "isWithdrawalNotification", label: "关闭提现通知", options: user_yes_no.value || [] },
  { key: "productMatching", label: "产品匹配", options: sys_enabled.value || [] },
  { key: "accountStatus", label: "账户状态", options: sys_enabled.value || [] },
  { key: "transactionStatus", label: "交易状态", options: sys_enabled.value || [] },
  { key: "withdrawalStatus", label: "提现状态", options: sys_enabled.value || [] },
  { key: "assistWithdrawalStatus", label: "协助金提现状态", options: sys_enabled.value || [] },
  { key: "depositBlockWithdrawal", label: "充值后禁止提现", options: user_yes_no.value || [] },
  { key: "web3AuthEnabled", label: "启用Web3授权", options: user_yes_no.value || [] },
  { key: "isInvalid", label: "是否无效", options: user_yes_no.value || [] },
  { key: "isActivity", label: "是否活动", options: user_yes_no.value || [] },
  { key: "verifyIdentityBeforeTask", label: "任务开始前是否验证身份信息", options: user_yes_no.value || [] },
  { key: "userContractEnabled", label: "是否启用用户合同", options: user_yes_no.value || [] },
  { key: "userContractSigned", label: "是否签署用户合同", options: user_yes_no.value || [] },
  { key: "formalContractEnabled", label: "是否启用正式合同", options: user_yes_no.value || [] },
  { key: "formalContractSigned", label: "是否签署正式合同", options: user_yes_no.value || [] },
]);
const advancedSelectFieldsBeforeWorkLimit = computed(() => advancedSelectFields.value.slice(0, 6));
const advancedSelectFieldsAfterWorkLimit = computed(() => advancedSelectFields.value.slice(6));

function createDefaultQueryParams() {
  return {
    pageNum: 1,
    pageSize: 20,
    keyword: null,
    parentUsername: null,
    isOnline: null,
    id: null,
    vipId: null,
    usernameList: null,
    parentInviteCode: null,
    balanceMin: null,
    balanceMax: null,
    lastLoginIp: null,
    reputationMin: null,
    reputationMax: null,
    gender: null,
    isEnabled: null,
    allowInvite: null,
    isFrozen: null,
    isFake: null,
    isBanned: null,
    workLimit: null,
    isWithdrawalNotification: null,
    productMatching: null,
    accountStatus: null,
    transactionStatus: null,
    withdrawalStatus: null,
    assistWithdrawalStatus: null,
    depositBlockWithdrawal: null,
    web3AuthEnabled: null,
    isInvalid: null,
    isActivity: null,
    verifyIdentityBeforeTask: null,
    userContractEnabled: null,
    userContractSigned: null,
    formalContractEnabled: null,
    formalContractSigned: null,
    createTimeRange: [],
    orderByColumn: null,
    isAsc: null,
  };
}

const data = reactive({
  form: {},
  queryParams: createDefaultQueryParams(),
});

const { queryParams, form } = toRefs(data);

const sortableColumnKeys = new Set([
  "vip",
  "taskProgress",
  "reputationScore",
  "gender",
  "isEnabled",
  "allowInvite",
  "isFrozen",
  "isFake",
  "isBanned",
  "workLimit",
  "isWithdrawalNotification",
  "productMatching",
  "accountStatus",
  "transactionStatus",
  "withdrawalStatus",
  "assistWithdrawalStatus",
  "depositBlockWithdrawal",
  "web3AuthEnabled",
  "isInvalid",
  "isActivity",
  "withdrawalPasswordFailLimit",
  "withdrawalPasswordFailCount",
  "maxSingleWithdrawal",
  "verifyIdentityBeforeTask",
  "userContractEnabled",
  "userContractSigned",
  "formalContractEnabled",
  "formalContractSigned",
  "createTime",
]);

const memberColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80, fixed: "left" },
  { title: "用户名", dataIndex: "username", key: "username", width: 140 },
  { title: "手机号码", dataIndex: "phoneNumber", key: "phoneNumber", width: 140 },
  { title: "VIP等级", dataIndex: ["memberLevel", "name"], key: "vip", width: 100 },
  { title: "上级信息", key: "parentInfo", width: 180 },
  { title: "重置次数", key: "resetInfo", width: 180 },
  { title: "余额信息", key: "balanceInfo", width: 180 },
  { title: "任务进度", key: "taskProgress", width: 120 },
  { title: "完成组数", key: "completeGroupNum", width: 120 },
  { title: "登录信息", key: "loginInfo", width: 300 },
  { title: "签到信息", key: "signinInfo", width: 180 },
  { title: "统计信息", key: "statInfo", width: 200 },
  { title: "信誉分", dataIndex: "reputationScore", key: "reputationScore", width: 80 },
  { title: "邀请码", dataIndex: "inviteCode", key: "inviteCode", width: 100 },
  { title: "性别", dataIndex: "gender", key: "gender", width: 100 },
  { title: "邮箱", dataIndex: "email", key: "email", width: 180 },
  { title: "生日", dataIndex: "birthday", key: "birthday", width: 140 },
  { title: "是否启用", dataIndex: "isEnabled", key: "isEnabled", dict: "enabled", width: 100 },
  { title: "允许邀请", dataIndex: "allowInvite", key: "allowInvite", dict: "yesNo", width: 100 },
  { title: "是否冻结", dataIndex: "isFrozen", key: "isFrozen", dict: "yesNo", width: 100 },
  { title: "是否假人", dataIndex: "isFake", key: "isFake", dict: "yesNo", width: 100, align: "center" },
  { title: "禁止工作", dataIndex: "isBanned", key: "isBanned", dict: "yesNo", width: 100 },
  { title: "工作限额", dataIndex: "workLimit", key: "workLimit", width: 100 },
  { title: "关闭提现通知", dataIndex: "isWithdrawalNotification", key: "isWithdrawalNotification", dict: "yesNo", width: 140 },
  { title: "产品匹配", dataIndex: "productMatching", key: "productMatching", dict: "enabled", width: 160 },
  { title: "账户状态", dataIndex: "accountStatus", key: "accountStatus", dict: "enabled", width: 100 },
  { title: "交易状态", dataIndex: "transactionStatus", key: "transactionStatus", dict: "enabled", width: 100 },
  { title: "提现状态", dataIndex: "withdrawalStatus", key: "withdrawalStatus", dict: "enabled", width: 100 },
  { title: "协助金提现状态", dataIndex: "assistWithdrawalStatus", key: "assistWithdrawalStatus", dict: "enabled", width: 140 },
  { title: "充值后禁止提现", dataIndex: "depositBlockWithdrawal", key: "depositBlockWithdrawal", dict: "yesNo", width: 140 },
  { title: "启用Web3授权", dataIndex: "web3AuthEnabled", key: "web3AuthEnabled", dict: "yesNo", width: 140 },
  { title: "是否无效", dataIndex: "isInvalid", key: "isInvalid", dict: "yesNo", width: 100 },
  { title: "是否活动", dataIndex: "isActivity", key: "isActivity", dict: "yesNo", width: 100 },
  { title: "禁止客户提现所需交易密码失败次数(0-不限制)", dataIndex: "withdrawalPasswordFailLimit", key: "withdrawalPasswordFailLimit", width: 160 },
  { title: "禁止客户提现交易密码连续失败次数", dataIndex: "withdrawalPasswordFailCount", key: "withdrawalPasswordFailCount", width: 160 },
  { title: "单次最大提现金额(0-不限制)", dataIndex: "maxSingleWithdrawal", key: "maxSingleWithdrawal", width: 160 },
  { title: "任务开始前是否验证身份信息", dataIndex: "verifyIdentityBeforeTask", key: "verifyIdentityBeforeTask", dict: "yesNo", width: 160 },
  { title: "是否启用用户合同", dataIndex: "userContractEnabled", key: "userContractEnabled", dict: "yesNo", width: 160 },
  { title: "是否签署用户合同", dataIndex: "userContractSigned", key: "userContractSigned", dict: "yesNo", width: 160 },
  { title: "是否启用正式合同", dataIndex: "formalContractEnabled", key: "formalContractEnabled", dict: "yesNo", width: 200 },
  { title: "是否签署正式合同", dataIndex: "formalContractSigned", key: "formalContractSigned", dict: "yesNo", width: 200 },
  { title: "禁止提现备注", dataIndex: "withdrawalBlockRemark", key: "withdrawalBlockRemark", width: 200 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170 },
  { title: "备注", dataIndex: "remarks", key: "remarks", width: 200 },
  { title: "操作", key: "operation", width: 300, fixed: "right" },
].map((column) => sortableColumnKeys.has(column.key)
  ? { ...column, sorter: true }
  : column);

const rowSelection = computed(() => ({
  columnWidth: 32,
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => {
    handleSelectionChange(selectedRows);
  },
}));

function dictText(options, value) {
  return proxy.selectDictLabel(options, value) || value || "-";
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

const sortColumnMap = {
  vip: "gml.level",
  taskProgress: "ou.taskProgress",
  reputationScore: "ou.reputationScore",
  gender: "ou.gender",
  isEnabled: "ou.isEnabled",
  allowInvite: "ou.allowInvite",
  isFrozen: "ou.isFrozen",
  isFake: "ou.isFake",
  isBanned: "ou.isBanned",
  workLimit: "ou.workLimit",
  isWithdrawalNotification: "ou.isWithdrawalNotification",
  productMatching: "ou.productMatching",
  accountStatus: "ou.accountStatus",
  transactionStatus: "ou.transactionStatus",
  withdrawalStatus: "ou.withdrawalStatus",
  assistWithdrawalStatus: "ou.assistWithdrawalStatus",
  depositBlockWithdrawal: "ou.depositBlockWithdrawal",
  web3AuthEnabled: "ou.web3AuthEnabled",
  isInvalid: "ou.isInvalid",
  isActivity: "ou.isActivity",
  withdrawalPasswordFailLimit: "ou.withdrawalPasswordFailLimit",
  withdrawalPasswordFailCount: "ou.withdrawalPasswordFailCount",
  maxSingleWithdrawal: "ou.maxSingleWithdrawal",
  verifyIdentityBeforeTask: "ou.verifyIdentityBeforeTask",
  userContractEnabled: "ou.userContractEnabled",
  userContractSigned: "ou.userContractSigned",
  formalContractEnabled: "ou.formalContractEnabled",
  formalContractSigned: "ou.formalContractSigned",
  createTime: "ou.createTime",
};

function handleTableChange(_pagination, _filters, sorter) {
  const columnKey = sorter?.columnKey;
  queryParams.value.orderByColumn = sorter?.order ? sortColumnMap[columnKey] || null : null;
  queryParams.value.isAsc = sorter?.order === "ascend"
    ? "asc"
    : sorter?.order === "descend"
      ? "desc"
      : null;
  queryParams.value.pageNum = 1;
  getList();
}

function completeGroupText(record) {
  const completed = record.completeGroupNum ?? record.completedGroupNum ?? record.completedGroups ?? 0;
  const configuredLimit = record.completeGroupLimit
    ?? record.totalGroupNum
    ?? record.memberLevel?.taskCountPerDay
    ?? 0;
  const hasStarted = Number(record.taskProgress || 0) > 0
    || Number(record.todayRest || 0) > 0
    || Number(completed || 0) > 0;
  return `${completed} / ${hasStarted ? configuredLimit : 0}`;
}

/** 查询订单用户列表 */
function getList() {
  const requestSequence = ++listRequestSequence;
  loading.value = true;
  const requestParams = {
    ...queryParams.value,
    params: {
      beginTime: queryParams.value.createTimeRange?.[0] || null,
      endTime: queryParams.value.createTimeRange?.[1] || null,
    },
  };
  delete requestParams.createTimeRange;
  listOrderuser(requestParams)
    .then((response) => {
      if (requestSequence !== listRequestSequence) return;
      orderuserList.value = response.rows;
      total.value = response.total;
    })
    .catch((error) => {
      if (requestSequence === listRequestSequence) {
        orderuserList.value = [];
        total.value = 0;
        proxy.$modal.msgError(error?.message || "查询会员列表失败");
      }
    })
    .finally(() => {
      if (requestSequence === listRequestSequence) loading.value = false;
    });
  if (hasAnyPermission([
    "member:orderuser:query",
    "member:orderuser:add",
    "member:orderuser:edit",
  ])) {
    getLevel().then((res) => {
      levelList.value = res.data;
    });
  } else {
    levelList.value = [];
  }
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    username: null,
    phoneNumber: null,
    avatar: null,
    vipId: null,
    parentId: null,
    parentInviteCode: null,
    balance: null,
    frozenBalance: null,
    baseSalary: null,
    taskProgress: null,
    reputationScore: 100,
    inviteCode: null,
    gender: "0",
    email: null,
    birthday: null,
    isEnabled: "0",
    allowInvite: "0",
    isFrozen: "1",
    isFake: "0",
    isBanned: "1",
    workLimit: 0,
    isWithdrawalNotification: "1",
    productMatching: "1",
    accountStatus: "0",
    transactionStatus: "0",
    withdrawalStatus: "0",
    depositBlockWithdrawal: "1",
    remarks: null,
    version: null,
    createTime: null,
    updateTime: null,
    password: null,
    tradePassword: null,
    assistWithdrawalStatus: "0",
    web3AuthEnabled: "1",
    isInvalid: "1",
    isActivity: "0",
    withdrawalPasswordFailLimit: 0,
    withdrawalPasswordFailCount: 0,
    maxSingleWithdrawal: 0,
    verifyIdentityBeforeTask: "1",
    userContractEnabled: "1",
    userContractSigned: "1",
    formalContractEnabled: "1",
    formalContractSigned: "1",
    withdrawalBlockRemark: null,
    signDays: 0,
    todaySignCount: 0,
    totalSignDays: 0,
  };
}

function updateMemberIdQuery(value) {
  if (value === null || value === undefined || String(value).trim() === "") {
    queryParams.value.id = null;
    return;
  }

  const normalized = normalizeMemberIdQuery(value);
  if (normalized) queryParams.value.id = normalized;
}

/** 搜索按钮操作 */
function handleQuery() {
  if (queryParams.value.id !== null && queryParams.value.id !== undefined) {
    const normalized = normalizeMemberIdQuery(queryParams.value.id);
    if (!normalized) {
      proxy.$modal.msgWarning("ID 请输入有效的正整数");
      return;
    }
    queryParams.value.id = normalized;
  }
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  Object.assign(queryParams.value, createDefaultQueryParams());
  handleQuery();
}

function handleUnlock() {
  const selectedIds = [...ids.value];
  proxy.$modal.confirm(`是否确认解冻选中的 ${selectedIds.length} 个会员登录？`)
    .then(() => unlockOrderusers(selectedIds))
    .then(() => {
      proxy.$modal.msgSuccess("登录解冻成功");
      getList();
    })
    .catch(() => {});
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  memberFormRequestSequence += 1;
  reset();
  formReadonly.value = false;
  open.value = true;
  title.value = "创建";
}

/** 查看用户 */
function handleView(row) {
  const requestSequence = ++memberFormRequestSequence;
  reset();
  getOrderuser(row.id).then((response) => {
    if (requestSequence !== memberFormRequestSequence) return;
    form.value = response.data;
    form.value.birthday = response.data.birthday
      ? proxy.parseTime(response.data.birthday, "{y}-{m}-{d}")
      : null;
    form.value.password = "********";
    form.value.tradePassword = "********";
    formReadonly.value = true;
    open.value = true;
    title.value = "查看";
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const requestSequence = ++memberFormRequestSequence;
  reset();
  formReadonly.value = false;
  const _id = row.id || ids.value;
  getOrderuser(_id).then((response) => {
    if (requestSequence !== memberFormRequestSequence) return;
    form.value = response.data;
    form.value.birthday = response.data.birthday
      ? proxy.parseTime(response.data.birthday, "{y}-{m}-{d}")
      : null;
    form.value.password = null;
    form.value.tradePassword = null;
    open.value = true;
    title.value = "修改";
  });
}

/** 上下分弹窗 */
function handleTransaction(row) {
  const _id = row.id || ids.value;
  transactionUserId.value = _id;
  transactionModalVisible.value = true;
}

/** 查看交易流水抽屉 */
function handleOpenFlow(rowOrId) {
  // 支持接收 row 对象或直接的 id
  const maybeId = typeof rowOrId === "object" && rowOrId ? rowOrId.id : rowOrId;
  const _id = maybeId || ids.value;
  flowDrawerUserId.value = _id;
  flowDrawerVisible.value = true;
}

/** 连单设置抽屉 */
function handleOpenLink(row) {
  const _id = row.id || ids.value;
  linkDrawerUserId.value = _id;
  linkDrawerVisible.value = true;
}

/** 彩金设置抽屉 */
function handleOpenBonus(row) {
  const _id = row.id || ids.value;
  bonusDrawerUserId.value = _id;
  bonusDrawerVisible.value = true;
}

/** 修改提现账户抽屉 */
function handleOpenWithdrawal(row) {
  const _id = row.id || ids.value;
  withdrawalDrawerUserId.value = _id;
  withdrawalDrawerVisible.value = true;
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除订单用户编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delOrderuser(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/orderuser/export",
    {
      ...queryParams.value,
    },
    `orderuser_${new Date().getTime()}.xlsx`
  );
}

function isValueOne(value) {
  return String(value ?? "") === "1";
}

function memberActionKey(row, field) {
  return `${field}:${row?.id ?? "unknown"}`;
}

function isMemberActionPending(row, field) {
  return pendingMemberActions.has(memberActionKey(row, field));
}

function memberTarget(row) {
  return row?.username || row?.phoneNumber || `ID ${row?.id}`;
}

async function runConfirmedMemberUpdate({ row, field, confirmText, payload, successMessage, failureMessage }) {
  const key = memberActionKey(row, field);
  if (pendingMemberActions.has(key)) return;
  pendingMemberActions.add(key);
  let confirmed = false;
  try {
    await proxy.$modal.confirm(confirmText);
    confirmed = true;
    await updateOrderuser(payload);
    proxy.$modal.msgSuccess(successMessage);
    getList();
  } catch (error) {
    if (confirmed) proxy.$modal.msgError(error?.message || failureMessage);
  } finally {
    pendingMemberActions.delete(key);
  }
}

async function runValidatedModalAction({
  formRef,
  submitting,
  visible,
  request,
  successMessage,
  failureMessage,
}) {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await formRef.value?.validate?.();
    await request();
    visible.value = false;
    proxy.$modal.msgSuccess(successMessage);
    getList();
  } catch (error) {
    if (!error?.errorFields) proxy.$modal.msgError(error?.message || failureMessage);
  } finally {
    submitting.value = false;
  }
}

function clearModalValidation(formRef) {
  nextTick(() => formRef.value?.clearValidate?.());
}

/** 重置订单数确认并调用接口 */
async function handleReset(row) {
  const _id = row.id || ids.value;
  const key = memberActionKey(row, "resetOrder");
  if (pendingMemberActions.has(key)) return;
  pendingMemberActions.add(key);
  let confirmed = false;
  try {
    await proxy.$modal.confirm(`是否确认重置会员“${memberTarget(row)}”的订单数量？`);
    confirmed = true;
    await resetOrder(_id);
    proxy.$modal.msgSuccess("重置成功");
    getList();
  } catch (error) {
    if (confirmed) proxy.$modal.msgError(error?.message || "重置失败，请刷新后重试");
  } finally {
    pendingMemberActions.delete(key);
  }
}

function openModifyCount(row) {
  modifyForm.id = row.id || ids.value;
  modifyForm.version = row.version ?? null;

  // 给 row.taskProgress 和 row.memberLevel.orderCountPerDay 设置默认值
  const taskProgress = row.taskProgress ?? 0; // 默认为0
  const orderCountPerDay = row.memberLevel?.orderCountPerDay ?? 0; // 默认为0，且要确保 row.memberLevel 存在

  modifyForm.orderCount = `${taskProgress} / ${orderCountPerDay}`;

  modifyForm.taskProgress = taskProgress;
  modifySubmitting.value = false;
  modifyModalVisible.value = true;
  clearModalValidation(modifyFormRef);
}

async function submitModifyCount() {
  await runValidatedModalAction({
    formRef: modifyFormRef,
    submitting: modifySubmitting,
    visible: modifyModalVisible,
    request: () => updateOrderuser(buildModifyCountPayload(modifyForm)),
    successMessage: "修改成功",
    failureMessage: "修改失败",
  });
}

function closeModifyCount() {
  if (modifySubmitting.value) return;
  modifyModalVisible.value = false;
  Object.assign(modifyForm, { id: null, version: null, orderCount: null, taskProgress: null });
  clearModalValidation(modifyFormRef);
}

// 修改登录密码
function handleModifyLoginPassword(row) {
  loginForm.id = row.id;
  loginForm.password = null;
  loginSubmitting.value = false;
  loginPasswordVisible.value = true;
  clearModalValidation(loginFormRef);
}

async function submitLoginPassword() {
  await runValidatedModalAction({
    formRef: loginFormRef,
    submitting: loginSubmitting,
    visible: loginPasswordVisible,
    request: () => editPassword({
      id: loginForm.id,
      password: String(loginForm.password || "").trim(),
    }),
    successMessage: "修改登录密码成功",
    failureMessage: "修改登录密码失败",
  });
}

function closeLoginPassword() {
  if (loginSubmitting.value) return;
  loginPasswordVisible.value = false;
  Object.assign(loginForm, { id: null, password: null });
  clearModalValidation(loginFormRef);
}

// 修改交易密码
function handleModifyTradePassword(row) {
  tradeForm.id = row.id;
  tradeForm.tradePassword = null;
  tradeSubmitting.value = false;
  tradePasswordVisible.value = true;
  clearModalValidation(tradeFormRef);
}

async function submitTradePassword() {
  await runValidatedModalAction({
    formRef: tradeFormRef,
    submitting: tradeSubmitting,
    visible: tradePasswordVisible,
    request: () => editTradePassword({
        id: tradeForm.id,
        tradePassword: String(tradeForm.tradePassword || "").trim(),
      }),
    successMessage: "修改交易密码成功",
    failureMessage: "修改交易密码失败",
  });
}

function closeTradePassword() {
  if (tradeSubmitting.value) return;
  tradePasswordVisible.value = false;
  Object.assign(tradeForm, { id: null, tradePassword: null });
  clearModalValidation(tradeFormRef);
}

// 修改上级
function handleModifyParent(row) {
  parentForm.id = row.id;
  parentTopLevel.value = Number(row.parentId || 0) === 0;
  parentForm.parentId = row.parentId ?? null;
  parentForm.parentInviteCode = row.parentInviteCode || null;
  parentSubmitting.value = false;
  parentVisible.value = true;
  clearModalValidation(parentFormRef);
}

function handleParentTypeChange(event) {
  if (event.target.value) {
    parentForm.parentId = 0;
    parentForm.parentInviteCode = null;
    parentFormRef.value?.clearValidate?.("parentInviteCode");
    return;
  }
  parentForm.parentId = null;
}

async function submitModifyParent() {
  await runValidatedModalAction({
    formRef: parentFormRef,
    submitting: parentSubmitting,
    visible: parentVisible,
    request: () => editParentId({
        id: parentForm.id,
        parentId: parentTopLevel.value ? 0 : null,
        parentInviteCode: parentTopLevel.value
          ? null
          : String(parentForm.parentInviteCode || "").trim(),
      }),
    successMessage: "修改上级成功",
    failureMessage: "修改上级失败",
  });
}

function closeModifyParent() {
  if (parentSubmitting.value) return;
  parentVisible.value = false;
  parentTopLevel.value = false;
  Object.assign(parentForm, { id: null, parentId: null, parentInviteCode: null });
  clearModalValidation(parentFormRef);
}

// 修改等级
function handleModifyVip(row) {
  vipForm.id = row.id;
  vipForm.version = row.version ?? null;
  vipForm.vipId = row.vipId ?? null;
  vipSubmitting.value = false;
  vipVisible.value = true;
  clearModalValidation(vipFormRef);
}

async function submitModifyVip() {
  await runValidatedModalAction({
    formRef: vipFormRef,
    submitting: vipSubmitting,
    visible: vipVisible,
    request: () => updateOrderuser({ id: vipForm.id, version: vipForm.version, vipId: vipForm.vipId }),
    successMessage: "修改等级成功",
    failureMessage: "修改等级失败",
  });
}

function closeModifyVip() {
  if (vipSubmitting.value) return;
  vipVisible.value = false;
  Object.assign(vipForm, { id: null, version: null, vipId: null });
  clearModalValidation(vipFormRef);
}

// 修改信誉分
function handleModifyReputation(row) {
  reputationForm.id = row.id;
  reputationForm.version = row.version ?? null;
  reputationForm.currentReputation = row.reputationScore ?? 100;
  reputationForm.newReputation = row.reputationScore ?? 100;
  reputationSubmitting.value = false;
  reputationVisible.value = true;
  clearModalValidation(reputationFormRef);
}

async function submitModifyReputation() {
  await runValidatedModalAction({
    formRef: reputationFormRef,
    submitting: reputationSubmitting,
    visible: reputationVisible,
    request: () => updateOrderuser({
      id: reputationForm.id,
      version: reputationForm.version,
      reputationScore: reputationForm.newReputation,
      }),
    successMessage: "修改信誉分成功",
    failureMessage: "修改信誉分失败",
  });
}

function closeModifyReputation() {
  if (reputationSubmitting.value) return;
  reputationVisible.value = false;
  Object.assign(reputationForm, {
    id: null,
    version: null,
    currentReputation: null,
    newReputation: null,
  });
  clearModalValidation(reputationFormRef);
}

async function copyText(value) {
  const text = String(value || "");
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    proxy.$modal.msgSuccess("复制成功");
  } catch {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand("copy");
    textarea.remove();
    proxy.$modal.msgSuccess("复制成功");
  }
}

function handleCopyMember(row) {
  memberFormRequestSequence += 1;
  reset();
  formReadonly.value = false;
  const defaults = { ...form.value };
  form.value = buildCopyMemberForm(defaults, row || {}, {
    formatBirthday: (value) => proxy.parseTime(value, "{y}-{m}-{d}"),
  });
  open.value = true;
  title.value = "创建";
}

function handleEditIdentity(row) {
  identityUserId.value = row.id;
  identityVisible.value = true;
}

function handleEditContract(row) {
  contractUserId.value = row.id;
  contractVisible.value = true;
}

// 更多菜单操作
function handleGift(row) {
  giftUserId.value = row.id;
  giftVisible.value = true;
}

function handleSubMembers(row) {
  // 打开下级会员抽屉
  subDrawerUserId.value = row.id;
  subDrawerVisible.value = true;
}

function handleModifySignDays(row) {
  Object.assign(signDaysForm, {
    id: row.id,
    version: row.version ?? null,
    currentSignDays: Number(row.signDays || 0),
    signDays: Number(row.signDays || 0),
    includeToday: Number(row.todaySignCount || 0) > 0 ? "1" : "0",
    todaySignCount: Number(row.todaySignCount || 0),
    totalSignDays: Number(row.totalSignDays || 0),
  });
  signDaysSubmitting.value = false;
  signDaysVisible.value = true;
  clearModalValidation(signDaysFormRef);
}

async function submitModifySignDays() {
  await runValidatedModalAction({
    formRef: signDaysFormRef,
    submitting: signDaysSubmitting,
    visible: signDaysVisible,
    request: () => updateOrderuser({
      id: signDaysForm.id,
      version: signDaysForm.version,
      signDays: signDaysForm.signDays,
      todaySignCount: signDaysForm.includeToday === "1" ? Math.max(signDaysForm.todaySignCount, 1) : 0,
      totalSignDays: signDaysForm.totalSignDays,
    }),
    successMessage: "修改签到天数成功",
    failureMessage: "修改签到天数失败",
  });
}

function closeModifySignDays() {
  if (signDaysSubmitting.value) return;
  signDaysVisible.value = false;
  Object.assign(signDaysForm, {
    id: null,
    version: null,
    currentSignDays: 0,
    signDays: 0,
    includeToday: "0",
    todaySignCount: 0,
    totalSignDays: 0,
  });
  clearModalValidation(signDaysFormRef);
}

function handleOrderDetails(row) {
  const _id = row.id || ids.value;
  orderinfoDrawerUserId.value = _id;
  orderinfoDrawerVisible.value = true;
}

function handleExtraCommission(row) {
  const _id = row.id || ids.value;
  extracommissionDrawerUserId.value = _id;
  extracommissionDrawerVisible.value = true;
}

async function handleToggleFake(row) {
  const { value: newValue, action } = buildFakeMemberToggle(row.isFake);
  return runConfirmedMemberUpdate({
    row,
    field: "isFake",
    confirmText: `是否确认将会员“${memberTarget(row)}”${action}？`,
    payload: { id: row.id, version: row.version, isFake: newValue },
    successMessage: `${action}成功`,
    failureMessage: `${action}失败`,
  });
}

async function handleToggleProductMatching(row) {
  const newValue = String(row.productMatching ?? "") === "0" ? "1" : "0";
  const action = newValue === "0" ? "启用" : "禁用";
  return runConfirmedMemberUpdate({
    row,
    field: "productMatching",
    confirmText: `是否确认对会员“${memberTarget(row)}”${action}产品匹配？`,
    payload: { id: row.id, version: row.version, productMatching: newValue },
    successMessage: `${action}产品匹配成功`,
    failureMessage: `${action}产品匹配失败`,
  });
}

async function handleToggleAccountStatus(row) {
  const newValue = String(row.accountStatus ?? "") === "0" ? "1" : "0";
  const action = newValue === "0" ? "启用" : "禁用";
  return runConfirmedMemberUpdate({
    row,
    field: "accountStatus",
    confirmText: `是否确认对会员“${memberTarget(row)}”${action}账户？`,
    payload: { id: row.id, version: row.version, accountStatus: newValue },
    successMessage: `${action}账户成功`,
    failureMessage: `${action}账户失败`,
  });
}

async function handleToggleTransactionStatus(row) {
  const newValue = String(row.transactionStatus ?? "") === "0" ? "1" : "0";
  const action = newValue === "0" ? "启用" : "禁用";
  return runConfirmedMemberUpdate({
    row,
    field: "transactionStatus",
    confirmText: `是否确认对会员“${memberTarget(row)}”${action}交易？`,
    payload: { id: row.id, version: row.version, transactionStatus: newValue },
    successMessage: `${action}交易成功`,
    failureMessage: `${action}交易失败`,
  });
}

async function handleToggleWithdrawalStatus(row) {
  const newValue = String(row.withdrawalStatus ?? "") === "0" ? "1" : "0";
  const action = newValue === "0" ? "启用" : "禁用";
  return runConfirmedMemberUpdate({
    row,
    field: "withdrawalStatus",
    confirmText: `是否确认对会员“${memberTarget(row)}”${action}提现？`,
    payload: { id: row.id, version: row.version, withdrawalStatus: newValue },
    successMessage: `${action}提现成功`,
    failureMessage: `${action}提现失败`,
  });
}

async function handleToggleAssistWithdrawalStatus(row) {
  const newValue = String(row.assistWithdrawalStatus ?? "") === "0" ? "1" : "0";
  const action = newValue === "0" ? "启用" : "禁用";
  return runConfirmedMemberUpdate({
    row,
    field: "assistWithdrawalStatus",
    confirmText: `是否确认对会员“${memberTarget(row)}”${action}协助金提现？`,
    payload: { id: row.id, version: row.version, assistWithdrawalStatus: newValue },
    successMessage: `${action}协助金提现成功`,
    failureMessage: `${action}协助金提现失败`,
  });
}

getList();
</script>
<style scoped>
:global(body:has(.member-orderuser-page) .copyright) {
  display: none;
}

:global(body:has(.member-orderuser-page) .app-main) {
  padding-bottom: 0 !important;
}

.ant-pro-member-page {
  margin: 44px 40px 0;
}

.member-orderuser-page :deep(.ant-pro-query-form),
.member-orderuser-page :deep(.ant-pro-table-toolbar) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif,
    "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
}

.member-orderuser-page :deep(.ant-pro-query-form *),
.member-orderuser-page :deep(.ant-pro-table-toolbar *),
.member-orderuser-page :deep(.ant-table-tbody .ant-btn) {
  font-family: inherit;
}

.ant-pro-query-form :deep(.ant-form-item) {
  flex-wrap: nowrap;
  margin-bottom: 0;
}

.ant-pro-query-form :deep(.ant-form-item-control) {
  flex: 1 1 0 !important;
  max-width: calc(100% - 100px);
}

.ant-pro-query-actions {
  display: flex;
  justify-content: flex-end;
  margin-left: auto;
}

.ant-pro-expand-btn {
  padding-right: 0;
}

.ant-pro-expand-icon-open {
  transform: rotate(180deg);
}

.online-query-field-collapsed {
  display: none;
}

@media (min-width: 1600px) {
  .online-query-field-collapsed {
    display: block;
  }
}

.query-label-help {
  margin-left: 4px;
  color: rgba(0, 0, 0, 0.45);
}

.range-input {
  width: calc(50% - 20px);
}

.range-separator {
  width: 40px;
  padding: 0;
  text-align: center;
  pointer-events: none;
}

.negative-balance {
  color: #ff4d4f;
}

.copy-button {
  width: 24px;
  height: 24px;
  padding: 0;
  color: #1677ff;
}

.table-action-link {
  color: #1677ff;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s ease;
}

.table-action-link:hover,
.table-action-link:focus-visible {
  color: #4096ff;
}

.table-action-link:active {
  color: #0958d9;
}

.table-action-link:focus-visible {
  outline: 2px solid rgba(22, 119, 255, 0.25);
  outline-offset: 2px;
}

.fake-member-link {
  cursor: pointer;
  text-decoration: none;
}

.member-status-badge :deep(.ant-badge-status-text) {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  font-size: 14px;
  line-height: 22px;
}

.fake-member-success {
  color: #52c41a !important;
}

.fake-member-error {
  color: #ff4d4f !important;
}

.member-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  vertical-align: middle;
  background: #bfbfbf;
  border-radius: 50%;
}

.member-dot-online {
  background: #52c41a;
}

.member-orderuser-page :deep(.ant-table-thead > tr > th),
.member-orderuser-page :deep(.ant-table-tbody > tr > td) {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  font-size: 15px;
  line-height: 23.5714px;
}

.member-orderuser-page :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f0f0f0 !important;
}

.member-orderuser-page :deep(.ant-table-body) {
  min-height: calc(100vh - 440px);
}

.member-orderuser-page :deep(.ant-pro-query-form .ant-btn-primary),
.member-orderuser-page :deep(.ant-pro-table-toolbar .ant-btn-primary:not(:disabled)),
.member-orderuser-page :deep(.ant-table-tbody .ant-btn-primary:not(.ant-btn-dangerous):not(.ant-action-warning):not(.ant-action-success)) {
  background: #1890ff;
  border-color: #1890ff;
}

.member-orderuser-page :deep(.ant-pro-query-form .ant-btn-primary:hover),
.member-orderuser-page :deep(.ant-pro-query-form .ant-btn-primary:focus),
.member-orderuser-page :deep(.ant-pro-table-toolbar .ant-btn-primary:not(:disabled):hover),
.member-orderuser-page :deep(.ant-pro-table-toolbar .ant-btn-primary:not(:disabled):focus),
.member-orderuser-page :deep(.ant-table-tbody .ant-btn-primary:not(.ant-btn-dangerous):not(.ant-action-warning):not(.ant-action-success):hover),
.member-orderuser-page :deep(.ant-table-tbody .ant-btn-primary:not(.ant-btn-dangerous):not(.ant-action-warning):not(.ant-action-success):focus) {
  background: #40a9ff;
  border-color: #40a9ff;
}

.ant-action-warning {
  background: #faad14;
  border-color: #faad14;
}

.ant-action-warning:hover,
.ant-action-warning:focus {
  background: #ffc53d;
  border-color: #ffc53d;
}

.ant-action-success {
  background: #52c41a;
  border-color: #52c41a;
}

.ant-action-success:hover,
.ant-action-success:focus {
  background: #73d13d;
  border-color: #73d13d;
}

.full-width {
  width: 100%;
}

.other-item {
  font-weight: 600;
  color: #409eff !important;
}
.danger-item {
  font-weight: 600;
  color: #f56c6c !important;
}
.success-item {
  font-weight: 600;
  color: #67c23a !important;
}
</style>
