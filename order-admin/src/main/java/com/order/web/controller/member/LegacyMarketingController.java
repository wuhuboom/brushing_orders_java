package com.order.web.controller.member;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.member.service.ILegacyMarketingService;

/**
 * Legacy customer-management compatibility Controller.
 */
@RestController
@RequestMapping("/member/legacy")
public class LegacyMarketingController extends BaseController
{
    @Autowired
    private ILegacyMarketingService legacyMarketingService;

    @PreAuthorize("@ss.hasPermi('member:authrecord:list')")
    @GetMapping("/authRecords/list")
    public TableDataInfo authRecordList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectAuthRecordList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:authrecord:query')")
    @GetMapping("/authRecords/{id}")
    public AjaxResult authRecordInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectAuthRecordById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:authrecord:add')")
    @Log(title = "授权记录", businessType = BusinessType.INSERT)
    @PostMapping("/authRecords")
    public AjaxResult addAuthRecord(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertAuthRecord(data));
    }

    @PreAuthorize("@ss.hasPermi('member:authrecord:edit')")
    @Log(title = "授权记录", businessType = BusinessType.UPDATE)
    @PutMapping("/authRecords")
    public AjaxResult editAuthRecord(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateAuthRecord(data));
    }

    @PreAuthorize("@ss.hasPermi('member:authrecord:remove')")
    @Log(title = "授权记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/authRecords/{ids}")
    public AjaxResult removeAuthRecord(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteAuthRecordByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('member:dateStatistics:list')")
    @GetMapping("/memberDateStatistics/list")
    public TableDataInfo memberDateStatistics(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectMemberDateStatistics(params));
    }

    @PreAuthorize("@ss.hasPermi('member:statistics:list')")
    @GetMapping("/memberStatistics/list")
    public TableDataInfo memberStatistics(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectMemberStatistics(params));
    }

    @PreAuthorize("@ss.hasPermi('member:performance:list')")
    @GetMapping("/performanceStatistics/list")
    public TableDataInfo performanceStatistics(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectPerformanceStatistics(params));
    }

    @PreAuthorize("@ss.hasPermi('member:performance:export')")
    @Log(title = "业绩统计", businessType = BusinessType.EXPORT)
    @PostMapping("/performanceStatistics/export")
    public void exportPerformanceStatistics(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException
    {
        exportCsv(response, "performance-statistics.csv", legacyMarketingService.selectPerformanceStatistics(params));
    }

    @PreAuthorize("@ss.hasPermi('member:duplicateIps:list')")
    @GetMapping("/duplicateIps/list")
    public TableDataInfo duplicateIps(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectDuplicateIpMembers(params));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoAccount:list')")
    @GetMapping("/yuebaoAccounts/list")
    public TableDataInfo yuebaoAccountList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectYuebaoAccountList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoAccount:query')")
    @GetMapping("/yuebaoAccounts/{id}")
    public AjaxResult yuebaoAccountInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectYuebaoAccountById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoAccount:add')")
    @Log(title = "余额宝账户", businessType = BusinessType.INSERT)
    @PostMapping("/yuebaoAccounts")
    public AjaxResult addYuebaoAccount(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertYuebaoAccount(data));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoAccount:edit')")
    @Log(title = "余额宝账户", businessType = BusinessType.UPDATE)
    @PutMapping("/yuebaoAccounts")
    public AjaxResult editYuebaoAccount(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateYuebaoAccount(data));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoAccount:remove')")
    @Log(title = "余额宝账户", businessType = BusinessType.DELETE)
    @DeleteMapping("/yuebaoAccounts/{ids}")
    public AjaxResult removeYuebaoAccount(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteYuebaoAccountByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:list')")
    @GetMapping("/yuebaoFlows/list")
    public TableDataInfo yuebaoFlowList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectYuebaoFlowList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:query')")
    @GetMapping("/yuebaoFlows/{id}")
    public AjaxResult yuebaoFlowInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectYuebaoFlowById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:add')")
    @Log(title = "余额宝交易流水", businessType = BusinessType.INSERT)
    @PostMapping("/yuebaoFlows")
    public AjaxResult addYuebaoFlow(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertYuebaoFlow(data));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:edit')")
    @Log(title = "余额宝交易流水", businessType = BusinessType.UPDATE)
    @PutMapping("/yuebaoFlows")
    public AjaxResult editYuebaoFlow(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateYuebaoFlow(data));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:edit')")
    @Log(title = "余额宝交易流水", businessType = BusinessType.UPDATE)
    @PutMapping("/yuebaoFlows/hidden/{isHidden}/{ids}")
    public AjaxResult changeYuebaoFlowHidden(@PathVariable String isHidden, @PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.updateYuebaoFlowHiddenByIds(ids, isHidden));
    }

    @PreAuthorize("@ss.hasPermi('member:yuebaoFlow:remove')")
    @Log(title = "余额宝交易流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/yuebaoFlows/{ids}")
    public AjaxResult removeYuebaoFlow(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteYuebaoFlowByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('member:bulletin:list')")
    @GetMapping("/bulletins/list")
    public TableDataInfo bulletinList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectBulletinList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:bulletin:query')")
    @GetMapping("/bulletins/{id}")
    public AjaxResult bulletinInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectBulletinById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:bulletin:add')")
    @Log(title = "公告管理", businessType = BusinessType.INSERT)
    @PostMapping("/bulletins")
    public AjaxResult addBulletin(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertBulletin(data));
    }

    @PreAuthorize("@ss.hasPermi('member:bulletin:edit')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PutMapping("/bulletins")
    public AjaxResult editBulletin(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateBulletin(data));
    }

    @PreAuthorize("@ss.hasPermi('member:bulletin:remove')")
    @Log(title = "公告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/bulletins/{ids}")
    public AjaxResult removeBulletin(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteBulletinByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('member:websiteStatistics:list')")
    @GetMapping("/websiteStatistics/list")
    public TableDataInfo websiteStatistics(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectWebsiteStatistics(params));
    }

    @PreAuthorize("@ss.hasPermi('member:wallet:list')")
    @GetMapping("/wallets/list")
    public TableDataInfo walletList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectWalletList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:wallet:query')")
    @GetMapping("/wallets/{id}")
    public AjaxResult walletInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectWalletById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:wallet:add')")
    @Log(title = "钱包管理", businessType = BusinessType.INSERT)
    @PostMapping("/wallets")
    public AjaxResult addWallet(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertWallet(data));
    }

    @PreAuthorize("@ss.hasPermi('member:wallet:edit')")
    @Log(title = "钱包管理", businessType = BusinessType.UPDATE)
    @PutMapping("/wallets")
    public AjaxResult editWallet(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateWallet(data));
    }

    @PreAuthorize("@ss.hasPermi('member:wallet:remove')")
    @Log(title = "钱包管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/wallets/{ids}")
    public AjaxResult removeWallet(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteWalletByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('member:recruitment:list')")
    @GetMapping("/recruitments/list")
    public TableDataInfo recruitmentList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(legacyMarketingService.selectRecruitmentList(params));
    }

    @PreAuthorize("@ss.hasPermi('member:recruitment:query')")
    @GetMapping("/recruitments/{id}")
    public AjaxResult recruitmentInfo(@PathVariable Long id)
    {
        return success(legacyMarketingService.selectRecruitmentById(id));
    }

    @PreAuthorize("@ss.hasPermi('member:recruitment:add')")
    @Log(title = "人才招聘", businessType = BusinessType.INSERT)
    @PostMapping("/recruitments")
    public AjaxResult addRecruitment(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.insertRecruitment(data));
    }

    @PreAuthorize("@ss.hasPermi('member:recruitment:edit')")
    @Log(title = "人才招聘", businessType = BusinessType.UPDATE)
    @PutMapping("/recruitments")
    public AjaxResult editRecruitment(@RequestBody Map<String, Object> data)
    {
        return toAjax(legacyMarketingService.updateRecruitment(data));
    }

    @PreAuthorize("@ss.hasPermi('member:recruitment:remove')")
    @Log(title = "人才招聘", businessType = BusinessType.DELETE)
    @DeleteMapping("/recruitments/{ids}")
    public AjaxResult removeRecruitment(@PathVariable Long[] ids)
    {
        return toAjax(legacyMarketingService.deleteRecruitmentByIds(ids));
    }

    private void exportCsv(HttpServletResponse response, String fileName, List<Map<String, Object>> rows) throws IOException
    {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        try (PrintWriter writer = response.getWriter())
        {
            if (rows == null || rows.isEmpty())
            {
                return;
            }
            Set<String> headers = new LinkedHashSet<>();
            rows.forEach(row -> headers.addAll(row.keySet()));
            writer.println(String.join(",", headers));
            for (Map<String, Object> row : rows)
            {
                boolean first = true;
                for (String header : headers)
                {
                    if (!first)
                    {
                        writer.print(",");
                    }
                    writer.print(csv(row.get(header)));
                    first = false;
                }
                writer.println();
            }
        }
    }

    private String csv(Object value)
    {
        if (value == null)
        {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
