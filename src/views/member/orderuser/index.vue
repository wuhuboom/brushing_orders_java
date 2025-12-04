<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phoneNumber">
        <el-input
          v-model="queryParams.phoneNumber"
          placeholder="请输入手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['member:orderuser:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:orderuser:edit']"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:orderuser:remove']"
          >删除</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="orderuserList"
      @selection-change="handleSelectionChange"
      :border="true"
    >
      <el-table-column type="selection" width="55" fixed="left" />
      <el-table-column label="ID" prop="id" fixed="left" align="center" />
      <el-table-column
        label="用户名"
        prop="username"
        align="center"
        width="180"
      />
      <el-table-column
        label="手机号"
        prop="phoneNumber"
        align="center"
        width="180"
      />
      <el-table-column
        label="会员等级"
        prop="memberLevel.name"
        align="center"
        width="100"
      />

      <el-table-column label="上级信息" width="180">
        <template #default="scope">
          <div>上级邀请码：{{ scope.row.parentInviteCode || "无" }}</div>
          <div>上级用户名：{{ scope.row.parentUsername || "无" }}</div>
        </template>
      </el-table-column>
      <el-table-column label="重置次数" width="180">
        <template #default="scope">
          <div>今日重置次数：{{ scope.row.todayRest }}</div>
          <div>累计重置次数：{{ scope.row.totalRest }}</div>
        </template>
      </el-table-column>
      <el-table-column label="余额信息" width="180">
        <template #default="scope">
          <div>总余额：{{ scope.row.balance + scope.row.frozenBalance }}</div>
          <div>余额：{{ scope.row.balance }}</div>
          <div>冻结余额：{{ scope.row.frozenBalance }}</div>
          <div>底薪：{{ scope.row.baseSalary }}</div>
          <div>今日佣金：{{ scope.row.todayCommission }}</div>
          <div>今日上级佣金：{{ scope.row.todayParentCommission }}</div>
        </template>
      </el-table-column>
      <el-table-column
        label="任务进度"
        align="center"
        prop="taskProgress"
        width="100"
      >
        <template #default="scope">
          <div>
            {{ scope.row.taskProgress }} /
            {{ scope.row.memberLevel.orderCountPerDay }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="登录信息" width="280">
        <template #default="scope">
          <div>最后登录IP：{{ scope.row.lastLoginIp }}</div>
          <div>最后登录地址：{{ scope.row.lastLoginAddress }}</div>
          <div>最后登录时间：{{ parseTime(scope.row.lastLoginTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="签到信息" width="180">
        <template #default="scope">
          <div>签到天数：0</div>
          <div>今日签到次数：0</div>
          <div>累计签到天数：0</div>
        </template>
      </el-table-column>
      <el-table-column label="统计信息" width="180">
        <template #default="scope">
          <div>直属下级数量：{{ scope.row.directChildrenCount }}</div>
          <div>今日提现次数：{{ scope.row.todayWithdrawalCount }}</div>
          <div>累计提现金额：{{ scope.row.totalWithdrawalAmount }}</div>
          <div>累计充值金额：{{ scope.row.totalRechargeAmount }}</div>
        </template>
      </el-table-column>
      <el-table-column label="信誉分" align="center" prop="reputationScore" />
      <el-table-column
        label="邀请码"
        align="center"
        prop="inviteCode"
        width="150"
      />
      <el-table-column label="性别" align="center" prop="gender">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender" />
        </template>
      </el-table-column>
      <el-table-column label="邮箱" align="center" prop="email" width="180" />
      <el-table-column label="生日" align="center" prop="birthday" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.birthday, "{y}-{m}-{d}") }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isEnabled" />
        </template>
      </el-table-column>
      <el-table-column label="允许邀请" align="center" prop="allowInvite">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.allowInvite" />
        </template>
      </el-table-column>
      <el-table-column label="是否冻结" align="center" prop="isFrozen">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isFrozen" />
        </template>
      </el-table-column>
      <el-table-column label="是否假人" align="center" prop="isFake">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isFake" />
        </template>
      </el-table-column>
      <el-table-column label="禁止工作" align="center" prop="isBanned">
        <template #default="scope">
          <dict-tag :options="user_yes_no" :value="scope.row.isBanned" />
        </template>
      </el-table-column>
      <el-table-column label="工作限额" prop="workLimit" align="center" />
      <el-table-column
        label="关闭提现通知"
        align="center"
        prop="isWithdrawalNotification"
        width="120px"
      >
        <template #default="scope">
          <dict-tag
            :options="user_yes_no"
            :value="scope.row.isWithdrawalNotification"
          />
        </template>
      </el-table-column>
      <el-table-column label="产品匹配" align="center" prop="productMatching">
        <template #default="scope">
          <dict-tag :options="sys_enabled" :value="scope.row.productMatching" />
        </template>
      </el-table-column>
      <el-table-column label="账户状态" align="center" prop="accountStatus">
        <template #default="scope">
          <dict-tag :options="sys_enabled" :value="scope.row.accountStatus" />
        </template>
      </el-table-column>

      <el-table-column label="交易状态" align="center" prop="transactionStatus">
        <template #default="scope">
          <dict-tag
            :options="sys_enabled"
            :value="scope.row.transactionStatus"
          />
        </template>
      </el-table-column>
      <el-table-column label="提现状态" align="center" prop="withdrawalStatus">
        <template #default="scope">
          <dict-tag
            :options="sys_enabled"
            :value="scope.row.withdrawalStatus"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="协助金提现状态"
        prop="assistWithdrawalStatus"
        align="center"
        width="140px"
      >
        <template #default="scope">
          <dict-tag
            :options="sys_enabled"
            :value="scope.row.assistWithdrawalStatus"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="充值后禁止提现"
        align="center"
        width="140px"
        prop="depositBlockWithdrawal"
      >
        <template #default="scope">
          <dict-tag
            :options="user_yes_no"
            :value="scope.row.depositBlockWithdrawal"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remarks" width="180" />
      <el-table-column
        label="操作"
        class-name="small-padding fixed-width"
        fixed="right"
        width="360"
      >
        <template #default="scope">
          <div class="button-flex">
            <div>
              <el-button type="warning" @click="handleTransaction(scope.row)">
                上下分
              </el-button>
              <el-button type="primary" @click="handleOpenLink(scope.row)">
                连单设置
              </el-button>
              <el-button type="primary" @click="handleReset(scope.row)">
                重置单数
              </el-button>
              <el-button type="primary" @click="handleUpdate(scope.row)">
                修 改
              </el-button>
            </div>
            <div>
              <el-button type="primary" @click="openModifyCount(scope.row)">
                修改单数
              </el-button>
              <el-button type="success" @click="handleOpenBonus(scope.row)">
                彩金设置
              </el-button>
              <el-button
                type="primary"
                @click="handleModifyLoginPassword(scope.row)"
              >
                修改登录密码
              </el-button>
            </div>
            <div>
              <el-button
                type="primary"
                @click="handleModifyTradePassword(scope.row)"
              >
                修改交易密码
              </el-button>
              <el-button type="primary" @click="handleOpenFlow(scope.row)">
                查看交易流水
              </el-button>
            </div>
            <div>
              <el-button
                type="primary"
                @click="handleOpenWithdrawal(scope.row)"
              >
                修改提现账户
              </el-button>
              <el-button
                type="primary"
                @click="handleModifyReputation(scope.row)"
              >
                修改信誉分
              </el-button>
              <el-button type="primary" @click="handleModifyParent(scope.row)">
                修改上级
              </el-button>
            </div>
            <div class="bt-button">
              <el-button type="primary" @click="handleModifyVip(scope.row)">
                修改等级
              </el-button>
              <el-dropdown trigger="click">
                <el-button type="primary">
                  更多
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleGift(scope.row)">
                      <span class="other-item">赠送</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleSubMembers(scope.row)">
                      <span class="other-item">查看下级会员</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleModifySignDays(scope.row)">
                      <span class="other-item">修改签到天数</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleOrderDetails(scope.row)">
                      <span class="other-item">查看订单明细</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleExtraCommission(scope.row)">
                      <span class="other-item">额外佣金设置</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleToggleFake(scope.row)">
                      <div
                        :class="
                          scope.row.isFake === '1'
                            ? 'danger-item'
                            : 'success-item'
                        "
                      >
                        {{ scope.row.isFake === "1" ? "设为假人" : "设为真人" }}
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="handleToggleAccountStatus(scope.row)"
                    >
                      <div
                        :class="
                          scope.row.accountStatus == '1'
                            ? 'success-item'
                            : 'danger-item'
                        "
                      >
                        {{
                          scope.row.accountStatus === "1"
                            ? "启用账户"
                            : "禁用账户"
                        }}
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="handleToggleTransactionStatus(scope.row)"
                    >
                      <div
                        :class="
                          scope.row.transactionStatus === '1'
                            ? 'success-item'
                            : 'danger-item'
                        "
                      >
                        {{
                          scope.row.transactionStatus === "1"
                            ? "启用交易"
                            : "禁用交易"
                        }}
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="handleToggleWithdrawalStatus(scope.row)"
                    >
                      <div
                        :class="
                          scope.row.withdrawalStatus === '1'
                            ? 'success-item'
                            : 'danger-item'
                        "
                      >
                        {{
                          scope.row.withdrawalStatus === "1"
                            ? "启用提现"
                            : "禁用提现"
                        }}
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item
                      @click="handleToggleAssistWithdrawalStatus(scope.row)"
                    >
                      <div
                        :class="
                          scope.row.assistWithdrawalStatus === '1'
                            ? 'success-item'
                            : 'danger-item'
                        "
                      >
                        {{
                          scope.row.assistWithdrawalStatus === "1"
                            ? "启用协助金提现"
                            : "禁用协助金提现"
                        }}
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      title="修改单数"
      v-model="modifyModalVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="modifyForm"
        :rules="modifyRules"
        label-position="top"
        label-width="100px"
        ref="modifyFormRef"
      >
        <el-form-item label="任务进度">
          <el-input
            v-model="modifyForm.orderCount"
            :disabled="true"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="单数" prop="taskProgress">
          <el-input-number
            v-model="modifyForm.taskProgress"
            :min="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="modifyModalVisible = false">取消</el-button>
          <el-button type="primary" @click="submitModifyCount">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改登录密码对话框 -->
    <el-dialog
      title="修改登录密码"
      v-model="loginPasswordVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        label-width="100px"
        ref="loginFormRef"
      >
        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入新登录密码（不少于6位）"
            show-password
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="loginPasswordVisible = false">取消</el-button>
          <el-button type="primary" @click="submitLoginPassword"
            >确定</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- 修改交易密码对话框 -->
    <el-dialog
      title="修改交易密码"
      v-model="tradePasswordVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="tradeForm"
        :rules="tradeRules"
        label-position="top"
        label-width="100px"
        ref="tradeFormRef"
      >
        <el-form-item label="交易密码" prop="tradePassword">
          <el-input
            v-model="tradeForm.tradePassword"
            type="password"
            placeholder="请输入新交易密码（不少于6位）"
            show-password
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="tradePasswordVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTradePassword"
            >确定</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- 修改上级对话框 -->
    <el-dialog
      title="修改上级"
      v-model="parentVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="parentForm"
        :rules="parentRules"
        label-position="top"
        label-width="100px"
        ref="parentFormRef"
      >
        <el-form-item label="上级ID" prop="parentId">
          <el-input
            v-model="parentForm.parentId"
            placeholder="请输入上级ID"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="parentVisible = false">取消</el-button>
          <el-button type="primary" @click="submitModifyParent">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改等级对话框 -->
    <el-dialog
      title="修改等级"
      v-model="vipVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="vipForm"
        :rules="vipRules"
        label-position="top"
        label-width="100px"
        ref="vipFormRef"
      >
        <el-form-item label="会员等级" prop="vipId">
          <el-select
            v-model="vipForm.vipId"
            placeholder="请选择会员等级"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in levelList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="vipVisible = false">取消</el-button>
          <el-button type="primary" @click="submitModifyVip">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改信誉分对话框 -->
    <el-dialog
      title="修改信誉分"
      v-model="reputationVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="reputationForm"
        :rules="reputationRules"
        label-position="top"
        label-width="100px"
        ref="reputationFormRef"
      >
        <el-form-item label="当前信誉分">
          <el-input
            v-model="reputationForm.currentReputation"
            :disabled="true"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="修改后的信誉分" prop="newReputation">
          <el-input-number
            v-model="reputationForm.newReputation"
            :min="0"
            :max="100"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reputationVisible = false">取消</el-button>
          <el-button type="primary" @click="submitModifyReputation"
            >确定</el-button
          >
        </div>
      </template>
    </el-dialog>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改订单用户对话框 -->
    <orderuser-form
      v-model="open"
      :title="title"
      :form-data="form"
      :level-list="levelList"
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
  giftAmount,
} from "@/api/member/orderuser";
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

const { proxy } = getCurrentInstance();
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
  orderCount: null,
  taskProgress: null,
});

