package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
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
    private final OrderQueryRepository orderQuery;

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

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQuery.findOrderQueryDtos();
    }
    // repository 를 구현할때 내부에서 사용하기 위해 entity 를 반환하는 클래스와 외부에 노출하기 위한 dto를 반환하는
    // 클래스를 분리해서 만들면 완벽히 분리가 가능하다
    // To One 관계는 쿼리문 한번에 해걸된다, 콜렉션 값들은 To One 관계의 행 만큼 쿼리문을 발생시킨다.  결과적 N + 1

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQuery.findAllByDto_optimization();
        // 주석은 해당 메서드에 기입하였음
        // 정규화되어서 데이터의 선택을 데이터베이스 에서 한다 하지만 엔티티방식으로 조회하면 batchsize로 똑같은 이득을 얻을수 있다
    }

    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQuery.findAllByDto_flat();
        return flats.stream()
                .collect(Collectors.groupingBy(o -> new OrderQueryDto(o.getOrderId(),o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(),o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList())))
                .entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),e.getKey().getName(), e.getKey().getOrderDate(),
                        e.getKey().getOrderStatus(),e.getKey().getAddress(), e.getValue())).collect(Collectors.toList());
        // dto 하나에 필요한 테이블을 다 조인해서 다 가져오게 만들었다.
        // 한 행에 다 가져오므로 1 x N 문제가 발생한다 즉 컬렉션 행 수 만큼 늘어난다
        // 그래도 쿼리는 한번에 된다
        // 페이징도 잘 안된다
        // 어플리케이션 에서 추가작업이 크다
        // dto내용을 분할해서 다시 collection 형으로 넣어서 dto 를 변환시킬수 있다.
        }

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
