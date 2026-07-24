package com.order.common.i18n;

import com.order.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;

/**
 * 多语言翻译对象 translations
 * 
 * @author order
 * @date 2025-12-16
 */
public class Translations extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 中文(简体) */
    @Excel(name = "中文(简体)")
    private String zhCn;

    /** 中文(繁体) */
    @Excel(name = "中文(繁体)")
    private String zhTw;

    /** 韩文 */
    @Excel(name = "韩文")
    private String koKr;

    /** 泰文 */
    @Excel(name = "泰文")
    private String thTh;

    /** 日文 */
    @Excel(name = "日文")
    private String jaJp;

    /** 葡萄牙语 */
    @Excel(name = "葡萄牙语")
    private String ptPt;

    /** 英文 */
    @Excel(name = "英文")
    private String enUs;

    @Excel(name = "阿拉伯语")
    private String arSa;

    @Excel(name = "西班牙语")
    private String esEs;

    @Excel(name = "瑞典语")
    private String svSe;

    @Excel(name = "意大利语")
    private String itIt;

    @Excel(name = "德语")
    private String deDe;

    @Excel(name = "挪威语")
    private String noNo;

    @Excel(name = "俄语")
    private String ruRu;

    @Excel(name = "匈牙利语")
    private String huHu;

    @Excel(name = "波兰语")
    private String plPl;

    @Excel(name = "斯洛伐克语")
    private String skSk;

    @Excel(name = "法语")
    private String frFr;

    @Excel(name = "捷克语")
    private String csCz;

    @Excel(name = "巴西葡萄牙语")
    private String ptBr;

    @Excel(name = "印地语")
    private String hiIn;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setZhCn(String zhCn) 
    {
        this.zhCn = zhCn;
    }

    public String getZhCn() 
    {
        return zhCn;
    }

    public void setZhTw(String zhTw) 
    {
        this.zhTw = zhTw;
    }

    public String getZhTw() 
    {
        return zhTw;
    }

    public void setKoKr(String koKr) 
    {
        this.koKr = koKr;
    }

    public String getKoKr() 
    {
        return koKr;
    }

    public void setThTh(String thTh) 
    {
        this.thTh = thTh;
    }

    public String getThTh() 
    {
        return thTh;
    }

    public void setJaJp(String jaJp) 
    {
        this.jaJp = jaJp;
    }

    public String getJaJp() 
    {
        return jaJp;
    }

    public void setPtPt(String ptPt) 
    {
        this.ptPt = ptPt;
    }

    public String getPtPt() 
    {
        return ptPt;
    }

    public void setEnUs(String enUs) 
    {
        this.enUs = enUs;
    }

    public String getEnUs() 
    {
        return enUs;
    }

    public String getArSa() {
        return arSa;
    }

    public void setArSa(String arSa) {
        this.arSa = arSa;
    }

    public String getEsEs() {
        return esEs;
    }

    public void setEsEs(String esEs) {
        this.esEs = esEs;
    }

    public String getSvSe() {
        return svSe;
    }

    public void setSvSe(String svSe) {
        this.svSe = svSe;
    }

    public String getItIt() {
        return itIt;
    }

    public void setItIt(String itIt) {
        this.itIt = itIt;
    }

    public String getDeDe() {
        return deDe;
    }

    public void setDeDe(String deDe) {
        this.deDe = deDe;
    }

    public String getNoNo() {
        return noNo;
    }

    public void setNoNo(String noNo) {
        this.noNo = noNo;
    }

    public String getRuRu() {
        return ruRu;
    }

    public void setRuRu(String ruRu) {
        this.ruRu = ruRu;
    }

    public String getHuHu() {
        return huHu;
    }

    public void setHuHu(String huHu) {
        this.huHu = huHu;
    }

    public String getPlPl() {
        return plPl;
    }

    public void setPlPl(String plPl) {
        this.plPl = plPl;
    }

    public String getSkSk() {
        return skSk;
    }

    public void setSkSk(String skSk) {
        this.skSk = skSk;
    }

    public String getFrFr() {
        return frFr;
    }

    public void setFrFr(String frFr) {
        this.frFr = frFr;
    }

    public String getCsCz() {
        return csCz;
    }

    public void setCsCz(String csCz) {
        this.csCz = csCz;
    }

    public String getPtBr() {
        return ptBr;
    }

    public void setPtBr(String ptBr) {
        this.ptBr = ptBr;
    }

    public String getHiIn() {
        return hiIn;
    }

    public void setHiIn(String hiIn) {
        this.hiIn = hiIn;
    }

    public boolean hasAnyValue() {
        return hasText(zhCn)
                || hasText(zhTw)
                || hasText(koKr)
                || hasText(thTh)
                || hasText(jaJp)
                || hasText(ptPt)
                || hasText(enUs)
                || hasText(arSa)
                || hasText(esEs)
                || hasText(svSe)
                || hasText(itIt)
                || hasText(deDe)
                || hasText(noNo)
                || hasText(ruRu)
                || hasText(huHu)
                || hasText(plPl)
                || hasText(skSk)
                || hasText(frFr)
                || hasText(csCz)
                || hasText(ptBr)
                || hasText(hiIn);
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("zhCn", getZhCn())
            .append("zhTw", getZhTw())
            .append("koKr", getKoKr())
            .append("thTh", getThTh())
            .append("jaJp", getJaJp())
            .append("ptPt", getPtPt())
            .append("enUs", getEnUs())
            .append("arSa", getArSa())
            .append("esEs", getEsEs())
            .append("svSe", getSvSe())
            .append("itIt", getItIt())
            .append("deDe", getDeDe())
            .append("noNo", getNoNo())
            .append("ruRu", getRuRu())
            .append("huHu", getHuHu())
            .append("plPl", getPlPl())
            .append("skSk", getSkSk())
            .append("frFr", getFrFr())
            .append("csCz", getCsCz())
            .append("ptBr", getPtBr())
            .append("hiIn", getHiIn())
            .toString();
    }
}
