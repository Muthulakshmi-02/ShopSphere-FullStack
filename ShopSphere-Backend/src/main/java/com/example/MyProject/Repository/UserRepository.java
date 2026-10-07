package com.example.MyProject.Repository;

import com.example.MyProject.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);

    // Case-insensitive, so "Abhi@x.com" and "abhi@x.com" cannot become two accounts.
    // findFirst... never throws if legacy data already contains such a pair.
    Optional<User> findFirstByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}