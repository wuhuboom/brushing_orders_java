package com.brushing.member.service.impl;

import com.brushing.BrushingApplication;
import com.brushing.common.exception.ServiceException;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = BrushingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.devtools.restart.enabled=false",
                "spring.task.scheduling.enabled=false"
        }
)
@Transactional
@EnabledIfEnvironmentVariable(named = "BRUSHING_RUN_INTEGRATION_TESTS", matches = "(?i)true")
class OrderMemberUserParentBindingIntegrationTest {

    @Autowired
    private IOrderMemberUserService userService;

    @Test
    void bindsByTopLevelUserIdAndInviteCode() {
        String prefix = uniquePrefix();
        OrderMemberUser root = insertUser(prefix + "root", "0");

        assertEquals(0L, root.getParentId());
        assertEquals("0", root.getAncestors());
        assertNotNull(root.getInviteCode());

        OrderMemberUser byId = insertUser(prefix + "byid", root.getId().toString());
        assertEquals(root.getId(), byId.getParentId());
        assertEquals("0," + root.getId(), byId.getAncestors());

        OrderMemberUser byInviteCode = insertUser(
                prefix + "bycode",
                root.getInviteCode().toLowerCase(java.util.Locale.ROOT)
        );
        assertEquals(root.getId(), byInviteCode.getParentId());
        assertEquals("0," + root.getId(), byInviteCode.getAncestors());
    }

    @Test
    void rejectsInvalidSelfAndDescendantParents() {
        String prefix = uniquePrefix();
        OrderMemberUser root = insertUser(prefix + "root", "0");
        OrderMemberUser child = insertUser(prefix + "child", root.getInviteCode());

        assertThrows(ServiceException.class,
                () -> insertUser(prefix + "invalid", "NO_SUCH_PARENT"));

        assertThrows(ServiceException.class,
                () -> changeParent(root, root.getId().toString()));

        assertThrows(ServiceException.class,
                () -> changeParent(root, child.getInviteCode()));

        assertThrows(ServiceException.class,
                () -> changeParent(child, "999999999999999999"));
    }

    @Test
    void movingBranchRebuildsOnlyThatBranchesAncestors() {
        String prefix = uniquePrefix();
        OrderMemberUser rootA = insertUser(prefix + "roota", "0");
        OrderMemberUser rootB = insertUser(prefix + "rootb", "0");
        OrderMemberUser moving = insertUser(prefix + "moving", rootA.getId().toString());
        OrderMemberUser movingChild = insertUser(prefix + "movingchild", moving.getInviteCode());
        OrderMemberUser movingGrandchild = insertUser(prefix + "movinggrand", movingChild.getInviteCode());
        OrderMemberUser sibling = insertUser(prefix + "sibling", rootA.getInviteCode());
        OrderMemberUser siblingChild = insertUser(prefix + "siblingchild", sibling.getId().toString());
        OrderMemberUser beforeMoving = userService.selectOrderMemberUserById(moving.getId());
        OrderMemberUser beforeMovingChild = userService.selectOrderMemberUserById(movingChild.getId());
        OrderMemberUser beforeMovingGrandchild =
                userService.selectOrderMemberUserById(movingGrandchild.getId());
        OrderMemberUser beforeSibling = userService.selectOrderMemberUserById(sibling.getId());

        assertEquals(1, changeParent(moving, rootB.getInviteCode()));

        OrderMemberUser reloadedMoving = userService.selectOrderMemberUserById(moving.getId());
        OrderMemberUser reloadedMovingChild = userService.selectOrderMemberUserById(movingChild.getId());
        OrderMemberUser reloadedMovingGrandchild = userService.selectOrderMemberUserById(movingGrandchild.getId());
        OrderMemberUser reloadedSibling = userService.selectOrderMemberUserById(sibling.getId());
        OrderMemberUser reloadedSiblingChild = userService.selectOrderMemberUserById(siblingChild.getId());

        assertEquals(rootB.getId(), reloadedMoving.getParentId());
        assertEquals("0," + rootB.getId(), reloadedMoving.getAncestors());
        assertEquals("0," + rootB.getId() + "," + moving.getId(), reloadedMovingChild.getAncestors());
        assertEquals("0," + rootB.getId() + "," + moving.getId() + "," + movingChild.getId(),
                reloadedMovingGrandchild.getAncestors());
        assertEquals(beforeMoving.getVersion() + 1, reloadedMoving.getVersion());
        assertEquals(beforeMovingChild.getVersion() + 1, reloadedMovingChild.getVersion());
        assertEquals(beforeMovingGrandchild.getVersion() + 1, reloadedMovingGrandchild.getVersion());

        assertEquals(rootA.getId(), reloadedSibling.getParentId());
        assertEquals("0," + rootA.getId(), reloadedSibling.getAncestors());
        assertEquals("0," + rootA.getId() + "," + sibling.getId(), reloadedSiblingChild.getAncestors());
        assertEquals(beforeSibling.getVersion(), reloadedSibling.getVersion());
    }