const giftVisible = ref(false);
const giftUserId = ref(null);

const modifyRules = reactive({
  taskProgress: [{ required: true, message: "请输入单数", trigger: "blur" }],
});

const modifyFormRef = ref(null);

// 修改登录密码相关
const loginPasswordVisible = ref(false);
const loginForm = reactive({
  id: null,
  password: null,
});
const loginRules = reactive({
  password: [
    { required: true, message: "请输入登录密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
});
const loginFormRef = ref(null);

// 修改交易密码相关
const tradePasswordVisible = ref(false);
const tradeForm = reactive({
  id: null,
  tradePassword: null,
});
const tradeRules = reactive({
  tradePassword: [
    { required: true, message: "请输入交易密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
});
const tradeFormRef = ref(null);

// 修改上级相关
const parentVisible = ref(false);
const parentForm = reactive({
  id: null,
  parentId: null,
});
const parentRules = reactive({
  parentId: [{ required: true, message: "请输入上级ID", trigger: "blur" }],
});
const parentFormRef = ref(null);

// 修改等级相关
const vipVisible = ref(false);
const vipForm = reactive({
  id: null,
  vipId: null,
});
const vipRules = reactive({
  vipId: [{ required: true, message: "请选择会员等级", trigger: "change" }],
});
const vipFormRef = ref(null);

// 修改信誉分相关
const reputationVisible = ref(false);
const reputationForm = reactive({
  id: null,
  currentReputation: null,
  newReputation: null,
});
const reputationRules = reactive({
  newReputation: [
    { required: true, message: "请输入修改后的信誉分", trigger: "blur" },
  ],
});
const reputationFormRef = ref(null);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: null,
    phoneNumber: null,
  },
});

const { queryParams, form } = toRefs(data);

/** 查询订单用户列表 */
function getList() {
  loading.value = true;
  listOrderuser(queryParams.value).then((response) => {
    orderuserList.value = response.rows;
    console.log(orderuserList.value);
    total.value = response.total;
    loading.value = false;
  });
  getLevel().then((res) => {
    levelList.value = res.data;
  });
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
    isFrozen: "0",
    isFake: "0",
    isBanned: "1",
    workLimit: null,
    isWithdrawalNotification: "1",
    productMatching: "0",
    accountStatus: "0",
    transactionStatus: "0",
    withdrawalStatus: "0",
    depositBlockWithdrawal: "0",
    remarks: null,
    version: null,
    createTime: null,
    updateTime: null,
    password: null,
    tradePassword: null,
    assistWithdrawalStatus: "0",
  };
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加用户";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getOrderuser(_id).then((response) => {
    form.value = response.data;
    form.value.password = "******";
    form.value.tradePassword = "******";
    open.value = true;
    title.value = "修改用户";
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
  console.log("handleOpenFlow, user id:", _id);
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
  console.log("handleOpenBonus id:", _id);
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
function handleDelete(row) {
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

/** 重置订单数确认并调用接口 */
function handleReset(row) {
  const _id = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认重置订单用户编号为"' + _id + '"的订单数量？')
    .then(function () {
      return resetOrder(_id);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("重置成功");
    })
    .catch(() => {});
}

function openModifyCount(row) {
  modifyForm.id = row.id || ids.value;

  // 给 row.taskProgress 和 row.memberLevel.orderCountPerDay 设置默认值
  const taskProgress = row.taskProgress ?? 0; // 默认为0
  const orderCountPerDay = row.memberLevel?.orderCountPerDay ?? 0; // 默认为0，且要确保 row.memberLevel 存在

  modifyForm.orderCount = `${taskProgress} / ${orderCountPerDay}`;

  modifyForm.taskProgress = taskProgress;
  modifyModalVisible.value = true;
}

async function submitModifyCount() {
  modifyFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await updateOrderuser(modifyForm);
      getList();
      modifyModalVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改失败");
    }
  });
}

