package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();

        result.stream().forEach(o -> {
            List<OrderItemQueryDto> orderItems =  findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });
        // 가져온 리스트에서 컬렉션 값을 넣는 반복문을 실행한다
        return result;
    }

    public List<OrderQueryDto> findAllByDto_optimization() {

        List<OrderQueryDto> result = findOrders();
        // dto 리스트 획득
        List<Long> orderIds = toOrderIds(result);
        // dto 리스트에서 pk 리스트 추출
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);
        // 추출된 pk 리스트를 jpql의 in 절에 넣어서 쿼리문 한번에 컬렉션 값을 다 끌어옴
        // 최적화 하기 위한 컬렉션 값 리스트를 fk를 키값으로 map 구조로 그룹화
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        // 컬렉션 값 맵 에서 dto 객체가 가진 pk로 가져온다.
        // 쿼리는 총 2번 발생
        return result;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto( oi.order.id, i.name, oi.orderPrice, oi.count )" +
                        "from OrderItem oi " +
                        "join oi.item i " +
                        "where oi.order.id in :orderId ", OrderItemQueryDto.class)
                .setParameter("orderId", orderIds)
                .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId()));
        return orderItemMap;
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream().map(orderQueryDto -> orderQueryDto.getOrderId()).collect(Collectors.toList());
        return orderIds;
    }


    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto( oi.order.id, i.name, oi.orderPrice, oi.count )" +
                        "from OrderItem oi " +
                        "join oi.item i " +
                        "where oi.order.id = :orderId ", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
        // 정의된 스팩대로 dto 컬렉션을 만들어 반환한다
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                        "from Order o " +
                        "join o.member m " +
                        "join o.delivery d", OrderQueryDto.class)
                .getResultList();
        // 컬렉션은 바로 넣을수 없다
    }


    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderFlatDto( o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count) " +
                        "from Order o " +
                        "join o.member m " +
                        "join o.delivery d " +
                        "join o.orderItems oi " +
                        "join oi.item i", OrderFlatDto.class)
                .getResultList();
    }
}

