package com.order.web.controller.member;

import com.order.api.service.RechargeApplicationService;
import com.order.common.core.domain.AjaxResult;
import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.service.IGoodsRechargeRecordService;
import com.order.member.service.IOrderInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinancialRecordControllerContractTest {
    @Mock
    private IGoodsRechargeRecordService rechargeRecordService;

    @Mock
    private RechargeApplicationService rechargeApplicationService;

    @Mock
    private IOrderInfoService orderInfoService;

    @Spy
    @InjectMocks
    private GoodsRechargeRecordController rechargeController = new GoodsRechargeRecordController();

    @InjectMocks
    private OrderInfoController orderInfoController;

    private MockMvc orderMockMvc;

    @BeforeEach
    void setUp() {
        orderMockMvc = MockMvcBuilders.standaloneSetup(orderInfoController).build();
    }

    @Test
    void rechargeEditRequiresAnId() {
        GoodsRechargeRecord request = new GoodsRechargeRecord();
        request.setIsHidden("1");

        AjaxResult result = rechargeController.edit(request);

        assertEquals(400, result.get(AjaxResult.CODE_TAG));
        assertEquals("充值记录ID不能为空", result.get(AjaxResult.MSG_TAG));
        verifyNoInteractions(rechargeRecordService);
    }

    @Test
    void rechargeEditAcceptsOnlyExplicitVisibilityValues() {
        for (String isHidden : new String[] { "", "2" }) {
            GoodsRechargeRecord request = new GoodsRechargeRecord();
            request.setId(7L);
            request.setIsHidden(isHidden);

            AjaxResult result = rechargeController.edit(request);

            assertEquals(400, result.get(AjaxResult.CODE_TAG));
            assertEquals("只能修改显示/隐藏状态", result.get(AjaxResult.MSG_TAG));
        }
        verifyNoInteractions(rechargeRecordService);
    }

    @Test
    void rechargeEditAllowsRemarkOnlyUpdates() {
        doReturn("tester").when(rechargeController).getUsername();
        when(rechargeRecordService.updateGoodsRechargeRecord(any())).thenReturn(1);
        GoodsRechargeRecord request = new GoodsRechargeRecord();
        request.setId(7L);
        request.setRemark("reviewed");

        AjaxResult result = rechargeController.edit(request);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(rechargeRecordService).updateGoodsRechargeRecord(argThat(update ->
                Long.valueOf(7L).equals(update.getId())
                        && "reviewed".equals(update.getRemark())
                        && update.getIsHidden() == null
                        && "tester".equals(update.getUpdateBy())));
    }

    @Test
    void orderCreationReturnsRealMethodNotAllowedStatus() throws Exception {
        orderMockMvc.perform(post("/member/orderinfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void orderDeletionReturnsRealMethodNotAllowedStatus() throws Exception {
        orderMockMvc.perform(delete("/member/orderinfo/7"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }
}
