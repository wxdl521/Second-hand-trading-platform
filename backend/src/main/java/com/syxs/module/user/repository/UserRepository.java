package com.syxs.module.user.repository;

import com.syxs.module.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    java.util.List<User> findAllByOrderByCreatedAtDesc();
}