    @Test
    void movesBranchToRoot() {
        String prefix = uniquePrefix();
        OrderMemberUser root = insertUser(prefix + "root", "0");
        OrderMemberUser moving = insertUser(prefix + "moving", root.getInviteCode());
        OrderMemberUser child = insertUser(prefix + "child", moving.getInviteCode());

        assertEquals(1, changeParent(moving, "0"));

        OrderMemberUser reloadedMoving = userService.selectOrderMemberUserById(moving.getId());
        OrderMemberUser reloadedChild = userService.selectOrderMemberUserById(child.getId());
        assertEquals(0L, reloadedMoving.getParentId());
        assertEquals("0", reloadedMoving.getAncestors());
        assertEquals("0," + moving.getId(), reloadedChild.getAncestors());
    }

    @Test
    void staleVersionAndGenericEditBypassLeaveHierarchyUntouched() {
        String prefix = uniquePrefix();
        OrderMemberUser rootA = insertUser(prefix + "roota", "0");
        OrderMemberUser rootB = insertUser(prefix + "rootb", "0");
        OrderMemberUser moving = insertUser(prefix + "moving", rootA.getInviteCode());
        OrderMemberUser child = insertUser(prefix + "child", moving.getInviteCode());
        Long staleVersion = userService.selectOrderMemberUserById(moving.getId()).getVersion();

        OrderMemberUser ordinaryEdit = new OrderMemberUser();
        ordinaryEdit.setId(moving.getId());
        ordinaryEdit.setVersion(staleVersion);
        ordinaryEdit.setRemark("advance-version");
        assertEquals(1, userService.updateOrderMemberUser(ordinaryEdit));

        assertThrows(ServiceException.class,
                () -> userService.changeParent(moving.getId(), rootB.getInviteCode(), staleVersion));

        OrderMemberUser bypass = new OrderMemberUser();
        bypass.setId(moving.getId());
        bypass.setVersion(userService.selectOrderMemberUserById(moving.getId()).getVersion());
        bypass.setParentId(rootB.getId());
        assertThrows(ServiceException.class, () -> userService.updateOrderMemberUser(bypass));

        OrderMemberUser reloadedMoving = userService.selectOrderMemberUserById(moving.getId());
        OrderMemberUser reloadedChild = userService.selectOrderMemberUserById(child.getId());
        assertEquals(rootA.getId(), reloadedMoving.getParentId());
        assertEquals("0," + rootA.getId(), reloadedMoving.getAncestors());
        assertEquals("0," + rootA.getId() + "," + moving.getId(), reloadedChild.getAncestors());
    }

    private OrderMemberUser insertUser(String username, String parentIdentifier) {
        OrderMemberUser user = new OrderMemberUser();
        user.setUsername(username);
        user.setPhone("9" + Long.toUnsignedString(System.nanoTime(), 10));
        user.setPassword("integration-test-only");
        user.setLevelId(1L);
        user.setParentIdentifier(parentIdentifier);
        user.setAccountStatus("0");
        user.setTradeStatus("0");
        user.setWithdrawStatus("0");
        user.setIsReal("Y");
        user.setSex("0");
        user.setTaskStatus("0");
        assertEquals(1, userService.insertOrderMemberUser(user));
        assertNotNull(user.getId());
        return user;
    }

    private int changeParent(OrderMemberUser existing, String parentIdentifier) {
        Long version = userService.selectOrderMemberUserById(existing.getId()).getVersion();
        return userService.changeParent(existing.getId(), parentIdentifier, version);
    }

    private String uniquePrefix() {
        return "codex_pb_" + Long.toUnsignedString(System.nanoTime(), 36) + "_";
    }
}