// 修改登录密码
function handleModifyLoginPassword(row) {
  loginForm.id = row.id;
  loginForm.password = null;
  loginPasswordVisible.value = true;
}

async function submitLoginPassword() {
  loginFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await editPassword({ id: loginForm.id, password: loginForm.password });
      getList();
      loginPasswordVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改登录密码成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改登录密码失败");
    }
  });
}

// 修改交易密码
function handleModifyTradePassword(row) {
  tradeForm.id = row.id;
  tradeForm.tradePassword = null;
  tradePasswordVisible.value = true;
}

async function submitTradePassword() {
  tradeFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await editTradePassword({
        id: tradeForm.id,
        tradePassword: tradeForm.tradePassword,
      });
      getList();
      tradePasswordVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改交易密码成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改交易密码失败");
    }
  });
}

// 修改上级
function handleModifyParent(row) {
  parentForm.id = row.id;
  parentForm.parentId = null;
  parentVisible.value = true;
}

async function submitModifyParent() {
  parentFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await editParentId({ id: parentForm.id, parentId: parentForm.parentId });
      getList();
      parentVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改上级成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改上级失败");
    }
  });
}

// 修改等级
function handleModifyVip(row) {
  vipForm.id = row.id;
  vipForm.vipId = row.vipId || null;
  vipVisible.value = true;
}

