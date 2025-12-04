import request from "@/utils/request";

// 查询奖品配置列表
export function listLotteryprize() {
  return request({
    url: "/member/lotteryprize/list",
    method: "get",
  });
}

// 查询奖品配置详细
export function getLotteryprize(id) {
  return request({
    url: "/member/lotteryprize/" + id,
    method: "get",
  });
}

// 新增奖品配置
export function addLotteryprize(data) {
  return request({
    url: "/member/lotteryprize",
    method: "post",
    data: data,
  });
}

// 修改奖品配置
export function updateLotteryprize(data) {
  return request({
    url: "/member/lotteryprize",
    method: "put",
    data: data,
  });
}

// 删除奖品配置
export function delLotteryprize(id) {
  return request({
    url: "/member/lotteryprize/" + id,
    method: "delete",
  });
}
