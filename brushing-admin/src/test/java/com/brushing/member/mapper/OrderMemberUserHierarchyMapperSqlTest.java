package com.brushing.member.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMemberUserHierarchyMapperSqlTest {

    @Test
    void genericUpdateCannotWriteHierarchyAndNoBroadReplaceRemains() throws IOException {
        String xml;
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream("mapper/OrderMemberUserMapper.xml")) {
            if (stream == null) {
                throw new IOException("OrderMemberUserMapper.xml not found");
            }
            xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int start = xml.indexOf("<update id=\"updateOrderMemberUser\"");
        int end = xml.indexOf("</update>", start);
        String genericUpdate = xml.substring(start, end);
        assertFalse(genericUpdate.contains("parent_id"));
        assertFalse(genericUpdate.contains("ancestors"));
        assertFalse(xml.toUpperCase().contains("REPLACE(ANCESTORS"));
        assertTrue(xml.contains("<update id=\"changeMemberParent\""));

        int memberLockStart = xml.indexOf("<select id=\"selectMemberHierarchiesForUpdate\"");
        int memberLockEnd = xml.indexOf("</select>", memberLockStart);
        String memberLock = xml.substring(memberLockStart, memberLockEnd);
        assertTrue(memberLock.contains("WHERE id IN"));
        assertTrue(memberLock.contains("ORDER BY id"));
        assertTrue(memberLock.contains("FOR UPDATE"));

        int childrenStart = xml.indexOf("<select id=\"selectMemberChildrenForUpdate\"");
        int childrenEnd = xml.indexOf("</select>", childrenStart);
        String childLock = xml.substring(childrenStart, childrenEnd);
        assertTrue(childLock.contains("FROM order_member_user"));
        assertTrue(childLock.contains("WHERE parent_id IN"));
        assertTrue(childLock.contains("ORDER BY id"));
        assertTrue(childLock.contains("FOR UPDATE"));
        assertFalse(childLock.contains("WITH RECURSIVE"));

        int batchStart = xml.indexOf("<update id=\"updateMemberAncestorsBatch\"");
        int batchEnd = xml.indexOf("</update>", batchStart);
        String descendantUpdate = xml.substring(batchStart, batchEnd);
        assertTrue(descendantUpdate.contains("version = COALESCE(version, 0) + 1"));

        int rootStart = xml.indexOf("<update id=\"changeMemberParent\"");
        int rootEnd = xml.indexOf("</update>", rootStart);
        String rootUpdate = xml.substring(rootStart, rootEnd);
        assertTrue(rootUpdate.contains("version = version + 1"));
        assertTrue(rootUpdate.contains("AND version = #{version}"));
    }
}