async function submitModifyVip() {
  vipFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await updateOrderuser({ id: vipForm.id, vipId: vipForm.vipId });
      getList();
      vipVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改等级成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改等级失败");
    }
  });
}

// 修改信誉分
function handleModifyReputation(row) {
  reputationForm.id = row.id;
  reputationForm.currentReputation = row.reputationScore || 100;
  reputationForm.newReputation = row.reputationScore || 100;
  reputationVisible.value = true;
}

async function submitModifyReputation() {
  reputationFormRef?.value?.validate?.(async (valid) => {
    if (!valid) return;
    try {
      await updateOrderuser({
        id: reputationForm.id,
        reputationScore: reputationForm.newReputation,
      });
      getList();
      reputationVisible.value = false;
      proxy.$modal.msgSuccess && proxy.$modal.msgSuccess("修改信誉分成功");
    } catch (err) {
      proxy.$modal.msgError &&
        proxy.$modal.msgError(err?.message || "修改信誉分失败");
    }
  });
}

// 更多菜单操作
function handleGift(row) {
  giftUserId.value = row.id;
  giftVisible.value = true;
  console.log(123);
}

function handleSubMembers(row) {
  // 打开下级会员抽屉
  subDrawerUserId.value = row.id;
  subDrawerVisible.value = true;
}

