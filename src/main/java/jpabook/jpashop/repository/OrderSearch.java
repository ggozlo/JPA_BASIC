package jpabook.jpashop.repository;

import jpabook.jpashop.domain.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderSearch {

    private String memberName;
    private OrderStatus orderStatus;
}
