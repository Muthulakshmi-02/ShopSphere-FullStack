package com.example.MyProject.Repository;

import com.example.MyProject.Models.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_UserIdOrderByIsDefaultDescAddressIdDesc(Long userId);

    Optional<Address> findByAddressIdAndUser_UserId(Long addressId, Long userId);

    Optional<Address> findByUser_UserIdAndIsDefaultTrue(Long userId);

    long countByUser_UserId(Long userId);
}