function handleModifySignDays(row) {
  console.log("修改签到天数", row.id);
  // TODO: 实现修改签到天数功能
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
  const isCurrentlyFake = row.isFake === "1";
  const newValue = isCurrentlyFake ? "0" : "1";
  const action = isCurrentlyFake ? "设为真人" : "设为假人";
  proxy.$modal
    .confirm(`是否确认${action}？`)
    .then(async () => {
      try {
        await updateOrderuser({ id: row.id, isFake: newValue });
        proxy.$modal.msgSuccess(`${action}成功`);
        getList();
      } catch (err) {
        proxy.$modal.msgError(err.message || `${action}失败`);
      }
    })
    .catch(() => {});
}

async function handleToggleAccountStatus(row) {
  const newValue = row.accountStatus === "0" ? "1" : "0";
  const action = row.accountStatus === "0" ? "启用" : "禁用";
  proxy.$modal
    .confirm(`是否确认${action}账户？`)
    .then(async () => {
      try {
        await updateOrderuser({ id: row.id, accountStatus: newValue });
        proxy.$modal.msgSuccess(`${action}账户成功`);
        getList();
      } catch (err) {
        proxy.$modal.msgError(err.message || `${action}账户失败`);
      }
    })
    .catch(() => {});
}

async function handleToggleTransactionStatus(row) {
  const newValue = row.transactionStatus === "0" ? "1" : "0";
  const action = row.transactionStatus === "0" ? "启用" : "禁用";
  proxy.$modal
    .confirm(`是否确认${action}交易？`)
    .then(async () => {
      try {
        await updateOrderuser({ id: row.id, transactionStatus: newValue });
        proxy.$modal.msgSuccess(`${action}交易成功`);
        getList();
      } catch (err) {
        proxy.$modal.msgError(err.message || `${action}交易失败`);
      }
    })
    .catch(() => {});
}

