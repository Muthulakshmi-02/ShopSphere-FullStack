package com.example.MyProject.Repository;
import com.example.MyProject.Models.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface PaymentRepository extends JpaRepository<PaymentDetail, Long> {
    Optional<PaymentDetail> findByOrder_OrderId(Long orderId);
    Optional<PaymentDetail> findByTransactionId(String transactionId);
}