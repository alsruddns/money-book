package com.moneybook.backend.admin;

import com.moneybook.backend.admin.repository.AdminReadRepository;
import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.enums.ActivityType;
import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.moneybook.repository.MoneyBookRepository;
import com.moneybook.backend.moneybook.repository.MoneyBookUserRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_read;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class AdminReadRepositoryTests {
    @Autowired private AdminReadRepository reads;
    @Autowired private UserRepository users;
    @Autowired private UserAuthRepository auths;
    @Autowired private MoneyBookRepository books;
    @Autowired private MoneyBookUserRepository memberships;
    @Autowired private ActivityRepository activities;

    @Test void listsAndDetailsUsePagedOperationalProjectionsAndAggregates() {
        User user=users.save(User.create("owner",null));
        auths.save(UserAuth.local(user,"owner@example.test","$2a$test"));
        MoneyBook book=books.save(MoneyBook.create("Family Book",user.getUserUid()));
        memberships.save(MoneyBookUser.owner(book,user.getUserUid()));

        var userPage=reads.users("owner@",null,SystemRole.USER,PageRequest.of(0,20));
        assertEquals(1,userPage.getTotalElements());
        assertEquals("owner@example.test",userPage.getContent().getFirst().loginId());
        assertEquals(0,reads.user(user.getUserUid()).orElseThrow().joinedMoneyBookCount());
        var bookPage=reads.moneyBooks("family",user.getUserUid(),PageRequest.of(0,20));
        assertEquals(1,bookPage.getTotalElements());
        assertEquals("owner",bookPage.getContent().getFirst().ownerNickname());
        assertEquals(1,bookPage.getContent().getFirst().memberCount());
        assertEquals(0,reads.moneyBook(book.getMoneyBookUid()).orElseThrow().transactionCount());
        assertEquals(1,reads.overview().totalMoneyBooks());
        assertEquals(0,activities.search(null,null,null,null,ActivityType.MONEY_BOOK_CREATED,null,
                PageRequest.of(0,20,org.springframework.data.domain.Sort.by("occurredAt"))).getTotalElements());
    }
}
