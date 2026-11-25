package com.example.MyProject.Repository;

import com.example.MyProject.Models.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrder_OrderIdOrderByChangedAtAsc(Long orderId);
}
