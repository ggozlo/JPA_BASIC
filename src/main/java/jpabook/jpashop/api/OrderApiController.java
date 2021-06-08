package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order o : all) {
            o.getMember().getName();
            o.getDelivery().getAddress();
            List<OrderItem> orderItems = o.getOrderItems();
            orderItems.forEach(orderItem -> orderItem.getItem().getName());
        } // 필요한 내용을 전부 초기화
        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString((new OrderSearch()));
        List<OrderDto> result = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return result;
    } // dto가 필드값으로 엔티티를 가지고 있다면 안좋은 상태다, 또한 쿼리문이 지연로딩으로 필요한 만큼 실행된다

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
       List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return result;
    } // 컬렉션 값을 가진 테이블에서의 페치조인 시도, 컬렉션 값과의 조인은 가져올 데이터의 양을 조인될 컬렉션 값의 수 많큼 늘림 ( 1 x n = n )
    // jpa의 distinct / 데이터베이스의 distinct는 row가 완전이 같아야 삭제하므로 대부분 효과가 없다
    // 하지만 jpql에서 출력타입 엔티티의 id값이 같다면 중복제거 한다 다만 전송 데이터량은 많다
    // ** 단점 컬렉션 페치 조인시 페이징이 불가능해진다!
    // 컬렉션 페치 조인은 하나만! 1 x N x M 식의 데이터 증가로 데이터가 깨질수 있다.

    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return result;
    } // 단일 상관관계만 페치 조인으로 끌어오고 나머지는 지연로딩 방식으로 끌어왔다, 생성자에서 초기화 됬음 1xNxM -페이징은 가능
    // 전역 설정에서 default_batch_fetch_size 또는 지역 @Batchsize 어노테이션을 통해 쿼리문을 사이즈 만큼 in 조건절로 땡겨올수 있다 (넘기면 사이즈만큼 반복).
    // 데이터 전송량 감축 가능


    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address; // 값타입은 큰 문제 없다
        private List<OrderItemDto> orderItems;


        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }
    @Getter
    static class OrderItemDto {

        private String itemName;  //상품명
        private int orderPrice; // 주문가격
        private int count; // 주문 수량
        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
