package com.order.member.mapper;

import com.order.member.domain.GoodsTransactionFlow;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminListMapperContractTest {

    @Test
    void listQueriesUseStableDefaultOrdering() throws IOException {
        assertTrue(mapper("GoodsRechargeRecordMapper.xml")
                .contains("order by gr.create_time desc, gr.id desc"));
        assertTrue(mapper("GoodsTransactionFlowMapper.xml")
                .contains("order by gtf.created_time desc, gtf.id desc"));
        assertTrue(mapper("OrderWithdrawalMapper.xml")
                .contains("order by ow.create_time desc, ow.id desc"));
        assertTrue(mapper("OrderInfoMapper.xml")
                .contains("order by oi.create_time desc, oi.id desc"));
    }

    @Test
    void withdrawalTypeFilterUsesWithdrawalSnapshotInsteadOfMutableAccount() throws IOException {
        String xml = mapper("OrderWithdrawalMapper.xml");

        assertTrue(xml.contains("and ow.withdrawal_type = #{withdrawalType}"));
        assertFalse(xml.contains("and gwa.type = #{withdrawalType}"));
    }

    @Test
    void blankOrderUsernameDoesNotCreateAnEmptyEqualityFilter() throws IOException {
        assertTrue(mapper("OrderInfoMapper.xml")
                .contains("username != null and username != ''"));
    }

    @Test
    void legacyBonusFlowsNormalizeByManualOrigin() throws IOException {
        String xml = mapper("GoodsTransactionFlowMapper.xml");

        assertTrue(xml.contains("<sql id=\"normalizedTransactionType\">"));
        assertTrue(xml.contains("gtf.transaction_type = 'bonus'"));
        assertTrue(xml.contains("gtf.remark like 'manual-bonus:%'"));
        assertTrue(xml.contains("then 'rwjl'"));
        assertTrue(xml.contains("then 'jj'"));
        assertTrue(xml.contains("<when test=\"transactionType == 'rwjl'\">"));
        assertTrue(xml.contains("<when test=\"transactionType == 'jj'\">"));
        assertTrue(xml.contains("gtf.remark is null"));
        assertTrue(xml.contains("gtf.remark not like 'manual-bonus:%'"));
        assertTrue(xml.contains("<include refid=\"normalizedTransactionType\"/> as transaction_type"));
        assertFalse(xml.contains("when gtf.transaction_type = 'bonus' then 'rwjl'"));
        assertTrue(xml.indexOf("then 'rwjl'") < xml.indexOf("then 'jj'"));
    }

    @Test
    void manualBonusTaskRewardsAreExcludedFromLegacyGiftStatistics() throws IOException {
        String xml = mapper("LegacyMarketingMapper.xml");

        assertFalse(xml.contains("transaction_type in ('give', 'gift', 'bonus')"));
        assertTrue(xml.contains("transaction_type in ('give', 'gift')"));
        assertTrue(xml.contains("remark is null or remark not like 'manual-bonus:%'"));
        assertFalse(xml.contains("'rwjl'"));
    }

    @Test
    void transactionFlowMapperRemainsParsableByMyBatis() throws IOException {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry()
                .registerAlias("GoodsTransactionFlow", GoodsTransactionFlow.class);
        String resource = "mapper/GoodsTransactionFlowMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(input, "Missing mapper resource " + resource);
            new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments())
                    .parse();
        }

        String namespace = GoodsTransactionFlowMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".selectGoodsTransactionFlowList"));
        assertTrue(configuration.hasStatement(namespace + ".selectGoodsTransactionFlowById"));
        assertTrue(configuration.hasStatement(namespace + ".selectPublicByUserId"));
    }

    @Test
    void legacyMarketingMapperRemainsParsableByMyBatis() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/LegacyMarketingMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(input, "Missing mapper resource " + resource);
            new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments())
                    .parse();
        }

        String namespace = LegacyMarketingMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".selectMemberDateStatistics"));
        assertTrue(configuration.hasStatement(namespace + ".selectMemberStatistics"));
    }

    private String mapper(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/" + name)) {
            assertNotNull(input, "Missing mapper resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