async function handleToggleWithdrawalStatus(row) {
  const newValue = row.withdrawalStatus === "0" ? "1" : "0";
  const action = row.withdrawalStatus === "0" ? "启用" : "禁用";
  proxy.$modal
    .confirm(`是否确认${action}提现？`)
    .then(async () => {
      try {
        await updateOrderuser({ id: row.id, withdrawalStatus: newValue });
        proxy.$modal.msgSuccess(`${action}提现成功`);
        getList();
      } catch (err) {
        proxy.$modal.msgError(err.message || `${action}提现失败`);
      }
    })
    .catch(() => {});
}

async function handleToggleAssistWithdrawalStatus(row) {
  const newValue = row.assistWithdrawalStatus === "0" ? "1" : "0";
  const action = row.assistWithdrawalStatus === "0" ? "启用" : "禁用";
  proxy.$modal
    .confirm(`是否确认${action}协助金提现？`)
    .then(async () => {
      try {
        await updateOrderuser({ id: row.id, assistWithdrawalStatus: newValue });
        proxy.$modal.msgSuccess(`${action}协助金提现成功`);
        getList();
      } catch (err) {
        proxy.$modal.msgError(err.message || `${action}协助金提现失败`);
      }
    })
    .catch(() => {});
}

getList();
</script>
<style scoped>
.button-flex {
  display: flex;
  flex-wrap: wrap; /* 允许换行 */
  justify-content: flex-start; /* 左对齐按钮 */
  gap: 10px 0; /* 设置按钮之间的间距 */
}
.el-button {
  height: 26px;
  font-size: 12px; /* 设置按钮字体大小 */
  padding: 6px 12px; /* 设置按钮内边距 */
}
.bt-button {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
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
