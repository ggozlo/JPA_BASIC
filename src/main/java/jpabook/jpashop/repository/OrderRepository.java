package jpabook.jpashop.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.jpashop.domain.*;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory query;

    @Autowired
    public OrderRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.query = new JPAQueryFactory(entityManager);
    }

    public void save(Order order) {
        entityManager.persist(order);
    }

    public Order findOne(Long id) {
        return entityManager.find(Order.class, id);
    }

    //문자열 조립
    public List<Order> findAllByString(OrderSearch orderSearch) {
        //language=JPQL
        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        } TypedQuery<Order> query = entityManager.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }
    
    // JPA CRITERIA
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name); }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = entityManager.createQuery(cq).setMaxResults(1000); //최대1000건
        return query.getResultList();
    }




    public List<Order> findAllWithMemberDelivery() {
        return entityManager.createQuery(
                "select o from Order o" +
                        " join fetch o.member" +
                        " join fetch o.delivery", Order.class
        ).getResultList();
    }


    public List<Order> findAllWithItem() {
        return entityManager.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m " +
                        " join  fetch o.delivery d " +
                        " join  fetch o.orderItems oi " +
                        "join  fetch oi.item i ", Order.class)
//                .setFirstResult(1).setMaxResults(100) // 넣어도 안먹힌다, 로그에 컬렉션 페치랑 같이쓰면 applying memory 로그가 뜬다 - 메모리에서 페이징 한다는 뜻(성능 저하)
                .getResultList();
    }


    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return entityManager.createQuery(
                "select o from Order o" +
                        " join fetch o.member" +
                        " join fetch o.delivery", Order.class)
                .setFirstResult(offset).setMaxResults(limit).getResultList();
    } // 페이징용 to One 관계만 페치 전역 배치사이즈를 잡았다면 페치조인을 안해도 어느정도 최적화가 되지만 페치조인이 좋다



    //쿼리 dsl
    public List<Order> findAll(OrderSearch orderSearch) {

//        JPAQueryFactory query = new JPAQueryFactory(entityManager);
//        QOrder order = QOrder.order;
//        QMember member = QMember.member;

        return query
                .select(QOrder.order)
                .from(QOrder.order)
                .join(QOrder.order.member, QMember.member)
                .where(stsusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName())) // 조건이 없다면(null) where 절 비활성화 있다면 조건 활성화
                .limit(1000)
                .fetch();
    }

    private BooleanExpression nameLike(String mamberName) {
        if(!StringUtils.hasText(mamberName)) {
            return null;
        }
        return QMember.member.name.like(mamberName);
    }

    private BooleanExpression stsusEq(OrderStatus statusCond) {
        if(statusCond == null) {
            return null;
        }
        return QOrder.order.status.eq(statusCond);
    }

}


