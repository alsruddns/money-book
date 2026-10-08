package com.moneybook.backend.board;

import tools.jackson.databind.ObjectMapper;
import com.moneybook.backend.board.controller.BoardController;
import com.moneybook.backend.board.repository.impl.BoardCategoryJpaRepository;
import com.moneybook.backend.board.repository.impl.BoardCommentJpaRepository;
import com.moneybook.backend.board.repository.impl.BoardPostJpaRepository;
import com.moneybook.backend.board.service.BoardService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.entity.BoardCategory;
import com.moneybook.backend.entity.BoardComment;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:board_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class BoardIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private BoardCategoryJpaRepository categories;
    @Autowired private BoardPostJpaRepository posts;
    @Autowired private BoardCommentJpaRepository comments;
    @Autowired private BoardService board;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private Long categoryUid;
    private Long userUid;
    private Long systemAdminUid;
    private Long superAdminUid;
    private Long otherUid;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        userUid = tx.execute(status -> users.save(User.create("민경운", null)).getUserUid());
        systemAdminUid = tx.execute(status -> {
            User user = User.create("시스템관리자", null);
            user.changeSystemRole(SystemRole.SYSTEM_ADMIN);
            return users.save(user).getUserUid();
        });
        superAdminUid = tx.execute(status -> {
            User user = User.create("홍길동님", null);
            user.changeSystemRole(SystemRole.SUPER_ADMIN);
            return users.save(user).getUserUid();
        });
        otherUid = tx.execute(status -> users.save(User.create("other", null)).getUserUid());
        categoryUid = tx.execute(status -> categories.save(new BoardCategory("category-" + System.nanoTime(), null, 1)).getCategoryUid());
    }

    @Test
    void postResponsesOnlyExposeMaskedAuthorDisplayNameAndHideSecretListContent() throws Exception {
        Long postUid = board.createPost(categoryUid, "private headline", "private body", true, auth(userUid)).getPostUid();
        BoardController controller = new BoardController(board);
        var page = controller.posts(null, null, 0, 20, auth(otherUid));
        var response = page.getContent().stream().filter(row -> row.postUid().equals(postUid)).findFirst().orElseThrow();
        assertEquals("비밀글입니다.", response.title());
        assertNull(response.content());
        assertEquals("민*운", response.authorDisplayName());
        String json = objectMapper.writeValueAsString(response);
        assertFalse(json.contains("민경운"));
        assertFalse(json.contains("loginId"));
        assertFalse(json.contains("email"));
        assertFalse(json.contains("authorUserUid"));
        assertFalse(json.contains("userUid"));
        assertEquals("private headline", controller.posts(null, "private", 0, 20, auth(userUid))
                .getContent().stream().filter(row -> row.postUid().equals(postUid)).findFirst().orElseThrow().title());
        assertTrue(controller.posts(null, "private", 0, 20, auth(otherUid)).getContent().isEmpty());
    }

    @Test
    void postSearchSupportsMissingAndCaseInsensitiveKeywordsAcrossTitleAndContent() {
        board.createPost(categoryUid, "Coffee TITLE", "Daily COFFEE notes", false, auth(userUid));
        assertEquals(1, board.posts(categoryUid, null, 0, 20, auth(userUid)).getTotalElements());
        assertEquals(1, board.posts(categoryUid, "cOfFeE", 0, 20, auth(userUid)).getTotalElements());
        assertEquals(1, board.posts(categoryUid, "NOTES", 0, 20, auth(userUid)).getTotalElements());
    }

    @Test
    void superAdminCanReadSecretPostAndSystemAdminAndOtherUsersCannot() {
        Long postUid = board.createPost(categoryUid, "secret", "body", true, auth(userUid)).getPostUid();
        assertEquals("secret", board.getPost(postUid, auth(userUid)).getTitle());
        assertEquals("secret", board.getPost(postUid, auth(superAdminUid)).getTitle());
        assertThrows(BusinessException.class, () -> board.getPost(postUid, auth(systemAdminUid)));
        assertThrows(BusinessException.class, () -> board.getPost(postUid, auth(otherUid)));
    }

    @Test
    void postOwnerCanEditAndDeleteWhileOtherUserCannotAndSuperAdminCanModerate() {
        Long postUid = board.createPost(categoryUid, "original", "body", false, auth(userUid)).getPostUid();
        assertThrows(BusinessException.class, () -> board.updatePost(postUid, "changed", "body", false, auth(otherUid)));
        assertThrows(BusinessException.class, () -> board.deletePost(postUid, auth(otherUid)));
        assertThrows(BusinessException.class, () -> board.deletePost(postUid, auth(systemAdminUid)));
        assertEquals("changed", board.updatePost(postUid, "changed", "body", false, auth(userUid)).getTitle());
        board.deletePost(postUid, auth(superAdminUid));
        assertTrue(posts.findById(postUid).orElseThrow().isDeleted());
    }

    @Test
    void onlySuperAdminCanSetNoticeAndNoticeIsReturnedInListOrdering() {
        Long regularUid = board.createPost(categoryUid, "regular", "body", false, auth(userUid)).getPostUid();
        Long noticeUid = board.createPost(categoryUid, "notice", "body", false, auth(userUid)).getPostUid();
        assertThrows(BusinessException.class, () -> board.notice(regularUid, true, auth(userUid)));
        assertThrows(BusinessException.class, () -> board.notice(regularUid, true, auth(systemAdminUid)));
        board.notice(noticeUid, true, auth(superAdminUid));
        var result = board.posts(null, null, 0, 20, auth(userUid));
        assertEquals(noticeUid, result.getContent().getFirst().getPostUid());
        assertTrue(result.getContent().getFirst().isNotice());
        assertFalse(java.util.Arrays.stream(BoardController.PostReq.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("notice")));
        assertFalse(java.util.Arrays.stream(BoardController.UpdatePostReq.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("notice")));
    }

    @Test
    void commentMutationsAreOwnerOnlyButSuperAdminCanModerate() {
        Long postUid = board.createPost(categoryUid, "title", "body", false, auth(userUid)).getPostUid();
        BoardComment comment = board.createComment(postUid, null, "comment", false, auth(userUid));
        assertThrows(BusinessException.class, () -> board.updateComment(comment.getCommentUid(), "other", false, auth(otherUid)));
        assertThrows(BusinessException.class, () -> board.deleteComment(comment.getCommentUid(), auth(otherUid)));
        assertThrows(BusinessException.class, () -> board.deleteComment(comment.getCommentUid(), auth(systemAdminUid)));
        assertEquals("updated", board.updateComment(comment.getCommentUid(), "updated", false, auth(userUid)).getContent());
        board.deleteComment(comment.getCommentUid(), auth(superAdminUid));
        assertEquals("삭제된 댓글입니다.", comments.findById(comment.getCommentUid()).orElseThrow().getContent());
    }

    @Test
    void thirdReplyLevelIsRejectedAndDeletingParentRetainsRepliesWithPlaceholder() {
        Long postUid = board.createPost(categoryUid, "title", "body", false, auth(userUid)).getPostUid();
        BoardComment root = board.createComment(postUid, null, "root comment", false, auth(userUid));
        BoardComment reply = board.createComment(postUid, root.getCommentUid(), "reply text", false, auth(otherUid));
        assertThrows(BusinessException.class, () -> board.createComment(postUid, reply.getCommentUid(), "third", false, auth(userUid)));
        board.deleteComment(root.getCommentUid(), auth(userUid));
        List<BoardComment> rows = board.comments(postUid, auth(otherUid));
        assertEquals(2, rows.size());
        assertEquals("삭제된 댓글입니다.", rows.get(0).getContent());
        assertTrue(rows.stream().anyMatch(row -> row.getCommentUid().equals(reply.getCommentUid())));
    }

    @Test
    void secretCommentsAreVisibleOnlyToTheirAuthorOrSuperAdmin() {
        Long postUid = board.createPost(categoryUid, "title", "body", false, auth(userUid)).getPostUid();
        BoardComment secret = board.createComment(postUid, null, "secret comment body", true, auth(otherUid));
        assertTrue(board.canReadComment(secret, auth(otherUid)));
        assertTrue(board.canReadComment(secret, auth(superAdminUid)));
        assertFalse(board.canReadComment(secret, auth(userUid)));
        assertFalse(board.canReadComment(secret, auth(systemAdminUid)));
        assertFalse(board.canReadComment(secret, auth(userUid)));
        var view = new BoardController(board).comments(postUid, auth(userUid)).getFirst();
        assertEquals("비밀 댓글입니다.", view.content());
        assertFalse(view.content().contains("secret comment body"));
        assertEquals("o***r", view.authorDisplayName());
        assertFalse(objectMapper.writeValueAsString(view).contains("secret comment body"));
        var superAdminComment = new BoardController(board).comments(postUid, auth(superAdminUid)).getFirst();
        assertEquals("secret comment body", superAdminComment.content());
    }

    @Test
    void commentAndReplyListingUsesBatchAuthorLookupInsteadOfPerCommentQueries() {
        Long postUid = board.createPost(categoryUid, "comment query count", "body", false, auth(userUid)).getPostUid();
        for (int i = 0; i < 10; i++) {
            BoardComment root = board.createComment(postUid, null, "root " + i, false, auth(userUid));
            board.createComment(postUid, root.getCommentUid(), "reply " + i, false, auth(otherUid));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var rows = new BoardController(board).comments(postUid, auth(otherUid));
        long queryCount = statistics.getPrepareStatementCount();
        assertEquals(10, rows.size());
        assertEquals(1, rows.getFirst().replies().size());
        assertTrue(queryCount <= 5, "댓글/대댓글 작성자 조회 query는 전체 수와 무관해야 함: " + queryCount);
    }

    @Test
    void onlySuperAdminCanManageCategoriesAndInactiveCategoriesAreNotPublic() {
        assertThrows(BusinessException.class, () -> board.createCategory("user", null, 1, auth(userUid)));
        assertThrows(BusinessException.class, () -> board.updateCategory(categoryUid, "system", null, 1, false, auth(systemAdminUid)));
        board.updateCategory(categoryUid, "inactive-" + System.nanoTime(), null, 1, false, auth(superAdminUid));
        assertFalse(board.categories(auth(userUid)).stream().anyMatch(category -> category.getCategoryUid().equals(categoryUid)));
        assertTrue(board.adminCategories(auth(superAdminUid)).stream().anyMatch(category -> category.getCategoryUid().equals(categoryUid)));
    }

    @Test
    void anonymousAndInactiveUsersCannotUseBoardService() {
        assertThrows(BusinessException.class, () -> board.categories(auth(999999L)));
        Long blockedUid = new TransactionTemplate(transactionManager).execute(status -> {
            User blocked = User.create("blocked", null);
            blocked.changeStatus(UserStatus.BLOCKED);
            return users.save(blocked).getUserUid();
        });
        assertThrows(BusinessException.class, () -> board.categories(auth(blockedUid)));
        Long withdrawnUid = new TransactionTemplate(transactionManager).execute(status -> {
            User withdrawn = users.save(User.create("withdrawn", null));
            withdrawn.withdraw();
            return withdrawn.getUserUid();
        });
        assertThrows(BusinessException.class, () -> board.categories(auth(withdrawnUid)));
    }

    @Test
    void postListUsesBoundedBatchQueriesForAuthorsAndCommentCounts() throws Exception {
        for (int i = 0; i < 20; i++) board.createPost(categoryUid, "post " + i, "body", false, auth(userUid));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        var page = new BoardController(board).posts(null, null, 0, 20, auth(otherUid));
        long queryCount = statistics.getPrepareStatementCount();
        assertEquals(20, page.getContent().size());
        assertEquals("민*운", page.getContent().getFirst().authorDisplayName());
        assertEquals(0, page.getContent().getFirst().commentCount());
        assertFalse(objectMapper.writeValueAsString(page).contains("민경운"));
        assertTrue(queryCount <= 6, "20개 게시글 작성자/댓글 집계가 고정된 query 수로 조회되어야 함: " + queryCount);
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test-token").header("alg", "none")
                .subject(uid.toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
    }
}
