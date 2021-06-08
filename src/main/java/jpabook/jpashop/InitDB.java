package jpabook.jpashop;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

@Component
@RequiredArgsConstructor
public class InitDB {

    private final InitService initService;


    @PostConstruct // 기본적으로 시작하면서 값을 넣고싶다면 초기화가 완료된 시점에 넣어줘야함
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final EntityManager manager;

        public void dbInit1() {
            Member member = createMember("userA", "서울", "1", "1111");
            manager.persist(member);

            Book book1 = createBook("JPA1 BOOK", 10000, 100);
            manager.persist(book1);

            Book book2 = createBook("JPA2 BOOK", 20000, 100);
            manager.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            manager.persist(order);
        }

        public void dbInit2() {
            Member member = createMember("userB", "진주", "2", "2222");
            manager.persist(member);

            Book book1 = createBook("SPRING1 BOOK", 20000, 200);
            manager.persist(book1);

            Book book2 = createBook("SPRING2 BOOK", 40000, 300);
            manager.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 20000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 40000, 4);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            manager.persist(order);
        }

        private Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }

        private Book createBook(String s, int i, int i2) {
            Book book1 = new Book();
            book1.setName(s);
            book1.setPrice(i);
            book1.setStockQuantity(i2);
            return book1;
        }

        private Member createMember(String user, String city, String st, String zipcode) {
            Member member = new Member();
            member.setName(user);
            member.setAddress(new Address(city, st, zipcode));
            return member;
        }


    }
}


