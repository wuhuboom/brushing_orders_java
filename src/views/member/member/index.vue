<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      v-show="showSearch"
      :inline="true"
      label-width="68px"
    >
      <el-form-item :label="$t('member.index.username')" prop="username">
        <el-input
          v-model="queryParams.username"
          :placeholder="$t('member.index.usernamePlaceholder')"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.index.phone')" prop="phone">
        <el-input
          v-model="queryParams.phone"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('member.index.inviteCode')" prop="inviteCode">
        <el-input
          v-model="queryParams.inviteCode"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('member.index.registerTime')"
        style="width: 400px"
      >
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('member.index.dateStart')"
          :end-placeholder="$t('member.index.dateEnd')"
        ></el-date-picker>
      </el-form-item>
      <el-form-item :label="$t('member.index.isFake')" prop="isReal">
        <el-select
          v-model="queryParams.isReal"
          :placeholder="$t('member.index.isFakePlaceholder')"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{
          $t("member.index.search")
        }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{
          $t("member.index.reset")
        }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['member:member:add']"
          >{{ $t("member.index.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:member:edit']"
          >{{ $t("member.index.edit") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:member:remove']"
          >{{ $t("member.index.delete") }}</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        :columns="columns"
        @queryTable="getList"
        @update:columns="handleColumnsUpdate"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="memberList"
      ref="tableRef"
      @selection-change="handleSelectionChange"
      :border="true"
      :key="tableKey"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        v-for="column in filteredColumns"
        :key="column.key"
        :label="column.label"
        :prop="column.prop"
        :width="column.width"
        :fixed="column.fixed"
        align="center"
      >
        <template v-if="column.key === '18'" #default="scope">
          <div>
            {{ scope.row.dealCount }}/{{ scope.row.userLevel.orderCount }}
          </div>
        </template>
        <template v-if="column.key === '21'" #default="{ row }">
          <div>
            <div v-for="(ip, index) in row.registerIp?.split(',')" :key="index">
              {{ ip }}
            </div>
          </div>
        </template>
        <template v-if="column.key === '22'" #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isReal" />
        </template>
        <template v-if="column.key === '23'" #default="scope">
          <dict-tag
            :options="sys_normal_disable"
            :value="scope.row.accountStatus"
          />
        </template>
        <template v-if="column.key === '24'" #default="scope">
          <dict-tag
            :options="sys_normal_disable"
            :value="scope.row.tradeStatus"
          />
        </template>
        <template v-if="column.key === '25'" #default="scope">
          <dict-tag
            :options="sys_normal_disable"
            :value="scope.row.withdrawStatus"
          />
        </template>
        <template v-if="column.key === '26'" #default="scope">
          <span>{{
            parseTime(scope.row.lastLoginTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
        <template v-if="column.key === '27'" #default="scope">
          <span>{{
            parseTime(scope.row.createTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
        <template v-if="column.key === '29'" #default="scope">
          <div class="operation-buttons">
            <el-button
              icon="Sort"
              type="warning"
              @click="handleTopup(scope.row)"
              >{{ $t("member.index.topup") }}</el-button
            >
            <el-button
              icon="List"
              type="primary"
              @click="handleSeries(scope.row)"
              >{{ $t("member.index.series") }}</el-button
            >
            <el-popconfirm
              class="box-item"
              :title="$t('member.index.resetOrderNumConfirm')"
              placement="top"
              @confirm="restOrderNum(scope.row)"
            >
              <template #reference>
                <el-button icon="Refresh" type="danger">{{
                  $t("member.index.resetOrderNum")
                }}</el-button>
              </template>
            </el-popconfirm>
            <el-dropdown trigger="click">
              <el-button type="primary">
                {{ $t("member.index.operation") }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    command="edit"
                    icon="Edit"
                    @click="handleUpdate(scope.row)"
                    >{{ $t("member.index.edit") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="account"
                    icon="Edit"
                    @click="openEditDealCountDialog(scope.row)"
                    >{{ $t("member.index.modifyOrderCount") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="account"
                    icon="Lock"
                    @click="openPasswordDialog(scope.row, 'login')"
                    >{{ $t("member.index.modifyPassword") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="account"
                    icon="Key"
                    @click="openPasswordDialog(scope.row, 'trade')"
                    >{{
                      $t("member.index.modifyTradePassword")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="changeParent"
                    icon="User"
                    @click="handleChangeParent(scope.row)"
                    >{{ $t("member.index.modifyParent") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="Money"
                    @click="openGiftBalanceDialog(scope.row)"
                    >{{ $t("member.index.giftBalance") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="Money"
                    @click="openAdjustBalanceDialog(scope.row)"
                    >{{ $t("member.index.modifyBalance") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="User"
                    @click="openSubList(scope.row)"
                    >{{ $t("member.index.subUsers") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="account"
                    icon="Money"
                    @click="handleAccount(scope.row)"
                    >{{ $t("member.index.accountChange") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="CreditCard"
                    @click="openWithdrawDialog(scope.row)"
                    >{{
                      $t("member.index.modifyWithdrawAccount")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="UserFilled"
                    @click="openRealNameDialog(scope.row, 'full')"
                    >{{ $t("member.index.modifyRealName") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="CircleCheck"
                    @click="openRealNameDialog(scope.row, 'status')"
                    >{{
                      $t("member.index.modifyRealNameStatus")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="Star"
                    @click="openCreditScoreDialog(scope.row)"
                    >{{
                      $t("member.index.modifyCreditScore")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="Edit"
                    @click="openWithdrawTipDialog(scope.row)"
                    >{{
                      $t("member.index.modifyWithdrawTip")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    icon="Edit"
                    @click="openRemarkDialog(scope.row)"
                    >{{ $t("member.index.modifyRemark") }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    :icon="
                      String(scope.row.accountStatus) === '0'
                        ? 'CircleClose'
                        : 'CircleCheck'
                    "
                    :style="{
                      color:
                        String(scope.row.accountStatus) === '0'
                          ? '#F56C6C'
                          : '#67C23A',
                    }"
                    @click="handleToggleAccountStatus(scope.row)"
                  >
                    {{
                      String(scope.row.accountStatus) === "0"
                        ? $t("member.index.accountDisable")
                        : $t("member.index.accountEnable")
                    }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    :icon="
                      String(scope.row.tradeStatus) === '0'
                        ? 'CircleClose'
                        : 'CircleCheck'
                    "
                    :style="{
                      color:
                        String(scope.row.tradeStatus) === '0'
                          ? '#F56C6C'
                          : '#67C23A',
                    }"
                    @click="handleToggleTradeStatus(scope.row)"
                  >
                    {{
                      String(scope.row.tradeStatus) === "0"
                        ? $t("member.index.tradeDisable")
                        : $t("member.index.tradeEnable")
                    }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    :icon="
                      String(scope.row.isReal) === 'Y'
                        ? 'CircleCheck'
                        : 'CircleClose'
                    "
                    :style="{
                      color:
                        String(scope.row.isReal) === 'Y'
                          ? '#67C23A'
                          : '#F56C6C',
                    }"
                    @click="handleToggleIsReal(scope.row)"
                  >
                    {{
                      String(scope.row.isReal) === "Y"
                        ? $t("member.index.setReal")
                        : $t("member.index.setFake")
                    }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    :icon="
                      String(scope.row.withdrawStatus) === '0'
                        ? 'CircleClose'
                        : 'CircleCheck'
                    "
                    :style="{
                      color:
                        String(scope.row.withdrawStatus) === '0'
                          ? '#F56C6C'
                          : '#67C23A',
                    }"
                    @click="handleToggleWithdrawStatus(scope.row)"
                  >
                    {{
                      String(scope.row.withdrawStatus) === "0"
                        ? $t("member.index.withdrawDisable")
                        : $t("member.index.withdrawEnable")
                    }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 其余对话框和抽屉保持不变 -->
    <el-dialog
      :title="$t('member.index.title')"
      v-model="open"
      width="680px"
      append-to-body
    >
      <el-form
        ref="memberRef"
        label-position="top"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('member.index.username')" prop="username">
              <el-input
                :disabled="isEdit"
                v-model="form.username"
                :placeholder="$t('member.index.enterUsername')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!isEdit">
            <el-form-item :label="$t('member.index.parentId')" prop="parentId">
              <el-input
                v-model="form.parentId"
                :placeholder="$t('member.index.enterParentId')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('member.index.phone')" prop="phone">
              <el-input
                v-model="form.phone"
                :placeholder="$t('member.index.enterPhone')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="isEdit">
            <el-form-item
              :label="$t('member.index.cardNumber')"
              prop="cardNumber"
            >
              <el-input-number
                v-model="form.cardNumber"
                :min="0"
                style="width: 240px"
                :max="999"
                :placeholder="$t('member.index.cardNumber')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="isEdit">
            <el-form-item
              :label="$t('member.index.cardAmount')"
              prop="cardAmount"
            >
              <el-input-number
                v-model="form.cardAmount"
                :min="0"
                style="width: 240px"
                :max="99999999"
                :placeholder="$t('member.index.cardAmount')"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('member.index.email')" prop="email">
              <el-input
                v-model="form.email"
                :placeholder="$t('member.index.enterEmail')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.index.loginPassword')"
              prop="password"
            >
              <el-input
                type="password"
                v-model="form.password"
                :placeholder="$t('member.index.enterLoginPassword')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.index.tradePassword')"
              prop="tradePassword"
            >
              <el-input
                type="password"
                v-model="form.tradePassword"
                :placeholder="$t('member.index.enterTradePassword')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.index.memberLevel')"
              prop="levelId"
            >
              <el-select
                v-model="form.levelId"
                :placeholder="$t('member.index.selectMemberLevel')"
                clearable
              >
                <el-option
                  v-for="item in LevelDatas"
                  :key="item.id"
                  :label="item.nameEn"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('member.index.isFake')" prop="isReal">
              <el-radio-group v-model="form.isReal">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.index.accountStatus')"
              prop="accountStatus"
            >
              <el-radio-group v-model="form.accountStatus">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('member.index.tradeStatus')"
              prop="tradeStatus"
            >
              <el-radio-group v-model="form.tradeStatus">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="cancel">{{ $t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 上下分对话框 -->
    <el-dialog
      :title="$t('member.index.topupTitle')"
      v-model="topupOpen"
      width="500px"
      append-to-body
    >
      <el-form
        ref="topupRef"
        :model="topupForm"
        :rules="topupRules"
        label-position="top"
        label-width="100px"
      >
        <el-form-item :label="$t('member.index.userBalance')" prop="balance">
          <el-input
            v-model="topupForm.balance"
            :placeholder="$t('member.index.currentBalance')"
            disabled
          />
        </el-form-item>
        <el-form-item :label="$t('member.index.operationType')" prop="type">
          <el-select
            v-model="topupForm.type"
            :placeholder="$t('member.index.selectOperationType')"
          >
            <el-option :label="$t('member.index.add')" value="0" />
            <el-option :label="$t('member.index.subtract')" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('member.index.amount')" prop="amount">
          <el-input-number
            v-model.number="topupForm.amount"
            style="width: 100%"
            :min="1"
            :placeholder="$t('member.index.enterAmount')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitTopupForm">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="cancelTopup">{{ $t("common.cancel") }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 连单操作 -->
    <el-drawer
      :title="$t('member.index.seriesTitle')"
      v-model="seriesOpen"
      direction="rtl"
      size="92%"
      append-to-body
    >
      <Series :userId="selectedUserId" />
    </el-drawer>

    <!-- 修改单数操作 -->
    <el-dialog
      v-model="dealDialogVisible"
      :title="$t('member.index.modifyOrderCountTitle')"
      width="500px"
    >
      <el-form
        ref="dealCountRef"
        :model="dealForm"
        label-width="100px"
        :rules="dealCountRules"
        label-position="top"
      >
        <el-form-item :label="$t('member.index.username')">
          <el-input v-model="dealForm.username" disabled />
        </el-form-item>
        <el-form-item :label="$t('member.index.currentOrderCount')">
          <el-input v-model="dealForm.currentDealCount" disabled />
        </el-form-item>
        <el-form-item
          :label="$t('member.index.orderCount')"
          prop="newDealCount"
        >
          <el-input-number
            v-model="dealForm.newDealCount"
            :min="0"
            :placeholder="$t('member.index.enterNewOrderCount')"
            style="width: 300px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dealDialogVisible = false">{{
          $t("common.cancel")
        }}</el-button>
        <el-button type="primary" @click="submitDealCount">{{
          $t("member.index.submit")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码操作 -->
    <el-dialog
      v-model="passwordDialogVisible"
      :title="passwordDialogTitle"
      width="500px"
    >
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item
          :label="$t('member.index.newPassword')"
          prop="newPassword"
        >
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            :placeholder="$t('member.index.enterNewPassword')"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{
          $t("common.cancel")
        }}</el-button>
        <el-button type="primary" @click="submitPassword">{{
          $t("member.index.submit")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 用户账单 -->
    <el-drawer
      :title="$t('member.index.userAccountTitle')"
      v-model="accountOpen"
      direction="rtl"
      size="92%"
      append-to-body
    >
      <Account :userId="selectedUserId" />
    </el-drawer>

    <!-- 新增修改上级对话框 -->
    <el-dialog
      :title="$t('member.index.modifyParentTitle')"
      v-model="changeParentOpen"
      width="500px"
      append-to-body
    >
      <el-form
        ref="changeParentRef"
        :model="changeParentForm"
        :rules="changeParentRules"
        label-position="top"
        label-width="100px"
      >
        <el-form-item :label="$t('member.index.parentId')" prop="parentId">
          <el-input-number
            v-model="changeParentForm.parentId"
            :min="0"
            :placeholder="$t('member.index.enterParentId')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitChangeParent">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="cancelChangeParent">{{
            $t("common.cancel")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 赠送余额弹框 -->
    <el-dialog
      :title="$t('member.index.giftBalanceTitle')"
      v-model="giftBalanceDialogVisible"
      width="500px"
    >
      <el-form
        ref="giftBalanceRef"
        :model="giftBalanceForm"
        :rules="giftBalancetRules"
        label-width="120px"
        label-position="top"
      >
        <el-form-item :label="$t('member.index.currentBalance')">
          <el-input v-model="giftBalanceForm.currentBalance" disabled />
        </el-form-item>
        <el-form-item :label="$t('member.index.giftAmount')" prop="giftAmount">
          <el-input-number
            v-model="giftBalanceForm.giftAmount"
            :min="0"
            :placeholder="$t('member.index.enterGiftAmount')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="confirmGiftBalance">{{
            $t("common.confirm")
          }}</el-button>
          <el-button @click="giftBalanceDialogClose">{{
            $t("common.cancel")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改余额弹框 -->
    <el-dialog
      :title="$t('member.index.modifyBalanceTitle')"
      v-model="adjustBalanceDialogVisible"
      width="500px"
    >
      <el-form
        ref="adjustBalanceRef"
        :model="adjustBalanceForm"
        :rules="adjustBalanceRules"
        label-width="120px"
        label-position="top"
      >
        <el-form-item :label="$t('member.index.currentBalance')">
          <el-input v-model="adjustBalanceForm.currentBalance" disabled />
        </el-form-item>
        <el-form-item
          :label="$t('member.index.modifyBalance')"
          prop="newBalance"
        >
          <el-input-number
            v-model="adjustBalanceForm.newBalance"
            :min="0"
            :placeholder="$t('member.index.enterNewBalance')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustBalanceDialogClose">{{
            $t("common.cancel")
          }}</el-button>
          <el-button type="primary" @click="submitAdjustBalance">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改提现账户 -->
    <el-dialog
      :title="$t('member.index.modifyWithdrawAccountTitle')"
      v-model="withdrawDialogVisible"
      width="560px"
      append-to-body
    >
      <!-- 判断 walletList 是否为空 -->
      <div v-if="walletList.length === 0" class="no-data">
        <el-tabs
          v-model="withdrawAccountActive"
          @tab-click="onWithdrawTabClick"
          type="card"
          class="mb-4"
        >
          <!-- 第一个为 TRC20 -->
          <el-tab-pane :label="$t('member.index.trc20')" name="TRC20" />
          <el-form
            ref="withdrawRef"
            :model="withdrawForm"
            :rules="withdrawRules"
            label-width="110px"
            label-position="top"
          >
            <el-form-item :label="$t('member.index.name')" prop="withdrawName">
              <el-input
                v-model="withdrawForm.withdrawName"
                :placeholder="$t('member.index.enterName')"
              />
            </el-form-item>
            <el-form-item prop="withdrawAddress">
              <el-input
                v-model="withdrawForm.withdrawAddress"
                :placeholder="$t('member.index.enterAddress')"
              />
            </el-form-item>
            <el-form-item prop="withdrawAddress">
              <el-input
                v-model="withdrawForm.withdrawType"
                :placeholder="$t('member.index.enterType')"
              />
            </el-form-item>
          </el-form>
        </el-tabs>
      </div>

      <el-tabs
        v-else
        v-model="withdrawAccountActive"
        @tab-click="onWithdrawTabClick"
        class="demo-tabs"
        type="card"
      >
        <el-tab-pane
          v-for="(item, index) in walletList"
          :key="index"
          :label="
            item.type === '1'
              ? $t('member.index.bankCard')
              : $t('member.index.wallet')
          "
          :name="item.id"
        >
          <el-form
            ref="withdrawRef"
            :model="withdrawForm"
            :rules="withdrawRules"
            label-width="110px"
            label-position="top"
          >
            <el-form-item :label="$t('member.index.name')" prop="name">
              <el-input
                v-model="withdrawForm.name"
                :placeholder="$t('member.index.enterName')"
              />
            </el-form-item>
            <el-form-item
              v-if="item.type === '1'"
              prop="bankCode"
              :label="$t('member.index.bankCode')"
            >
              <el-input
                v-model="withdrawForm.bankCode"
                :placeholder="$t('member.index.enterBankCode')"
              />
            </el-form-item>
            <el-form-item
              v-if="item.type === '1'"
              prop="bankCard"
              :label="$t('member.index.bankCardNumber')"
            >
              <el-input
                v-model="withdrawForm.bankCard"
                :placeholder="$t('member.index.enterBankCardNumber')"
              />
            </el-form-item>
            <el-form-item
              v-if="item.type === '2'"
              prop="walletType"
              :label="$t('member.index.walletType')"
            >
              <el-input
                v-model="withdrawForm.walletType"
                :placeholder="$t('member.index.enterWalletType')"
              />
            </el-form-item>
            <el-form-item
              v-if="item.type === '2'"
              prop="walletAddress"
              :label="$t('member.index.walletAddress')"
            >
              <el-input
                v-model="withdrawForm.walletAddress"
                :placeholder="$t('member.index.enterWalletAddress')"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeWithdrawDialog">{{
            $t("common.cancel")
          }}</el-button>
          <el-button
            type="primary"
            v-if="walletList.length === 0"
            @click="submitWithdrawAccountTwo"
            >{{ $t("common.confirm") }}</el-button
          >
          <el-button v-else type="primary" @click="submitWithdrawAccount">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改实名信息 -->
    <el-dialog
      :title="
        realNameDialogMode === 'status'
          ? $t('member.index.modifyRealNameStatusTitle')
          : $t('member.index.modifyRealNameTitle')
      "
      v-model="realNameDialogVisible"
      width="600px"
      append-to-body
    >
      <el-form
        ref="realNameRef"
        :model="realNameForm"
        :rules="realNameRules"
        label-width="110px"
      >
        <el-form-item :label="$t('member.index.realName')" prop="realName">
          <el-input
            v-model="realNameForm.realName"
            :placeholder="$t('member.index.enterRealName')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.index.idCardNumber')"
          prop="idCardNumber"
        >
          <el-input
            v-model="realNameForm.idCardNumber"
            :placeholder="$t('member.index.enterIdCardNumber')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('member.index.idCardFront')"
          prop="idCardFront"
        >
          <image-upload v-model="realNameForm.idCardFront" :limit="1" />
        </el-form-item>
        <el-form-item :label="$t('member.index.idCardBack')" prop="idCardBack">
          <image-upload v-model="realNameForm.idCardBack" :limit="1" />
        </el-form-item>
        <el-form-item
          :label="$t('member.index.realNameStatus')"
          prop="realNameStatus"
        >
          <el-radio-group v-model="realNameForm.realNameStatus">
            <el-radio :label="0">{{ $t("member.index.realNameNot") }}</el-radio>
            <el-radio :label="1">{{
              $t("member.index.realNameVerified")
            }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeRealNameDialog">{{
            $t("common.cancel")
          }}</el-button>
          <el-button type="primary" @click="submitRealName">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改信誉分 -->
    <el-dialog
      :title="$t('member.index.modifyCreditScoreTitle')"
      v-model="creditDialogVisible"
      width="520px"
      append-to-body
    >
      <el-form
        ref="creditRef"
        :model="creditForm"
        :rules="creditRules"
        label-position="top"
        label-width="100px"
      >
        <el-form-item
          :label="$t('member.index.creditScore')"
          prop="creditScore"
        >
          <div class="score-row">
            <el-slider
              v-model.number="creditForm.creditScore"
              :min="0"
              :max="100"
              :step="1"
            />
            <el-input-number
              v-model.number="creditForm.creditScore"
              :min="0"
              :max="100"
              :step="1"
              controls-position="right"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeCreditScoreDialog">{{
            $t("common.cancel")
          }}</el-button>
          <el-button type="primary" @click="submitCreditScore">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改禁止提现提示 -->
    <el-dialog
      :title="$t('member.index.modifyWithdrawTipTitle')"
      v-model="withdrawTipDialogVisible"
      width="560px"
      append-to-body
    >
      <el-form
        ref="withdrawTipRef"
        :model="withdrawTipForm"
        label-position="top"
        label-width="110px"
      >
        <el-form-item :label="$t('member.index.tip')">
          <el-input
            v-model="withdrawTipForm.withdrawTip"
            type="textarea"
            :rows="5"
            :maxlength="200"
            show-word-limit
            :placeholder="$t('member.index.enterWithdrawTip')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeWithdrawTipDialog">{{
            $t("common.cancel")
          }}</el-button>
          <el-button type="primary" @click="submitWithdrawTip">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改备注 -->
    <el-dialog
      :title="$t('member.index.modifyRemarkTitle')"
      v-model="remarkDialogVisible"
      width="560px"
      append-to-body
    >
      <el-form
        ref="remarkRef"
        :model="remarkForm"
        label-position="top"
        label-width="110px"
      >
        <el-form-item :label="$t('member.index.remarkLabel')">
          <el-input
            v-model="remarkForm.remark"
            type="textarea"
            :rows="5"
            :maxlength="200"
            show-word-limit
            :placeholder="$t('member.index.enterRemark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeRemarkDialog">{{
            $t("common.cancel")
          }}</el-button>
          <el-button type="primary" @click="submitRemark">{{
            $t("common.confirm")
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 下级用户列表 抽屉 -->
    <el-drawer
      :title="$t('member.index.subListTitle')"
      v-model="subListOpen"
      direction="rtl"
      size="92%"
      append-to-body
    >
      <SubMemberList :userId="subUserId" />
    </el-drawer>
  </div>
</template>

<script setup name="Member">
import {
  listMember,
  getMember,
  delMember,
  addMember,
  updateMember,
  levelList,
  topupAmount,
  restDealCount,
  upAmount,
  updateAmount,
  userBankList,
} from "@/api/member/member";
import { updateWallet } from "@/api/member/wallet";
import Series from "@/views/member/member/Series.vue";
import Account from "@/views/member/member/Account.vue";
import SubMemberList from "@/views/member/member/ScopeUser.vue";
import { useI18n } from "vue-i18n";
const { t, locale } = useI18n();
const { proxy } = getCurrentInstance();
const { sys_yes_no, sys_normal_disable } = proxy.useDict(
  "sys_yes_no",
  "sys_normal_disable"
);

const tableKey = ref(Date.now());
const columns = ref([
  {
    key: "0",
    labelKey: "member.index.id",
    label: t("member.index.id"),
    visible: true,
    prop: "id",
    width: "55",
  },
  {
    key: "1",
    labelKey: "member.index.username",
    label: t("member.index.username"),
    visible: true,
    prop: "username",
    width: "120",
  },
  {
    key: "2",
    labelKey: "member.index.phone",
    label: t("member.index.phone"),
    visible: true,
    prop: "phone",
    width: "180",
  },
  {
    key: "3",
    labelKey: "member.index.vipLevel",
    label: t("member.index.vipLevel"),
    visible: true,
    prop: "userLevel.nameZh",
    width: "180",
  },
  {
    key: "4",
    labelKey: "member.index.upgradeRechargeNeeded",
    label: t("member.index.upgradeRechargeNeeded"),
    visible: true,
    prop: "rechargeNeededForNextLevel",
    width: "150",
  },
  {
    key: "5",
    labelKey: "member.index.parentId",
    label: t("member.index.parentId"),
    visible: true,
    prop: "parentId",
    width: "80",
  },
  {
    key: "6",
    labelKey: "member.index.parentUsername",
    label: t("member.index.parentUsername"),
    visible: true,
    prop: "parentUsername",
    width: "120",
  },
  {
    key: "7",
    labelKey: "member.index.parentPhone",
    label: t("member.index.parentPhone"),
    visible: true,
    prop: "parentPhone",
    width: "120",
  },
  {
    key: "8",
    labelKey: "member.index.email",
    label: t("member.index.email"),
    visible: true,
    prop: "email",
    width: "200",
  },
  {
    key: "9",
    labelKey: "member.index.directSubCount",
    label: t("member.index.directSubCount"),
    visible: true,
    prop: "directSubCount",
    width: "120",
  },
  {
    key: "10",
    labelKey: "member.index.allSubCount",
    label: t("member.index.allSubCount"),
    visible: true,
    prop: "allSubCount",
    width: "120",
  },
  {
    key: "11",
    labelKey: "member.index.creditScore",
    label: t("member.index.creditScore"),
    visible: true,
    prop: "creditScore",
    width: "80",
  },
  {
    key: "12",
    labelKey: "member.index.balance",
    label: t("member.index.balance"),
    visible: true,
    prop: "balance",
    width: "120",
  },
  {
    key: "13",
    labelKey: "member.index.frozenBalance",
    label: t("member.index.frozenBalance"),
    visible: true,
    prop: "frozenBalance",
    width: "120",
  },
  {
    key: "14",
    labelKey: "member.index.totalBalance",
    label: t("member.index.totalBalance"),
    visible: true,
    prop: "totalBalance",
    width: "120",
  },
  {
    key: "15",
    labelKey: "member.index.todayWithdrawCount",
    label: t("member.index.todayWithdrawCount"),
    visible: true,
    prop: "todayWithdrawCount",
    width: "120",
  },
  {
    key: "16",
    labelKey: "member.index.todayResetCount",
    label: t("member.index.todayResetCount"),
    visible: true,
    prop: "todayResetCount",
    width: "120",
  },
  {
    key: "17",
    labelKey: "member.index.totalResetCount",
    label: t("member.index.totalResetCount"),
    visible: true,
    prop: "totalResetCount",
    width: "120",
  },
  {
    key: "18",
    labelKey: "member.index.taskProgress",
    label: t("member.index.taskProgress"),
    visible: true,
    prop: null,
    width: "120",
  },
  {
    key: "19",
    labelKey: "member.index.todayProfit",
    label: t("member.index.todayProfit"),
    visible: true,
    prop: "commission",
    width: "120",
  },
  {
    key: "20",
    labelKey: "member.index.inviteCode",
    label: t("member.index.inviteCode"),
    visible: true,
    prop: "inviteCode",
    width: "120",
  },
  {
    key: "21",
    labelKey: "member.index.ip",
    label: t("member.index.ip"),
    visible: true,
    prop: null,
    width: "180",
  },
  {
    key: "22",
    labelKey: "member.index.isFake",
    label: t("member.index.isFake"),
    visible: true,
    prop: null,
    width: "100",
  },
  {
    key: "23",
    labelKey: "member.index.accountStatus",
    label: t("member.index.accountStatus"),
    visible: true,
    prop: null,
    width: "120",
  },
  {
    key: "24",
    labelKey: "member.index.tradeStatus",
    label: t("member.index.tradeStatus"),
    visible: true,
    prop: null,
    width: "120",
  },
  {
    key: "25",
    labelKey: "member.index.withdrawStatus",
    label: t("member.index.withdrawStatus"),
    visible: true,
    prop: null,
    width: "120",
  },
  {
    key: "26",
    labelKey: "member.index.lastLoginTime",
    label: t("member.index.lastLoginTime"),
    visible: true,
    prop: null,
    width: "160",
  },
  {
    key: "27",
    labelKey: "member.index.registerTime",
    label: t("member.index.registerTime"),
    visible: true,
    prop: null,
    width: "160",
  },
  {
    key: "28",
    labelKey: "member.index.remark",
    label: t("member.index.remark"),
    visible: true,
    prop: "remark",
    width: "160",
    fixed: "right",
  },
  {
    key: "29",
    labelKey: "member.index.operation",
    label: t("member.index.operation"),
    visible: true,
    prop: null,
    width: "480",
    fixed: "right",
    className: "operation-buttons",
  },
]);
const filteredColumns = computed(() => {
  return columns.value.filter((column) => column.visible);
});

// 当语言切换时，更新 columns 中的 label 字段以触发子组件重新渲染
watch(
  () => locale.value,
  () => {
    columns.value.forEach((col) => {
      if (col.labelKey) {
        col.label = t(col.labelKey);
      }
    });
    // 更新 tableKey 以确保表格在语言切换后能完整重新渲染（必要时）
    tableKey.value = Date.now();
  }
);

const memberList = ref([]);
const open = ref(false);
const topupOpen = ref(false);
const loading = ref(true);
const dateRange = ref([]);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const LevelDatas = ref([]);
const isEdit = ref(false);
const seriesOpen = ref(false);
const accountOpen = ref(false);
const selectedUserId = ref(null);
const dealDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const passwordDialogTitle = ref("");
const passwordType = ref("login");
const changeParentOpen = ref(false);
const giftBalanceDialogVisible = ref(false);
const adjustBalanceDialogVisible = ref(false);
const withdrawDialogVisible = ref(false);
const withdrawAccountActive = ref("TRC20");
const realNameDialogVisible = ref(false);
const realNameDialogMode = ref("full");
const creditDialogVisible = ref(false);
const withdrawTipDialogVisible = ref(false);
const remarkDialogVisible = ref(false);
const subListOpen = ref(false);
const subUserId = ref(null);
const walletList = ref([]);

const data = reactive({
  form: {},
  seriesForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: null,
    phone: null,
  },
  dealForm: {
    userId: null,
    username: "",
    currentDealCount: 0,
    newDealCount: null,
  },
  passwordForm: {
    userId: null,
    newPassword: "",
  },
  topupForm: {
    userId: null,
    balance: null,
    type: "0",
    amount: null,
  },
  changeParentForm: {
    userId: null,
    parentId: null,
  },
  giftBalanceForm: {
    userId: null,
    currentBalance: null,
    giftAmount: null,
  },
  adjustBalanceForm: {
    userId: null,
    currentBalance: null,
    newBalance: null,
  },
  withdrawForm: {},
  realNameForm: {
    userId: null,
    realName: "",
    idCardNumber: "",
    idCardFront: "",
    idCardBack: "",
    realNameStatus: 0,
  },
  creditForm: {
    userId: null,
    creditScore: 0,
  },
  withdrawTipForm: {
    userId: null,
    withdrawTip: "",
  },
  remarkForm: {
    userId: null,
    remark: "",
  },
  rules: {
    username: [
      {
        required: true,
        message: t("member.index.usernameRequired"),
        trigger: "blur",
      },
    ],
    phone: [
      {
        required: true,
        message: t("member.index.phoneRequired"),
        trigger: "blur",
      },
    ],
    isReal: [
      {
        required: true,
        message: t("member.index.isRealRequired"),
        trigger: "blur",
      },
    ],
    levelId: [
      {
        required: true,
        message: t("member.index.levelIdRequired"),
        trigger: "blur",
      },
    ],
  },
  topupRules: {
    type: [
      {
        required: true,
        message: t("member.index.operationTypeRequired"),
        trigger: "change",
      },
    ],
    amount: [
      {
        required: true,
        message: t("member.index.amountRequired"),
        trigger: "blur",
      },
      {
        type: "number",
        min: 0,
        message: t("member.index.amountPositive"),
        trigger: "blur",
      },
    ],
  },
  dealCountRules: {
    newDealCount: [
      {
        required: true,
        message: t("member.index.newDealCountRequired"),
        trigger: "blur",
      },
    ],
  },
  changeParentRules: {
    parentId: [
      {
        required: true,
        message: t("member.index.parentIdRequired"),
        trigger: "blur",
      },
    ],
  },
  giftBalancetRules: {
    giftAmount: [
      {
        required: true,
        message: t("member.index.giftAmountRequired"),
        trigger: "blur",
      },
    ],
  },
  adjustBalanceRules: {
    newBalance: [
      {
        required: true,
        message: t("member.index.newBalanceRequired"),
        trigger: "blur",
      },
    ],
  },
  withdrawRules: {
    withdrawName: [
      {
        required: true,
        message: t("member.index.withdrawNameRequired"),
        trigger: "blur",
      },
    ],
    withdrawAddress: [
      {
        required: true,
        message: t("member.index.withdrawAddressRequired"),
        trigger: "blur",
      },
    ],
    withdrawType: [
      {
        required: true,
        message: t("member.index.withdrawTypeRequired"),
        trigger: "blur",
      },
    ],
  },
  realNameRules: {
    realName: [
      {
        validator: (rule, value, callback) => {
          if (realNameForm.value.realNameStatus === 1 && !value) {
            return callback(new Error(t("member.index.realNameRequired")));
          }
          callback();
        },
        trigger: "blur",
      },
    ],
    idCardNumber: [
      {
        validator: (rule, value, callback) => {
          if (realNameForm.value.realNameStatus === 1 && !value) {
            return callback(new Error(t("member.index.idCardNumberRequired")));
          }
          callback();
        },
        trigger: "blur",
      },
    ],
    idCardFront: [
      {
        validator: (rule, value, callback) => {
          if (realNameForm.value.realNameStatus === 1 && !value) {
            return callback(new Error(t("member.index.idCardFrontRequired")));
          }
          callback();
        },
        trigger: "change",
      },
    ],
    idCardBack: [
      {
        validator: (rule, value, callback) => {
          if (realNameForm.value.realNameStatus === 1 && !value) {
            return callback(new Error(t("member.index.idCardBackRequired")));
          }
          callback();
        },
        trigger: "change",
      },
    ],
  },
  creditRules: {
    creditScore: [
      {
        required: true,
        message: t("member.index.creditScoreRequired"),
        trigger: "blur",
      },
      {
        type: "number",
        min: 0,
        max: 100,
        message: t("member.index.creditScoreRange"),
        trigger: "blur",
      },
    ],
  },
});

const {
  queryParams,
  form,
  topupForm,
  rules,
  topupRules,
  dealCountRules,
  seriesForm,
  dealForm,
  passwordForm,
  changeParentForm,
  changeParentRules,
  giftBalanceForm,
  giftBalancetRules,
  adjustBalanceForm,
  adjustBalanceRules,
  withdrawForm,
  withdrawRules,
  realNameForm,
  realNameRules,
  creditForm,
  creditRules,
  withdrawTipForm,
  remarkForm,
} = toRefs(data);

/** 更新列顺序 */
function handleColumnsUpdate(newColumns) {
  console.log("Received newColumns:", newColumns);
  // 验证并过滤无效列
  const validColumns = newColumns.filter(
    (column) =>
      column &&
      typeof column === "object" &&
      Object.prototype.hasOwnProperty.call(column, "visible") &&
      Object.prototype.hasOwnProperty.call(column, "key")
  );
  if (validColumns.length !== newColumns.length) {
    console.warn(
      "Filtered out invalid columns:",
      newColumns.filter(
        (column) =>
          !column ||
          typeof column !== "object" ||
          !Object.prototype.hasOwnProperty.call(column, "visible") ||
          !Object.prototype.hasOwnProperty.call(column, "key")
      )
    );
  }
  columns.value = validColumns;
  tableKey.value = Date.now(); // 强制表格重新渲染
}

// 动态规则：根据是否是新增决定是否必填
watch(
  () => form.value.id,
  (newVal) => {
    if (newVal) {
      rules.value.password = [];
      rules.value.tradePassword = [];
    } else {
      rules.value.password = [
        { required: true, message: "登录密码不能为空", trigger: "blur" },
      ];
    }
  },
  { immediate: true }
);

function openSubList(row) {
  subUserId.value = row.id;
  subListOpen.value = true;
}

function handleToggleTradeStatus(row) {
  const cur = String(row.tradeStatus);
  const target = cur === "0" ? "1" : "0";
  const actionText =
    target === "1" ? t("member.index.disable") : t("member.index.enable");
  proxy.$modal
    .confirm(
      t("member.index.confirmToggleTrade", {
        action: actionText,
        userId: row.id,
        username: row.username,
      })
    )
    .then(() => updateMember({ id: row.id, tradeStatus: target }))
    .then(() => {
      proxy.$modal.msgSuccess(
        t("member.index.toggleSuccess", { action: actionText })
      );
      getList();
    })
    .catch(() => {});
}

function handleToggleWithdrawStatus(row) {
  const cur = String(row.withdrawStatus);
  const target = cur === "0" ? "1" : "0";
  const actionText =
    target === "1" ? t("member.index.disable") : t("member.index.enable");
  proxy.$modal
    .confirm(
      t("member.index.confirmToggleWithdraw", {
        action: actionText,
        userId: row.id,
        username: row.username,
      })
    )
    .then(() => updateMember({ id: row.id, withdrawStatus: target }))
    .then(() => {
      proxy.$modal.msgSuccess(
        t("member.index.toggleSuccess", { action: actionText })
      );
      getList();
    })
    .catch(() => {});
}

function handleToggleIsReal(row) {
  const cur = String(row.isReal);
  const target = cur === "Y" ? "N" : "Y";
  const actionText =
    target === "N" ? t("member.index.setReal") : t("member.index.setFake");
  proxy.$modal
    .confirm(
      t("member.index.confirmToggleReal", {
        action: actionText,
        userId: row.id,
        username: row.username,
      })
    )
    .then(() => updateMember({ id: row.id, isReal: target }))
    .then(() => {
      proxy.$modal.msgSuccess(
        t("member.index.toggleSuccess", { action: actionText })
      );
      getList();
    })
    .catch(() => {});
}

function handleToggleAccountStatus(row) {
  const cur = String(row.accountStatus);
  const target = cur === "0" ? "1" : "0";
  const actionText =
    target === "1" ? t("member.index.disable") : t("member.index.enable");
  proxy.$modal
    .confirm(
      t("member.index.confirmToggleAccount", {
        action: actionText,
        userId: row.id,
        username: row.username,
      })
    )
    .then(() => updateMember({ id: row.id, accountStatus: target }))
    .then(() => {
      proxy.$modal.msgSuccess(
        t("member.index.toggleSuccess", { action: actionText })
      );
      getList();
    })
    .catch(() => {});
}

function resetRemarkForm() {
  remarkForm.value = {
    userId: null,
    remark: "",
  };
  proxy.resetForm("remarkRef");
}

function openRemarkDialog(row) {
  resetRemarkForm();
  remarkForm.value.userId = row.id;
  remarkForm.value.remark = row.remark || "";
  remarkDialogVisible.value = true;
}

function closeRemarkDialog() {
  remarkDialogVisible.value = false;
  resetRemarkForm();
}

function submitRemark() {
  updateMember({
    id: remarkForm.value.userId,
    remark: remarkForm.value.remark || "",
  })
    .then(() => {
      proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
      remarkDialogVisible.value = false;
      getList();
    })
    .catch(() => proxy.$modal.msgError(t("member.index.updateFailed")));
}

function resetWithdrawTipForm() {
  withdrawTipForm.value = {
    userId: null,
    withdrawTip: "",
  };
  proxy.resetForm("withdrawTipRef");
}

function openWithdrawTipDialog(row) {
  resetWithdrawTipForm();
  withdrawTipForm.value.userId = row.id;
  withdrawTipForm.value.withdrawTip = row.withdrawTip || "";
  withdrawTipDialogVisible.value = true;
}

function closeWithdrawTipDialog() {
  withdrawTipDialogVisible.value = false;
  resetWithdrawTipForm();
}

function submitWithdrawTip() {
  updateMember({
    id: withdrawTipForm.value.userId,
    withdrawTip: withdrawTipForm.value.withdrawTip || "",
  })
    .then(() => {
      proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
      withdrawTipDialogVisible.value = false;
      getList();
    })
    .catch(() => proxy.$modal.msgError(t("member.index.updateFailed")));
}

function resetCreditForm() {
  creditForm.value = {
    userId: null,
    creditScore: 0,
  };
  proxy.resetForm("creditRef");
}

function openCreditScoreDialog(row) {
  resetCreditForm();
  creditForm.value.userId = row.id;
  creditForm.value.creditScore = Number(row.creditScore ?? 0);
  creditDialogVisible.value = true;
}

function submitCreditScore() {
  proxy.$refs["creditRef"].validate((valid) => {
    if (!valid) return;
    updateMember({
      id: creditForm.value.userId,
      creditScore: creditForm.value.creditScore,
    })
      .then(() => {
        proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
        creditDialogVisible.value = false;
        getList();
      })
      .catch(() => proxy.$modal.msgError(t("member.index.updateFailed")));
  });
}

const hasRealInfo = computed(() => {
  const f = realNameForm.value;
  return !!(f.realName && f.idCardNumber && f.idCardFront && f.idCardBack);
});

function resetRealNameForm() {
  realNameForm.value = {
    userId: null,
    realName: "",
    idCardNumber: "",
    idCardFront: "",
    idCardBack: "",
    realNameStatus: 0,
  };
  proxy.resetForm("realNameRef");
}

function openRealNameDialog(row, mode = "full") {
  resetRealNameForm();
  realNameDialogMode.value = mode;
  realNameDialogVisible.value = true;
  realNameForm.value.userId = row.id;
  realNameForm.value.realName = row.realName || "";
  realNameForm.value.idCardNumber = row.idCardNumber || "";
  realNameForm.value.idCardFront = row.idCardFront || "";
  realNameForm.value.idCardBack = row.idCardBack || "";
  realNameForm.value.realNameStatus = Number(row.realNameStatus ?? 0);
}

function closeRealNameDialog() {
  realNameDialogVisible.value = false;
  resetRealNameForm();
}

function submitRealName() {
  if (realNameDialogMode.value === "status") {
    if (realNameForm.value.realNameStatus === 1 && !hasRealInfo.value) {
      proxy.$modal.msgWarning(t("member.index.realNameWarning"));
      return;
    }
    const payload = {
      id: realNameForm.value.userId,
      realNameStatus: realNameForm.value.realNameStatus,
    };
    updateMember(payload)
      .then(() => {
        proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
        realNameDialogVisible.value = false;
        getList();
      })
      .catch(() => {
        proxy.$modal.msgError(t("member.index.updateFailed"));
      });
    return;
  }
  proxy.$refs["realNameRef"].validate((valid) => {
    if (!valid) return;
    const payload = {
      id: realNameForm.value.userId,
      realNameStatus: realNameForm.value.realNameStatus,
    };
    if (realNameForm.value.realNameStatus === 1) {
      payload.realName = realNameForm.value.realName;
      payload.idCardNumber = realNameForm.value.idCardNumber;
      payload.idCardFront = realNameForm.value.idCardFront;
      payload.idCardBack = realNameForm.value.idCardBack;
    }
    updateMember(payload).then(() => {
      proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
      realNameDialogVisible.value = false;
      getList();
    });
  });
}

function resetWithdrawForm() {
  withdrawForm.value = {};
}

async function openWithdrawDialog(row) {
  try {
    const res = await userBankList(row.id);
    walletList.value = res.data;
    resetWithdrawForm();
    if (walletList.value.length > 0) {
      withdrawForm.value = { ...walletList.value[0] }; // 赋值给 withdrawForm
      withdrawAccountActive.value = walletList.value[0].id; // 根据 id 设置默认选中的 tab
    } else {
      withdrawForm.value.userId = row.id;
      withdrawForm.value.withdrawName = row.withdrawName || "";
      withdrawForm.value.withdrawAddress = row.withdrawAddress || "";
      withdrawForm.value.withdrawType = row.withdrawType || "";
    }
  } catch (error) {
    console.error("Failed to fetch withdraw data:", error); // 捕获并处理错误
  }

  withdrawDialogVisible.value = true; // 打开对话框
}

function closeWithdrawDialog() {
  withdrawDialogVisible.value = false;
  resetWithdrawForm();
}

function onWithdrawTabClick(tab, event) {
  // 找到对应的 item，通过 tab.name (tab.id) 匹配
  const selectedItem = walletList.value.find(
    (item) => item.id === tab.paneName
  );
  if (selectedItem) {
    withdrawForm.value = { ...selectedItem }; // 将点击的 tab 对应的数据赋值给 withdrawForm
  }
}

function submitWithdrawAccount() {
  updateWallet(withdrawForm.value).then((response) => {
    proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
    withdrawDialogVisible.value = false;
  });
}

function submitWithdrawAccountTwo() {
  proxy.$refs["withdrawRef"].validate((valid) => {
    if (!valid) return;
    const payload = {
      id: withdrawForm.value.userId,
      withdrawName: withdrawForm.value.withdrawName,
      withdrawAddress: withdrawForm.value.withdrawAddress,
      withdrawType: withdrawForm.value.withdrawType,
    };
    updateMember(payload)
      .then(() => {
        proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
        withdrawDialogVisible.value = false;
        getList();
      })
      .catch(() => {
        proxy.$modal.msgError(t("member.index.updateFailed"));
      });
  });
}

function resetAdjustBalance() {
  adjustBalanceForm.value.userId = null;
  adjustBalanceForm.value.currentBalance = null;
  adjustBalanceForm.value.newBalance = null;
  proxy.resetForm("adjustBalanceRef");
}

function adjustBalanceDialogClose() {
  adjustBalanceDialogVisible.value = false;
  resetAdjustBalance();
}

function openAdjustBalanceDialog(row) {
  resetAdjustBalance();
  adjustBalanceForm.value.userId = row.id;
  adjustBalanceForm.value.currentBalance = row.balance;
  adjustBalanceForm.value.newBalance = row.balance;
  adjustBalanceDialogVisible.value = true;
}

function submitAdjustBalance() {
  proxy.$refs["adjustBalanceRef"].validate((valid) => {
    if (!valid) return;
    const payload = {
      userId: adjustBalanceForm.value.userId,
      amount: adjustBalanceForm.value.newBalance,
    };
    updateAmount(payload)
      .then(() => {
        proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
        adjustBalanceDialogVisible.value = false;
        getList();
      })
      .catch(() => {
        proxy.$modal.msgError(t("member.index.updateFailed"));
      });
  });
}

function resetGiftBalance() {
  giftBalanceForm.value.userId = null;
  giftBalanceForm.value.currentBalance = null;
  giftBalanceForm.value.giftAmount = null;
  proxy.resetForm("giftBalanceRef");
}

function giftBalanceDialogClose() {
  giftBalanceDialogVisible.value = false;
  resetGiftBalance();
}

function openGiftBalanceDialog(row) {
  giftBalanceForm.value.userId = row.id;
  giftBalanceForm.value.currentBalance = row.balance;
  giftBalanceDialogVisible.value = true;
}

function confirmGiftBalance() {
  proxy.$refs["giftBalanceRef"].validate((valid) => {
    if (valid) {
      const payload = {
        userId: giftBalanceForm.value.userId,
        amount: giftBalanceForm.value.giftAmount,
      };
      upAmount(payload).then(() => {
        proxy.$modal.msgSuccess(t("member.index.operationSuccess"));
        giftBalanceDialogVisible.value = false;
        getList();
      });
    }
  });
}

function resetChangeParent() {
  changeParentForm.value.userId = null;
  changeParentForm.value.parentId = null;
  proxy.resetForm("changeParentRef");
}

function cancelChangeParent() {
  changeParentOpen.value = false;
  resetChangeParent();
}

function handleChangeParent(row) {
  resetChangeParent();
  changeParentForm.value.userId = row.id;
  changeParentOpen.value = true;
}

function submitChangeParent() {
  proxy.$refs["changeParentRef"].validate((valid) => {
    if (valid) {
      const payload = {
        id: changeParentForm.value.userId,
        parentId: changeParentForm.value.parentId,
      };
      updateMember(payload).then(() => {
        proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
        changeParentOpen.value = false;
        getList();
      });
    }
  });
}

function restOrderNum(row) {
  restDealCount(row.id).then((res) => {
    proxy.$modal.msgSuccess(t("member.index.operationSuccess"));
    getList();
  });
}

function handleAccount(row) {
  selectedUserId.value = row.id;
  accountOpen.value = true;
}

function openPasswordDialog(row, type) {
  passwordForm.value = {
    userId: row.id,
    newPassword: "",
  };
  passwordType.value = type;
  passwordDialogTitle.value =
    type === "login"
      ? t("member.index.modifyPassword")
      : t("member.index.modifyTradePassword");
  passwordDialogVisible.value = true;
}

function submitPassword() {
  const payload = {
    id: passwordForm.value.userId,
    [passwordType.value === "login" ? "password" : "tradePassword"]:
      passwordForm.value.newPassword,
  };
  updateMember(payload).then(() => {
    proxy.$modal.msgSuccess(t("member.index.operationSuccess"));
    passwordDialogVisible.value = false;
    getList();
  });
}

function openEditDealCountDialog(row) {
  getMember(row.id).then((response) => {
    dealForm.value = {
      userId: row.id,
      username: response.data.username,
      currentDealCount: response.data.dealCount,
      newDealCount: null,
    };
  });
  dealDialogVisible.value = true;
}

function submitDealCount() {
  proxy.$refs["dealCountRef"].validate((valid) => {
    if (valid) {
      const payload = {
        id: dealForm.value.userId,
        dealCount: dealForm.value.newDealCount,
      };
      updateMember(payload).then(() => {
        proxy.$modal.msgSuccess(t("member.index.operationSuccess"));
        dealDialogVisible.value = false;
        getList();
      });
    }
  });
}

function handleSeries(row) {
  seriesForm.value.userId = row.id;
  selectedUserId.value = row.id;
  seriesOpen.value = true;
}

function getList() {
  loading.value = true;
  listMember(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      memberList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
}

function AllLevel() {
  levelList().then((res) => {
    LevelDatas.value = res.data;
  });
}
AllLevel();

function cancel() {
  open.value = false;
  reset();
}

function cancelTopup() {
  topupOpen.value = false;
  resetTopup();
}

function reset() {
  form.value = {
    id: null,
    username: null,
    phone: null,
    password: null,
    tradePassword: null,
    parentId: null,
    ancestors: null,
    email: null,
    creditScore: null,
    balance: null,
    frozenBalance: null,
    totalBalance: null,
    inviteCode: null,
    registerIp: null,
    lastLoginTime: null,
    accountStatus: "0",
    tradeStatus: "0",
    withdrawStatus: "0",
    realNameStatus: null,
    realName: null,
    idCardNumber: null,
    idCardFront: null,
    idCardBack: null,
    withdrawName: null,
    withdrawAddress: null,
    withdrawType: null,
    createTime: null,
    remark: null,
    isReal: null,
    levelId: null,
    cardNumber: null,
    cardAmount: null,
  };
  proxy.resetForm("memberRef");
}

function resetTopup() {
  topupForm.value = {
    userId: null,
    balance: null,
    type: "0",
    amount: null,
  };
  proxy.resetForm("topupRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  isEdit.value = false;
  open.value = true;
  title.value = t("member.index.addMember");
}

function handleUpdate(row) {
  reset();
  isEdit.value = true;
  getMember(row.id).then((response) => {
    form.value = response.data;
    form.value.password = null;
    form.value.tradePassword = null;
    open.value = true;
    title.value = t("member.index.editMember");
  });
}

function handleTopup(row) {
  resetTopup();
  topupForm.value.userId = row.id;
  topupForm.value.balance = row.balance;
  topupOpen.value = true;
}

function submitTopupForm() {
  proxy.$refs["topupRef"].validate((valid) => {
    if (valid) {
      topupAmount({
        userId: topupForm.value.userId,
        type: topupForm.value.type,
        amount: topupForm.value.amount,
      })
        .then(() => {
          proxy.$modal.msgSuccess(t("member.index.operationSuccess"));
          topupOpen.value = false;
          getList();
        })
        .catch(() => {
          proxy.$modal.msgError(t("member.index.operationFailed"));
        });
    }
  });
}

function submitForm() {
  proxy.$refs["memberRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateMember(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.index.updateSuccess"));
          open.value = false;
          getList();
        });
      } else {
        addMember(form.value).then((response) => {
          proxy.$modal.msgSuccess(t("member.index.addSuccess"));
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(t("member.index.deleteConfirm", { ids: _ids }))
    .then(function () {
      return delMember(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(t("member.index.deleteSuccess"));
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    "member/member/export",
    { ...queryParams.value },
    `member_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>

<style scoped>
.operation-buttons {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.score-row {
  display: flex;
  align-items: center;
  width: 100%;
}
.score-row :deep(.el-slider) {
  flex: 1;
  margin-right: 12px;
}
.score-row :deep(.el-input-number) {
  width: 140px;
}
</style>
