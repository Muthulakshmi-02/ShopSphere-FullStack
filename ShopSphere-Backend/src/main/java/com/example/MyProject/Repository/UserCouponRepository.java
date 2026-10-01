package com.example.MyProject.Repository;

import com.example.MyProject.Models.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    boolean existsByUserEmailAndCouponCode(String userEmail, String couponCode);

    void deleteByUserEmailAndCouponCode(String userEmail, String couponCode);
}