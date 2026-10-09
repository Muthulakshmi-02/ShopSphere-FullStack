package com.example.MyProject.Repository;

import com.example.MyProject.Models.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_UserIdOrderByIsDefaultDescAddressIdDesc(Long userId);

    Optional<Address> findByAddressIdAndUser_UserId(Long addressId, Long userId);

    Optional<Address> findByUser_UserIdAndIsDefaultTrue(Long userId);

    long countByUser_UserId(Long userId);

    // One statement clears every default of this user. The old "find the one default, then
    // save" approach threw IncorrectResultSizeDataAccessException if two requests ever left
    // two defaults behind. clearAutomatically refreshes what's loaded in this transaction.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.user.userId = :userId and a.isDefault = true")
    int clearDefaultForUser(@Param("userId") Long userId);
}