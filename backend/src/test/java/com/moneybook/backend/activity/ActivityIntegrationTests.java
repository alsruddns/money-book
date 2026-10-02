package com.moneybook.backend.activity;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.activity.service.ActivityService;
import com.moneybook.backend.category.dto.CreateCategoryRequest;
import com.moneybook.backend.category.service.CategoryService;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:activity_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ActivityIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private CategoryService categories;
    @Autowired private ActivityService activities;
    @Autowired private ActivityRepository activityRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void activityIsPersistedOnlyAfterSuccessfulMutationCommitAndCanBeFiltered() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Fixture f = tx.execute(status -> {
            User user = users.save(User.create("snapshot name", null));
            MoneyBook book = books.save(MoneyBook.create("house", user.getUserUid()));
            memberships.save(MoneyBookUser.owner(book, user.getUserUid()));
            return new Fixture(book.getMoneyBookUid(), user.getUserUid());
        });
        assertNotNull(f);
        JwtAuthenticationToken auth = auth(f.userUid());

        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            categories.create(f.bookUid(), new CreateCategoryRequest("rollback", TransactionType.EXPENSE, 0), auth);
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(0, activityRepository.search(f.bookUid(), null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements());

        var created = categories.create(f.bookUid(),
                new CreateCategoryRequest("food", TransactionType.EXPENSE, 0), auth);
        categories.create(f.bookUid(), new CreateCategoryRequest("transport", TransactionType.EXPENSE, 1), auth);
        tx.executeWithoutResult(status -> users.findById(f.userUid()).orElseThrow().changeNickname("new name"));
        var result = activities.search(f.bookUid(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                f.userUid(), ActivityType.CATEGORY_CREATED, null, 0, 20, auth);
        assertEquals(2, result.totalElements());
        assertEquals("snapshot name", result.content().getLast().actorNickname());
        assertEquals(created.categoryUid(), result.content().getLast().targetUid());
        assertEquals("카테고리 'food'를 만들었습니다.", result.content().getLast().summary());
        assertEquals(1, activities.search(f.bookUid(), null, null, f.userUid(), ActivityType.CATEGORY_CREATED,
                null, 1, 1, auth).content().size());
        assertThrows(RuntimeException.class, () -> activities.search(f.bookUid(), null, null, null,
                null, null, 0, 101, auth));
    }

    private JwtAuthenticationToken auth(Long uid) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("token").header("alg", "none")
                .subject(uid.toString()).issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60)).build());
    }
    private record Fixture(Long bookUid, Long userUid) { }
}
